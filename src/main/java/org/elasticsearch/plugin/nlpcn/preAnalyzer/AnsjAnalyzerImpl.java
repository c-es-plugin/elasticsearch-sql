package org.elasticsearch.plugin.nlpcn.preAnalyzer;

import org.elasticsearch.plugin.nlpcn.request.HttpRequester;
import org.elasticsearch.plugin.nlpcn.request.HttpResponse;

import java.net.URLEncoder;

/**
 * Created by fangbb on 2016-12-12.
 */
public class AnsjAnalyzerImpl implements Analyzer {

    public String[] analyzer(String term) throws Exception {
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
            //String preUrl = "http://" + ip + ":" + port + midUrl;
            String preUrl = "http://192.168.25.11:9688/_cat/analyze?analyzer=query_ansj&text=";
            //System.out.println(preUrl);
            String enTerm = URLEncoder.encode(term, "UTF-8");
            String url = preUrl + enTerm;
            //System.out.println(url);
            response = request.sendGet(url);
            if (response.getCode() == 200) {
                if (response != null && response.getContent().length() > 10) {
                    sourceTerms = response.getContent();
                }
            }

        } catch (Exception e) {
            throw new Exception("There is an error in the word segmentation");
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
