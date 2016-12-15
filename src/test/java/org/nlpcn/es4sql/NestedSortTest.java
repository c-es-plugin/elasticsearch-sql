package org.nlpcn.es4sql;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;
import org.junit.Test;
import org.nlpcn.es4sql.query.QueryAction;
import org.nlpcn.es4sql.query.SqlElasticRequestBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * Created by fangbb on 2016-12-12.
 */
public class NestedSortTest {
    @Test
    public void nestedSortTest(){
        try {
            TransportClient client;
            client = TransportClient.builder().addPlugin(DeleteByQueryPlugin.class).build().addTransportAddress(getTransportAddress());
            String sql = "select * from test where nested(info,info.name=term(\"java\") and info.name=term(\"python\") or info.name=term(\"Hadoop\")) order by nested(info,sum(info.age),info.name=matchQuery(\"java\") and info.name=term(\"python\") and info.name=\"Hadoop\") desc,score desc";
            //String sql = "SELECT * FROM test order by nested(a,sum(a.b),a.c=matchQuery(\"百度和谷歌\"))";
            //String sql = "SELECT * FROM test order by nested(a,sum(a.b),a.c=term(\"百度和谷歌\") and a.c=term(\"test\"))";
            //String sql = "SELECT * FROM test order by nested(a,sum(a.b),a.c=\"百度和谷歌\")";
            //String sql = "SELECT * FROM test where nested(calc_skill_blog,calc_skill_blog.keyword=\"java\")";
            Long now = System.currentTimeMillis();
            SearchDao searchDao = new SearchDao(client);
            QueryAction queryAction = searchDao.explain(sql);
            SqlElasticRequestBuilder xx = queryAction.explain();
            String jsonExplanation = xx.explain();
            //String jsonExplanation = queryAction.explain().explain();
            System.out.println(System.currentTimeMillis() - now + " ms");
            System.out.println(jsonExplanation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static InetSocketTransportAddress getTransportAddress() throws UnknownHostException {

        String host = "192.168.25.11";
        String port = "9300";
        if (host == null) {
            host = "localhost";
            System.out.println("ES_TEST_HOST enviroment variable does not exist. choose default 'localhost'");
        }

        if (port == null) {
            port = "9300";
            System.out.println("ES_TEST_PORT enviroment variable does not exist. choose default '9300'");
        }

        System.out.println(String.format("Connection details: host: %s. port:%s.", host, port));
        return new InetSocketTransportAddress(InetAddress.getByName(host), Integer.parseInt(port));
    }


}
