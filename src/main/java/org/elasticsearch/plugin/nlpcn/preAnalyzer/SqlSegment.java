package org.elasticsearch.plugin.nlpcn.preAnalyzer;

import com.alibaba.druid.sql.SQLUtils;
import org.elasticsearch.plugin.nlpcn.preAnalyzer.AnsjElasticConfigurator;
import org.elasticsearch.plugin.nlpcn.request.HttpRequester;
import org.elasticsearch.plugin.nlpcn.request.HttpResponse;

import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fangbb on 2016-11-25.
 */
public class SqlSegment {
    public static String seg(String sql) {
        if (sql.contains("seg(")){
            String pattern = "( .*?)=(.*?)seg\\((.*?)\\)";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(sql);
            String pAll = "";
            String pName = "";
            String pFun = "";
            String pTerm = "";
            //System.out.println(sql);
            while (m.find()) {
                pAll = m.group();
                pName = m.group(1);
                pFun = m.group(2);
                pTerm = m.group(3);
                System.out.println("========");
                System.out.println(pAll);
                if (pFun.contains("=")) {
                    String pNew = pFun + "seg(" + pTerm + ")";
                    // System.out.println(pNew);
                    Matcher pm = p.matcher(pNew);
                    pm.find();
                    pAll = pm.group();
                    pName = pm.group(1);
                    pFun = pm.group(2);
                    pTerm = pm.group(3);
                }
                pName = parseName(pName);
                pFun = parseFun(pFun);
                if (pFun == null || pFun.equals("")) {
                    sql = replaceSql(sql, pAll, pName, pTerm);
                } else {
                    sql = replaceSql(sql, pAll, pName, pFun, pTerm);
                }
            }
        }
        return sql;
    }

    public static String replaceSql(String sql, String all, String name, String terms) {
        String[] termsArr = analyzer(terms);
        int size = termsArr.length - 1;
        String newAll = "";
        StringBuffer conBuffer = new StringBuffer();
        //String source = "";
        String source = name + "=" + "seg(" + terms + ")";
        for (int i = 0; i <= size; i++) {
            if (i == 0) {
                conBuffer.append(name).append("=").append(termsArr[i]);
            } else {
                conBuffer.append(" and ").append(name).append("=").append(termsArr[i]);
            }
        }
        newAll = all.replace(source, conBuffer.toString());
        sql = sql.replace(all, newAll);
        return sql;
    }

    public static String replaceSql(String sql, String all, String name, String fun, String terms) {
        String[] termsArr = analyzer(terms);
        int indexMax = termsArr.length - 1;
        String newAll = "";
        //String conditions = "";
        StringBuffer conBuffer = new StringBuffer();
        String source = "";
        source = name + "=" + fun + "(" + "seg(" + terms + ")";
        for (int i = 0; i <= indexMax; i++) {
            if (i == 0) { //第一个term
                if (indexMax == 0) { //共一个term
                    conBuffer.append(name).append("=").append(fun).append("(").append(termsArr[i]);
                } else {
                    conBuffer.append(name).append("=").append(fun).append("(").append(termsArr[i]).append(")");
                }
            } else if (i == indexMax) { //多个term时，最后一个
                conBuffer.append(" and ").append(name).append("=").append(fun).append("(").append(termsArr[i]);
            } else { //中间的term
                conBuffer.append(" and ").append(name).append("=").append(fun).append("(").append(termsArr[i]).append(")");
            }
        }
        newAll = all.replace(source, conBuffer.toString());
        sql = sql.replace(all, newAll);
        return sql;
    }


    public static String parseName(String name) {
        String[] tmp = name.split(",| ");
        int num = tmp.length;
        name = tmp[num - 1];
        return name;
    }

    public static String parseFun(String fun) {
        if (fun == null || fun.equals("")) {
            fun = "";
        } else {
            fun = fun.replace("(", "");
        }
        return fun;
    }

    public static String parseTerm(String term) {
        if (term != null || term.equals("")) {
            term = term.replace("\"", "");
        }
        return term;
    }

    public static String[] analyzer(String term) {
        //TODO done
        term = term.replaceAll("\"", "");
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
                sb.append("\"").append(term).append("\"");
            } else {
                sb.append(",\"").append(term).append("\"");
            }
        }
        return sb.toString();
    }

}
