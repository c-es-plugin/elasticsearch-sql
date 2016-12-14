package org.elasticsearch.plugin.nlpcn.preAnalyzer;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.alibaba.druid.util.JdbcConstants;
import org.nlpcn.es4sql.parse.ElasticLexer;
import org.nlpcn.es4sql.parse.ElasticSqlExprParser;

import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by fangbb on 2016-12-6.
 */
public class SqlParseAnalyzer {
//    public String dbType = JdbcConstants.MYSQL;
    private Analyzer analyzer;

    public SqlParseAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String seg(String sql) throws Exception {
        if (sql.contains("seg(")) {
            MySqlSelectQueryBlock query = getQueryBlock(sql);
            parseWhere(query.getWhere());
            parseOrderBys(query.getOrderBy());
            sql = printSql(query);
        }
        return sql;
    }

    private MySqlSelectQueryBlock getQueryBlock(String sql) {
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

    private void parseWhere(SQLExpr where) throws Exception {
        if (where == null) {
            return;
        }
        preTraverse(where);
    }

    private void parseOrderBys(SQLOrderBy sqlOrderBy) throws Exception {
        if (sqlOrderBy == null) {
            return;
        }
        List<SQLSelectOrderByItem> items = sqlOrderBy.getItems();
        for (SQLSelectOrderByItem item : items) {
            SQLExpr sqlExpr = item.getExpr();
            if (sqlExpr instanceof SQLMethodInvokeExpr) {
                String nested = ((SQLMethodInvokeExpr) sqlExpr).getMethodName();
                List<SQLExpr> params = ((SQLMethodInvokeExpr) sqlExpr).getParameters();
                if (nested.equals("nested") && params.size() == 3) {
                    parseWhere(params.get(2));
                } else {
                    throw new Exception("Nested sorting must be 3 parameters");
                }
            }
        }
    }

    //先序遍历获取叶子节点
    private void preTraverse(SQLExpr sqlExpr) throws Exception {
        if (sqlExpr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr sqlBinaryOpExpr = (SQLBinaryOpExpr) sqlExpr;
            SQLExpr left = sqlBinaryOpExpr.getLeft();
            SQLExpr right = sqlBinaryOpExpr.getRight();
            SQLBinaryOperator sqlBinaryOperator = sqlBinaryOpExpr.getOperator();
            if (isLeaf(sqlBinaryOperator)) {
                // 普通叶节点替换
                replaceLeafNode(sqlBinaryOpExpr);
            } else {
                preTraverse(left);
                preTraverse(right);
            }
        } else if (isNested(sqlExpr)) {
            //nested 嵌套叶节点
            replaceNestedLeafNode(sqlExpr);
        } else {
            throw new SQLFeatureNotSupportedException();
        }
    }

    //TODO 对Nested叶节点拆分
    //TODO 分词
    //TODO 构造新节点
    private void replaceNestedLeafNode(SQLExpr sqlExpr) throws Exception {
        SQLObject sqlObject = sqlExpr.getParent();
        SQLExpr newExpr = parseNested(sqlExpr);
        if (sqlObject instanceof MySqlSelectQueryBlock) {
            ((MySqlSelectQueryBlock) sqlObject).setWhere(newExpr);
        } else if (sqlObject instanceof SQLBinaryOpExpr) {
            if (sqlExpr.equals(((SQLBinaryOpExpr) sqlObject).getRight())) {
                ((SQLBinaryOpExpr) sqlObject).setRight(newExpr);
            } else {
                ((SQLBinaryOpExpr) sqlObject).setLeft(newExpr);
            }
        }
    }

    //TODO 解析nested的叶节点，返回新构造的叶节点
    private SQLExpr parseNested(SQLExpr sqlExpr) throws Exception {
        String methodName = ((SQLMethodInvokeExpr) sqlExpr).getMethodName();
        if (((SQLMethodInvokeExpr) sqlExpr).getParameters().size() != 2) {
            throw new Exception("Nested query must be 2 parameters");
        }
        SQLExpr pathName = ((SQLMethodInvokeExpr) sqlExpr).getParameters().get(0);
        SQLExpr where = ((SQLMethodInvokeExpr) sqlExpr).getParameters().get(1);
        SQLExpr retExpr = null;
        if (where != null) {
            preTraverseNested(methodName, pathName, where);
            if (isLeaf(where)) {
                SQLObject parent = where.getParent();
                if (parent instanceof SQLMethodInvokeExpr) {
                    SQLMethodInvokeExpr tmp = ((SQLMethodInvokeExpr) parent);
                    if (tmp.getParameters().size() == 3) {
                        retExpr = tmp.getParameters().get(2);
                    } else {
                        retExpr = tmp;
                    }
                }
            } else if (where instanceof SQLBinaryOpExpr) {
                retExpr = where;
            }

        }
        return retExpr;
    }

    private void preTraverseNested(String methodName, SQLExpr pathName, SQLExpr sqlExpr) throws Exception {
        if (sqlExpr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr sqlBinaryOpExpr = (SQLBinaryOpExpr) sqlExpr;
            SQLExpr left = sqlBinaryOpExpr.getLeft();
            SQLExpr right = sqlBinaryOpExpr.getRight();
            SQLBinaryOperator sqlBinaryOperator = sqlBinaryOpExpr.getOperator();
            //left和right都是a=b这种形式
            if (isLeaf(sqlBinaryOperator)) {
                generateNestedLeafNode(methodName, pathName, sqlBinaryOpExpr);
            } else {
                preTraverseNested(methodName, pathName, left);
                preTraverseNested(methodName, pathName, right);
            }
        }
    }

    private boolean isNested(SQLExpr sqlExpr) {
        if (sqlExpr instanceof SQLMethodInvokeExpr) {
            String mName = ((SQLMethodInvokeExpr) sqlExpr).getMethodName();
            if (mName.equals("nested")) {
                return true;
            }
        }
        return false;
    }

    private boolean isLeaf(SQLBinaryOperator sqlBinaryOperator) {
        if (sqlBinaryOperator.equals(SQLBinaryOperator.BooleanOr) || sqlBinaryOperator.equals(SQLBinaryOperator.BooleanAnd)) {
            return false;
        }
        return true;
    }

    private boolean isLeaf(SQLExpr sqlExpr) {
        if (sqlExpr instanceof SQLBinaryOpExpr) {
            SQLBinaryOperator sqlBinaryOperator = ((SQLBinaryOpExpr) sqlExpr).getOperator();
            if (sqlBinaryOperator.equals(SQLBinaryOperator.BooleanOr) || sqlBinaryOperator.equals(SQLBinaryOperator.BooleanAnd)) {
                return false;
            }
        }
        return true;
    }

    private Method parseMethod(SQLExpr right) throws Exception {
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

    private boolean segNoQuota(SQLMethodInvokeExpr methodInvokeExpr) {
        List<SQLExpr> params = methodInvokeExpr.getParameters();
        if (params.get(0) instanceof SQLIdentifierExpr) {
            return true;
        }
        return false;
    }

    private void removeSegFun(SQLBinaryOpExpr binaryOpExpr, SQLExpr right, Method method) {
        //当seg内没有引号时，去掉seg()
        if (segNoQuota((SQLMethodInvokeExpr) right) && method.containSeg()) {
            method.setParentMethod(null);
            List<SQLExpr> params = ((SQLMethodInvokeExpr) right).getParameters();
            binaryOpExpr.setRight(params.get(0));
        }
    }

    private void generateNestedLeafNode(String methodName, SQLExpr pathName, SQLBinaryOpExpr binaryOpExpr) throws Exception {
        //right:a.b = "d"
        SQLExpr left = binaryOpExpr.getLeft();
        SQLExpr right = binaryOpExpr.getRight();
        SQLBinaryOperator operator = binaryOpExpr.getOperator();
        String filed = ((SQLIdentifierExpr) left).getName();
        if (right instanceof SQLMethodInvokeExpr) {
            Method method = parseMethod(right);
            String sourceTerm = method.getParams();
            removeSegFun(binaryOpExpr, right, method);
            //seg(term("abc")) exception
            if (method.containSeg() && sourceTerm != null) {
                String[] terms = analyzer.analyzer(sourceTerm);
                //String[] terms = "a,b".split(",");
                String funName = method.getFunName();
                List<SQLExpr> allNewNode = new ArrayList<SQLExpr>();
                for (String term : terms) {
                    if (!funName.equals("")) {
                        SQLMethodInvokeExpr methodInvokeExpr = new SQLMethodInvokeExpr();
                        methodInvokeExpr.setMethodName(funName);
                        methodInvokeExpr.addParameter(new SQLCharExpr(term));
                        SQLBinaryOpExpr opNode = createOpNode(filed, methodInvokeExpr, operator);
                        SQLMethodInvokeExpr nestedNode = createNestedNode(methodName, pathName, opNode);
                        allNewNode.add(nestedNode);
                    } else {
                        SQLCharExpr charExpr = new SQLCharExpr();
                        charExpr.setText(term);
                        SQLBinaryOpExpr opNode = createOpNode(filed, charExpr, operator);
                        SQLMethodInvokeExpr nestedNode = createNestedNode(methodName, pathName, opNode);
                        allNewNode.add(nestedNode);
                    }
                }
                conNestedTree(binaryOpExpr, allNewNode);
            } else {
                List<SQLExpr> allNewNode = new ArrayList<SQLExpr>();
                SQLBinaryOpExpr opNode = createOpNode(filed, right, operator);
                SQLMethodInvokeExpr nestedNode = createNestedNode(methodName, pathName, opNode);
                allNewNode.add(nestedNode);
                conNestedTree(binaryOpExpr, allNewNode);
            }
        } else {
            List<SQLExpr> allNewNode = new ArrayList<SQLExpr>();
            SQLBinaryOpExpr opNode = createOpNode(filed, right, operator);
            SQLMethodInvokeExpr nestedNode = createNestedNode(methodName, pathName, opNode);
            allNewNode.add(nestedNode);
            conNestedTree(binaryOpExpr, allNewNode);
        }
    }

    private void replaceOldNode(SQLExpr sqlExpr,SQLExpr newExpr) throws Exception {
        SQLObject sqlObject = sqlExpr.getParent();
        if (sqlObject instanceof MySqlSelectQueryBlock) {
            ((MySqlSelectQueryBlock) sqlObject).setWhere(newExpr);
        } else if (sqlObject instanceof SQLBinaryOpExpr) {
            if (sqlExpr.equals(((SQLBinaryOpExpr) sqlObject).getRight())) {
                ((SQLBinaryOpExpr) sqlObject).setRight(newExpr);
            } else {
                ((SQLBinaryOpExpr) sqlObject).setLeft(newExpr);
            }
        } else if (sqlObject instanceof SQLMethodInvokeExpr) {
            ((SQLMethodInvokeExpr) sqlObject).addParameter(newExpr);
        }
    }

    //构造新的二叉树替换原有节点
    private void conNestedTree(SQLBinaryOpExpr retExpr, List<SQLExpr> sqlExprs) throws Exception{
        int size = sqlExprs.size();
        int andNum = size - 1;
        List<SQLExpr> allNode = new ArrayList<SQLExpr>();
        if (andNum == 0) {
            replaceOldNode(retExpr, sqlExprs.get(0));
        } else {
            for (int i = 0; i < andNum; i++) {
                if (i == 0) {
                    retExpr.setOperator(SQLBinaryOperator.BooleanAnd);
                    //retExpr做为顶点
                    allNode.add(retExpr);
                } else {
                    allNode.add(createOpNode(null, null, SQLBinaryOperator.BooleanAnd));
                }
            }
            allNode.addAll(sqlExprs);
            //共有n-1个And，n个node，每一个节点从0开始进行编号，那么第i个节点的左孩子的编号为2*i+1，右孩子为2*i+2。
            for (int parentIndex = 0; parentIndex < andNum; parentIndex++) {
                if (allNode.get(parentIndex) instanceof SQLBinaryOpExpr) {
                    ((SQLBinaryOpExpr) allNode.get(parentIndex)).setLeft(allNode.get(parentIndex * 2 + 1));
                    ((SQLBinaryOpExpr) allNode.get(parentIndex)).setRight(allNode.get(parentIndex * 2 + 2));
                }
            }
        }
    }

    //对叶节点分词,构造新节点
    private void replaceLeafNode(SQLBinaryOpExpr binaryOpExpr) throws Exception {
        SQLExpr left = binaryOpExpr.getLeft();
        SQLExpr right = binaryOpExpr.getRight();
        SQLBinaryOperator operator = binaryOpExpr.getOperator();
        String filed = ((SQLIdentifierExpr) left).getName();
        if (right instanceof SQLMethodInvokeExpr) {
            Method method = parseMethod(right);
            String sourceTerm = method.getParams();
            removeSegFun(binaryOpExpr, right, method);
            //seg(term("abc")) exception
            if (method.containSeg() && sourceTerm != null) {
                String[] terms = analyzer.analyzer(sourceTerm);
                //String[] terms = "a,b".split(",");
                String funName = method.getFunName();
                List<SQLBinaryOpExpr> allNewNode = new ArrayList<SQLBinaryOpExpr>();
                for (String term : terms) {
                    if (!funName.equals("")) {
                        SQLMethodInvokeExpr methodInvokeExpr = new SQLMethodInvokeExpr();
                        methodInvokeExpr.setMethodName(funName);
                        methodInvokeExpr.addParameter(new SQLCharExpr(term));
                        allNewNode.add(createOpNode(filed, methodInvokeExpr, operator));
                    } else {
                        SQLCharExpr charExpr = new SQLCharExpr();
                        charExpr.setText(term);
                        allNewNode.add(createOpNode(filed, charExpr, operator));
                    }

                }
                conTree(binaryOpExpr, allNewNode);
            }
        }
    }

    //构造新的二叉树替换原有节点
    private void conTree(SQLBinaryOpExpr retExpr, List<SQLBinaryOpExpr> SQLBinaryOpNode) {
        int size = SQLBinaryOpNode.size();
        int andNum = size - 1;
        List<SQLBinaryOpExpr> allNode = new ArrayList<SQLBinaryOpExpr>();
        if (andNum == 0) {
            retExpr.setRight(SQLBinaryOpNode.get(0).getRight());
        } else {
            for (int i = 0; i < andNum; i++) {
                if (i == 0) {
                    retExpr.setOperator(SQLBinaryOperator.BooleanAnd);
                    //retExpr做为顶点
                    allNode.add(retExpr);
                } else {
                    allNode.add(createOpNode(null, null, SQLBinaryOperator.BooleanAnd));
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
    private SQLBinaryOpExpr createOpNode(String filed, SQLExpr value, SQLBinaryOperator operator) {
        SQLBinaryOpExpr retWhere = new SQLBinaryOpExpr();
        SQLIdentifierExpr ileft = new SQLIdentifierExpr();
        ileft.setName(filed);
        retWhere.setLeft(ileft);
        retWhere.setOperator(operator);
        retWhere.setRight(value);
        return retWhere;
    }

    //TODO 构造一个nested节点
    private SQLMethodInvokeExpr createNestedNode(String name, SQLExpr pathName, SQLBinaryOpExpr sqlBinaryOpExpr) {
        SQLMethodInvokeExpr sqlMethodInvokeExpr = new SQLMethodInvokeExpr();
        sqlMethodInvokeExpr.setMethodName(name);
        sqlMethodInvokeExpr.addParameter(pathName);
        sqlMethodInvokeExpr.addParameter(sqlBinaryOpExpr);
        return sqlMethodInvokeExpr;
    }

    private String printSql(MySqlSelectQueryBlock query) {
        StringBuilder out = new StringBuilder();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(out);
        query.accept0(visitor);
        return out.toString();
    }

}
