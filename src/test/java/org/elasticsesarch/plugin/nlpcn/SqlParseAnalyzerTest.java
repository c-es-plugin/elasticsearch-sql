package org.elasticsesarch.plugin.nlpcn;

import org.elasticsearch.plugin.nlpcn.preAnalyzer.Analyzer;
import org.elasticsearch.plugin.nlpcn.preAnalyzer.SqlParseAnalyzer;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Created by linxueqing on 2016/12/13.
 */
public class SqlParseAnalyzerTest {
    public class MockAnalyzer extends Analyzer {
        @Override
        public String[] analyzer(String term) throws Exception {
            return new String[]{"大数据", "持续集成"};
        }
    }

    @Test
    public void withoutSegKeyword() throws Exception {
        SqlParseAnalyzer sqlParseAnalyzer = new SqlParseAnalyzer(new MockAnalyzer());
        String inputSql = "select * from a";
        String expectedOutputSql = inputSql;
        String outputSql = sqlParseAnalyzer.seg(inputSql);
        assertEquals(expectedOutputSql, outputSql);
    }

    @Test
    public void segOneTerm() throws Exception {
        SqlParseAnalyzer sqlParseAnalyzer = new SqlParseAnalyzer(new MockAnalyzer());
        String inputSql = "select * from a where name=seg(\"大数据持续集成\") and age > 18 order by age desc";
        String outputSql = sqlParseAnalyzer.seg(inputSql);
        String expectedOutputSql =  "SELECT *\n" +
                                    "FROM a\n" +
                                    "WHERE name = '大数据'\n" +
                                    "\tAND name = '持续集成'\n" +
                                    "\tAND age > 18\n" +
                                    "ORDER BY age DESC";
        assertEquals(expectedOutputSql, outputSql);
    }

    @Test
    public void segOneNestedTerm() throws Exception {
        SqlParseAnalyzer sqlParseAnalyzer = new SqlParseAnalyzer(new MockAnalyzer());
        String inputSql = "select * from a where nested(b, b.name=seg(\"大数据持续集成\")) and age > 18 order by age desc";
        String outputSql = sqlParseAnalyzer.seg(inputSql);
        String expectedOutputSql =  "SELECT *\n" +
                                    "FROM a\n" +
                                    "WHERE nested(b, b.name = '大数据')\n" +
                                    "\tAND nested(b, b.name = '持续集成')\n" +
                                    "\tAND age > 18\n" +
                                    "ORDER BY age DESC";
        assertEquals(expectedOutputSql, outputSql);
    }

    @Test
    public void segNestedOrder() throws Exception {
        SqlParseAnalyzer sqlParseAnalyzer = new SqlParseAnalyzer(new MockAnalyzer());
        String inputSql = "SELECT * FROM a where nested(b,b.c=\"abc\") order by nested(a,sum(a.b),a.c=term(seg(\"大数据持续集成\")))";
        String outputSql = sqlParseAnalyzer.seg(inputSql);
        String expectedOutputSql =  "SELECT *\n" +
                                    "FROM a\n" +
                                    "WHERE nested(b, b.c = 'abc')\n" +
                                    "ORDER BY nested(a, SUM(a.b), a.c = term('大数据')\n" +
                                    "AND a.c = term('持续集成'))";
        assertEquals(expectedOutputSql, outputSql);
    }

    @Test
    public void segMoreThanOneTerm() throws Exception {
        SqlParseAnalyzer sqlParseAnalyzer = new SqlParseAnalyzer(new MockAnalyzer());
        String inputSql = "SELECT * FROM test where nested(a,a.c=term(seg(\"大数据持续集成\")) and a.c=\"中国\") or nested(a,a.c=term(seg(\"大数据持续集成\")))";
        String outputSql = sqlParseAnalyzer.seg(inputSql);
        String expectedOutputSql =  "SELECT *\n" +
                                    "FROM test\n" +
                                    "WHERE nested(a, a.c = term('大数据'))\n" +
                                    "\tAND nested(a, a.c = term('持续集成'))\n" +
                                    "\tAND nested(a, a.c = '中国')\n" +
                                    "\tOR nested(a, a.c = term('大数据'))\n" +
                                    "\tAND nested(a, a.c = term('持续集成'))";
        assertEquals(expectedOutputSql, outputSql);
        System.out.println(outputSql);
    }
}
