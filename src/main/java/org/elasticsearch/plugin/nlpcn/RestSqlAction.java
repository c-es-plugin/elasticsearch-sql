package org.elasticsearch.plugin.nlpcn;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugin.nlpcn.executors.ActionRequestRestExecuterFactory;
import org.elasticsearch.plugin.nlpcn.executors.RestExecutor;
import org.elasticsearch.plugin.nlpcn.preAnalyzer.SqlParseAnalyzer;
import org.elasticsearch.rest.*;
import org.nlpcn.es4sql.SearchDao;
import org.nlpcn.es4sql.query.QueryAction;

import java.util.Map;


public class RestSqlAction extends BaseRestHandler {

	@Inject
	public RestSqlAction(Settings settings, Client client, RestController restController) {
		super(settings, restController, client);
		restController.registerHandler(RestRequest.Method.POST, "/_sql/_explain", this);
		restController.registerHandler(RestRequest.Method.GET, "/_sql/_explain", this);
		restController.registerHandler(RestRequest.Method.POST, "/_sql", this);
		restController.registerHandler(RestRequest.Method.GET, "/_sql", this);
		restController.registerHandler(RestRequest.Method.GET, "/_sql/_seg", this);
	}

	@Override
	protected void handleRequest(RestRequest request, RestChannel channel, final Client client) throws Exception {
		String sql = request.param("sql");

		if (sql == null) {
			sql = request.content().toUtf8();
		}
		//ananlyze
		//sql = SqlAnalyzer.seg(sql);
		sql = SqlParseAnalyzer.seg(sql);
		SearchDao searchDao = new SearchDao(client);
        QueryAction queryAction= searchDao.explain(sql);

		// TODO add unittests to explain. (rest level?)
		if(request.path().endsWith("_seg")){
			BytesRestResponse bytesRestResponse = new BytesRestResponse(RestStatus.OK, sql);
			channel.sendResponse(bytesRestResponse);
		} else if (request.path().endsWith("/_explain")) {
			String jsonExplanation = queryAction.explain().explain();
			BytesRestResponse bytesRestResponse = new BytesRestResponse(RestStatus.OK, jsonExplanation);
			channel.sendResponse(bytesRestResponse);
		} else {
            Map<String, String> params = request.params();
            RestExecutor restExecutor = ActionRequestRestExecuterFactory.createExecutor(params.get("format"));
			restExecutor.execute(client,params,queryAction,channel);
		}
	}
}