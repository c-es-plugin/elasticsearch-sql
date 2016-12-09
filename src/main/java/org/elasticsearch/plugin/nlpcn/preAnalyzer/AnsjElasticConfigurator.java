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
            logger.info("es 本地IP：" + ES_IP);
            logger.info("es 端口："+ ES_PORT);
            if (ES_IP==null || ES_IP.equals("")){
                logger.error("本地IP获取失败");
            }
            if (ES_PORT==null || ES_PORT.equals("")){
                logger.error("ES服务获取失败");
            }
        } catch (Exception e) {
            logger.error("本地IP和ES端口获取失败");
        }
    }
}
