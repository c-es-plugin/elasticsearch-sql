package org.elasticsearch.plugin.nlpcn.preAnalyzer;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.alibaba.druid.util.JdbcConstants;
import org.elasticsearch.plugin.nlpcn.request.HttpRequester;
import org.elasticsearch.plugin.nlpcn.request.HttpResponse;
import org.nlpcn.es4sql.parse.ElasticLexer;
import org.nlpcn.es4sql.parse.ElasticSqlExprParser;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by fangbb on 2016-12-6.
 */
public class SqlParseAnalyzer {
    public static String dbType = JdbcConstants.MYSQL;

    public static String seg(String sql) throws Exception {
        if (sql.contains("seg(")) {
            MySqlSelectQueryBlock query = getQueryBlock(sql);
            parseWhere(query.getWhere());
            parseOrderBys(query.getOrderBy());
            sql = printSql(query);
        }
        return sql;
    }

    private static MySqlSelectQueryBlock getQueryBlock(String sql) {
        ElasticLexer lexer = new ElasticLexer(sql);
        lexer.nextToken();
        ElasticSqlExprParser elasticSqlExprParser = new ElasticSqlExprParser(lexer);
        SQLExpr expr = elasticSqlExprParser.expr();
        SQLQueryExpr sqlExpr = (SQLQueryExpr) expr;
        SQLSelect sqlSelect = sqlExpr.getSubQuery();
        //获取SQLSelectQuery
        SQLSelectQuery sqlSelectQuery = sqlSelect.getQuery();
        MySqlSelectQueryBlock query = (MySqlSelectQueryBlock) sqlSelectQuery;
        return query;
    }

    private static void parseWhere(SQLExpr where) throws Exception {
        if (where == null) {
            return;
        }
        preTraverse(where);
    }

    private static void parseOrderBys(SQLOrderBy sqlOrderBy) throws Exception {
        if (sqlOrderBy == null) {
            return;
        }
        List<SQLSelectOrderByItem> items = sqlOrderBy.getItems();
        for (SQLSelectOrderByItem item : items) {
            SQLExpr sqlExpr = item.getExpr();
            if (sqlExpr instanceof SQLMethodInvokeExpr) {
                String nested = ((SQLMethodInvokeExpr) sqlExpr).getMethodName();
                List<SQLExpr> params = ((SQLMethodInvokeExpr) sqlExpr).getParameters();
                SQLBinaryOpExpr condition;
                if (nested.equals("nested") && params.size() == 3) {
                    parseWhere((SQLBinaryOpExpr) params.get(2));
                    //params.add(2, condition);
                } else {
                    new Exception("Nested sorting must be 3 parameters");
                }
            }
        }
    }

    //先序遍历获取叶子节点
    private static void preTraverse(SQLExpr sqlExpr) throws Exception {
        if (sqlExpr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr sqlBinaryOpExpr = (SQLBinaryOpExpr) sqlExpr;
            SQLExpr left = sqlBinaryOpExpr.getLeft();
            SQLExpr right = sqlBinaryOpExpr.getRight();
            if (isLeaf(left) && isLeaf(right)) {
                //TODO
                replaceLeafNode(sqlBinaryOpExpr);
                return;
            } else {
                preTraverse(left);
                preTraverse(right);
            }
        } else if (sqlExpr instanceof SQLMethodInvokeExpr) {
            String nested = ((SQLMethodInvokeExpr) sqlExpr).getMethodName();
            List<SQLExpr> params = ((SQLMethodInvokeExpr) sqlExpr).getParameters();
            SQLBinaryOpExpr condition;
            if (nested.equals("nested") && params.size() == 2) {
                parseWhere((SQLBinaryOpExpr) params.get(1));
            } else {
                new Exception("Nested where must be 2 parameters");
            }
        }
    }

    private static boolean isLeaf(SQLExpr sqlExpr) {
        if (sqlExpr instanceof SQLBinaryOpExpr) {
            return false;
        }
        return true;
    }

    private static Method parseMethod(SQLExpr right) throws Exception {
        Method retMethod = new Method();
        if (right instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr methodInvokeExpr = ((SQLMethodInvokeExpr) right);
            String methodName = methodInvokeExpr.getMethodName();
            retMethod.setParentMethod(methodName);
            List<SQLExpr> childMethod = methodInvokeExpr.getParameters();
            SQLExpr sqlExpr = childMethod.get(0);
            if (sqlExpr instanceof SQLMethodInvokeExpr) {
                retMethod.setChildenMethod(((SQLMethodInvokeExpr) sqlExpr).getMethodName());
                SQLExpr nSqlExpr = ((SQLMethodInvokeExpr) sqlExpr).getParameters().get(0);
                if (nSqlExpr instanceof SQLCharExpr) {
                    retMethod.setParams(((SQLCharExpr) nSqlExpr).getText());
                } else if (nSqlExpr instanceof SQLIdentifierExpr) {
                    if (retMethod.getChildenMethod().equals("seg")) {
                        SQLIdentifierExpr identifierExpr = (SQLIdentifierExpr) nSqlExpr;
                        retMethod.setParams(identifierExpr.getName());
                        childMethod.clear();
                        childMethod.add(0, identifierExpr);
                        retMethod.setChildenMethod(null);
                    }
                }
            } else if (sqlExpr instanceof SQLCharExpr) {
                retMethod.setParams(((SQLCharExpr) sqlExpr).getText());
            } else if (sqlExpr instanceof SQLIdentifierExpr) {
                retMethod.setParams(((SQLIdentifierExpr) sqlExpr).getName());
            }
        }
        return retMethod;
    }

