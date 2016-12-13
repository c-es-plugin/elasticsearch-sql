package org.nlpcn.es4sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlExprParser;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import org.elasticsearch.plugin.nlpcn.preAnalyzer.SqlAnalyzer;
import org.elasticsearch.plugin.nlpcn.preAnalyzer.SqlParseAnalyzer;
import org.nlpcn.es4sql.parse.ElasticLexer;
import org.nlpcn.es4sql.parse.ElasticSqlExprParser;
import org.junit.Test;
import java.net.InetAddress;
import java.util.List;

/**
 * Created by fangbb on 2016-12-4.
 */
public class Test {
    @Test
    public void StrTest() {
        //String sql = "select * from test where  nested(info,info.name =  term(seg(\"大数据云计算\")) and info.name=seg(\"python|Hbase|Hive\") or info.name=term(seg(\"Hadoop\"))) and city=seg(\"hah\") and province=\"河北省\" order by nested(info,sum(info.age), info.name=seg(\"java\") and info.name=terms(seg(\"python\")) and info.name=seg(\"Hadoop\")) desc,score desc";
        String sql = "select * from a where name=seg(\"大数据云计算\") and age > 18 order by age desc";
        //System.out.println(sql);
        //sql = SqlSegment.seg(sql);
        sql = SqlParseAnalyzer.seg(sql);
        //sql = SqlAnalyzer.seg(sql);
        System.out.println("-------------");
        System.out.println(sql);
        System.out.println("-------------");
    }

    @org.junit.Test
    public void Str() throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println(ip);
    }

    @org.junit.Test
    public void SqlExprParser() {
        try {
            String sql = "select * from a where name=seg(\"大数据云计算\") and age > 18 order by age desc";
            ElasticSqlExprParser parser1 = new ElasticSqlExprParser(sql);
            SQLExpr expr = parser1.expr();
            SQLQueryExpr sqlExpr = (SQLQueryExpr) expr;
            MySqlSelectQueryBlock query = (MySqlSelectQueryBlock) sqlExpr.getSubQuery().getQuery();
            SQLOrderBy orderBy = query.getOrderBy();
            SQLExpr sqlExpr1 = query.getWhere();
            System.out.println("====");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @org.junit.Test
    public void StatementParser() {
        String sql = "select * from a where name=seg(\"大数据云计算\") and age > 18 order by age desc";
        ElasticLexer lexer = new ElasticLexer(sql);
        lexer.nextToken();
        MySqlStatementParser parser = new MySqlStatementParser(lexer);
        List<SQLStatement> statementList = parser.parseStatementList();
        StringBuilder out = new StringBuilder();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(out);
        for (SQLStatement statement : statementList) {
            statement.accept(visitor);
            //visitor.println();
        }
        System.out.println(out.toString());
    }

    @org.junit.Test
    public void myStatementParser() {
        String sql = "select * from a-b where name=seg(\"大数据云计算\") and age > 18 order by age desc";
        ElasticLexer lexer = new ElasticLexer(sql);
        lexer.nextToken();
        MySqlStatementParser mySqlStatementParser = new MySqlStatementParser(lexer);

        MySqlExprParser mySqlExprParser = mySqlStatementParser.getExprParser();
        SQLExpr expr = mySqlExprParser.expr();
        SQLQueryExpr sqlExpr = (SQLQueryExpr) expr;
        MySqlSelectQueryBlock query = (MySqlSelectQueryBlock) sqlExpr.getSubQuery().getQuery();


        List<SQLStatement> statementList = mySqlStatementParser.parseStatementList();
        StringBuilder out = new StringBuilder();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(out);
        for (SQLStatement statement : statementList) {
            statement.accept(visitor);
            //visitor.println();
        }
        System.out.println(out.toString());
    }


    @Test
    public void pdStr() {
        String tmp = "hello w o   !";
        char[] ch = tmp.toCharArray();
        int idx = ch.length - 1;
        int len = 0;
        char term;
        for (int i = idx; i >= 0; i--) {
            term = ch[i];
            if (Character.isAlphabetic(term)) {
                len += 1;
            } else if (term == ' ' && len != 0) {
                break;
            }
            System.out.println(term);
        }
        System.out.println("======");
        System.out.println(len);
        System.out.println("======");
    }

}
