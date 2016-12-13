package org.nlpcn.es4sql;

import org.elasticsearch.plugin.nlpcn.preAnalyzer.Analyzer;
import org.elasticsearch.plugin.nlpcn.preAnalyzer.AnsjAnalyzer;
import org.elasticsearch.plugin.nlpcn.preAnalyzer.SqlParseAnalyzer;
import org.junit.Test;

/**
 * Created by fangbb on 2016-12-12.
 */
public class PreAnalyzer {
    @Test
    public void SqlParseAnalyzerTest(){
        try {
            String sql = "SELECT * FROM test where nested(a,a.c=term(seg(\"百度和谷歌\")) and a.c=\"中国\") or nested(a,a.c=term(seg(\"java hadoop\")))";
            //String sql = "SELECT * FROM test where nested(a,a.c=term(seg(\"百度和谷歌\")) and a.c=seg(\"中国\")) and a=\"hadop\"";
            //String sql = "SELECT * FROM test where nested(a,a.c=term(seg(\"百度\"))) ";
            //String sql = "SELECT * FROM test where nested(a,a.c=seg(\"百度\"))";
            //String sql = "SELECT * FROM test where nested(a,a.c=term(\"中国\"))";
            //String sql = "SELECT * FROM test where a is not null and b=\"xxx\" and nested(a,a.c=term(seg(\"百度\")))";
            //String sql = "SELECT * FROM test where a is not null and b=\"xxx\" and nested(a,a.c=term(seg(\"百度和谷歌\")) and a.c=term(seg(\"hadoop java\")) and a.c=term(seg(\"test\")))";
            //String sql = "SELECT * FROM test where a is not null and b=\"xxx\" and nested(a,a.c=\"abc\") order by nested(a,sum(a.b),a.c=term(seg(\"百度和谷歌\")))";
            //String sql = "SELECT * FROM test where nested(a,a.c=\"abc\") order by nested(a,sum(a.b),a.c=term(seg(\"百度和谷歌\")))";
            //String sql = "SELECT * FROM test where nested(a,a.c=term(seg(\"百度和谷歌\"))) ";

            SqlParseAnalyzer sqlParseAnalyzer = new SqlParseAnalyzer(new AnsjAnalyzer());
            String ret = sqlParseAnalyzer.seg(sql);
            System.out.println(ret);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
