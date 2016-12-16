package org.elasticsearch.plugin.nlpcn.preAnalyzer;


import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

/**
 * Created by fangbb on 2016-11-25.
 */
public class AnsjElasticConfigurator {
    public static ESLogger logger = Loggers.getLogger("sql-init");
    public static Environment environment;
    public static String ES_IP = "";
    public static String ES_PORT = "";

    public static void init(Settings settings) {
        try {
            ES_IP = settings.get("network.host");
            ES_PORT = settings.get("http.port");
            if (ES_IP == null || ES_IP.equals("")) {
                logger.error("network.host获取失败");
            } else {
                logger.info("network.host：" + ES_IP);
            }
            if (ES_PORT == null || ES_PORT.equals("")) {
                ES_PORT = "9200";
                logger.error("http.port获取失败,使用默认端口:9200");
            } else {
                logger.info("http.port："+ ES_PORT);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
