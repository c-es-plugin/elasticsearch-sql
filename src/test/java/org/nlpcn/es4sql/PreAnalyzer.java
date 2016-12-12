package org.nlpcn.es4sql;

import org.elasticsearch.plugin.nlpcn.preAnalyzer.SqlParseAnalyzer;
import org.junit.Test;

/**
 * Created by fangbb on 2016-12-12.
 */
public class PreAnalyzer {
    @Test
    public void SqlParseAnalyzerTest(){
        try {
            String sql = "SELECT * FROM test where nested(a,a.c=term(seg(\"百度和谷歌\"))) order by nested(a,sum(a.b),a.c=term(seg(\"java hadoop\")) and a.c=term(seg(\"百度和谷歌\")) or a.c=term(\"test\"))";
            String ret = SqlParseAnalyzer.seg(sql);
            System.out.println(ret);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