    private static boolean segNoQuota(SQLMethodInvokeExpr methodInvokeExpr) {
        List<SQLExpr> params = methodInvokeExpr.getParameters();
        if (params.get(0) instanceof SQLIdentifierExpr) {
            return true;
        }
        return false;
    }

    private static  void removeSegFun(SQLBinaryOpExpr binaryOpExpr,SQLExpr right,Method method){
        //当seg内没有引号时，去掉seg()
        if (segNoQuota((SQLMethodInvokeExpr) right) && method.containSeg()) {
            method.setParentMethod(null);
            List<SQLExpr> params = ((SQLMethodInvokeExpr) right).getParameters();
            binaryOpExpr.setRight(params.get(0));
        }
    }

    //对叶节点分词,构造新节点
    private static void replaceLeafNode(SQLBinaryOpExpr binaryOpExpr) throws Exception {
        SQLExpr left = binaryOpExpr.getLeft();
        SQLExpr right = binaryOpExpr.getRight();
        SQLBinaryOperator operator = binaryOpExpr.getOperator();
        String filed = ((SQLIdentifierExpr) left).getName();
        if (right instanceof SQLMethodInvokeExpr) {
            Method method = parseMethod(right);
            String sourceTerm = method.getParams();
            removeSegFun(binaryOpExpr,right,method);
            //seg(term("abc")) exception
            if (method.containSeg() && sourceTerm != null) {
                String[] terms = analyzer(sourceTerm);
                //String[] terms = "a,b".split(",");
                String funName = method.getFunName();
                List<SQLBinaryOpExpr> allNewNode = new ArrayList<SQLBinaryOpExpr>();
                for (String term : terms) {
                    if (!funName.equals("")) {
                        SQLMethodInvokeExpr methodInvokeExpr = new SQLMethodInvokeExpr();
                        methodInvokeExpr.setMethodName(funName);
                        methodInvokeExpr.addParameter(new SQLCharExpr(term));
                        allNewNode.add(createNode(filed, methodInvokeExpr, operator));
                    } else {
                        SQLCharExpr charExpr = new SQLCharExpr();
                        charExpr.setText(term);
                        allNewNode.add(createNode(filed, charExpr, operator));
                    }

                }
                conTree(binaryOpExpr, allNewNode);
            }
        }
    }

    //构造新的二叉树替换原有节点
    private static void conTree(SQLBinaryOpExpr retExpr, List<SQLBinaryOpExpr> SQLBinaryOpNode) {
        int size = SQLBinaryOpNode.size();
        int andNum = size - 1;
        List<SQLBinaryOpExpr> allNode = new ArrayList<SQLBinaryOpExpr>();
        if (andNum == 0) {
            retExpr.setRight(SQLBinaryOpNode.get(0).getRight());
        }else {
            for (int i = 0; i < andNum; i++) {
                if (i == 0) {
                    retExpr.setOperator(SQLBinaryOperator.BooleanAnd);
                    allNode.add(retExpr);
                } else {
                    allNode.add(createNode(null, null, SQLBinaryOperator.BooleanAnd));
                }
            }
            allNode.addAll(SQLBinaryOpNode);
            //共有n-1个And，n个node，每一个节点从0开始进行编号，那么第i个节点的左孩子的编号为2*i+1，右孩子为2*i+2。
            for (int parentIndex = 0; parentIndex < andNum; parentIndex++) {
                allNode.get(parentIndex).setLeft(allNode.get(parentIndex * 2 + 1));
                allNode.get(parentIndex).setRight(allNode.get(parentIndex * 2 + 2));
            }
        }
    }


    //TODO 构造一个节点
    private static SQLBinaryOpExpr createNode(String filed, SQLExpr value, SQLBinaryOperator operator) {
        SQLBinaryOpExpr retWhere = new SQLBinaryOpExpr();
        SQLIdentifierExpr ileft = new SQLIdentifierExpr();
        ileft.setName(filed);
        retWhere.setLeft(ileft);
        retWhere.setOperator(operator);
        retWhere.setRight(value);
        return retWhere;
    }

    private static String printSql(MySqlSelectQueryBlock query) {
        StringBuilder out = new StringBuilder();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(out);
        query.accept0(visitor);
        return out.toString();
    }

    public static String[] analyzer(String term) {
        //TODO done
        HttpRequester request = new HttpRequester();
        HttpResponse response = null;
        String sourceTerms = "";
//        http://192.168.25.11:9688/_cat/analyze?text=大数据&analyzer=query_ansj
        try {
            //String ip = InetAddress.getLocalHost().getHostAddress();
            String ip = AnsjElasticConfigurator.ES_IP;
            String port = AnsjElasticConfigurator.ES_PORT;
            String midUrl = "/_cat/analyze?analyzer=query_ansj&text=";
            String preUrl = "http://" + ip + ":" + port + midUrl;
            //String preUrl = "http://192.168.25.11:9688/_cat/analyze?analyzer=query_ansj&text=";
            System.out.println(preUrl);
            String enTerm = URLEncoder.encode(term, "UTF-8");
            String url = preUrl + enTerm;
            System.out.println(url);
            response = request.sendGet(url);
            if (response.getCode() == 200) {
                if (response != null && response.getContent().length() > 10) {
                    sourceTerms = response.getContent();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return getTerms(sourceTerms).split(",");
    }

    private static String getTerms(String sourceTerms) {
        StringBuffer sb = new StringBuffer();
        String[] lines = sourceTerms.split("\n");
        int lineLen = lines.length;
        for (int i = 0; i < lineLen; i++) {
            String[] terms = lines[i].split("\t");
            String term = terms[0].trim();
            int size = terms.length;
            if (i == 0) {
                //sb.append("\"").append(term).append("\"");
                sb.append(term);
            } else {
                sb.append(",").append(term);
            }
        }
        return sb.toString();
    }

}
