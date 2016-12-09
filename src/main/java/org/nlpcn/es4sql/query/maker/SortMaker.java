package org.nlpcn.es4sql.query.maker;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.nlpcn.es4sql.domain.Condition;
import org.nlpcn.es4sql.domain.Order;
import org.nlpcn.es4sql.domain.Where;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fangbb on 2016-11-22.
 */
public class SortMaker extends Maker {
    public SortMaker() {
        super(true);
    }

    public static SortBuilder explan(Order order) {
        String flag = "_last";
        flag = order.getType().equals("DESC") ? "_last" : "_first";
        Where where = order.getCondition();
        String filedName = order.getName();
        String path = order.getPath();
        String mode = order.getMode() == null ? "sum" : order.getMode();
        String type = order.getType();
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        List<Where> conditions = where.getWheres();
        setCondition(conditions, queryBuilder);
        SortBuilder sort = SortBuilders
                .fieldSort(filedName)
                .setNestedFilter(queryBuilder)
                .setNestedPath(path)
                .sortMode(mode)
                .order(SortOrder.valueOf(type))
                .missing(flag);
        return sort;
    }

    public static void setCondition(List<Where> conditions, BoolQueryBuilder queryBuilder) {
        String key = "";
        String val = "";
        for (Where con : conditions) {
            if (con instanceof Condition) {
                key = ((Condition) con).getName();
                Object vals = ((Condition) con).getValue();
                if (vals.getClass().isArray()) {
                    Object[] md = (Object[]) vals;
                    val = md[0].toString();
                } else {
                    val = vals.toString();
                }
                queryBuilder = explanSort(queryBuilder, key, val);
            } else if (con instanceof Where) {
                List<Where> conWhere = con.getWheres();
                setCondition(conWhere, queryBuilder);
            }

        }
    }

    public static BoolQueryBuilder explanSort(BoolQueryBuilder queryBuilder, String key, String value) {
        QueryBuilder termQueryBuilder = QueryBuilders.termQuery(key, value);
        queryBuilder = queryBuilder.should(termQueryBuilder);
        return queryBuilder;
    }

}
