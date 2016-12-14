package org.elasticsearch.plugin.nlpcn.preAnalyzer;

/**
 * Created by linxueqing on 2016/12/13.
 */
public interface Analyzer {
    public String[] analyzer(String term) throws Exception;
}
