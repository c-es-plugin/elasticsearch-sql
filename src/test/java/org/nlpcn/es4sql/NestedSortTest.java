package org.nlpcn.es4sql;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;
import org.junit.Assert;
import org.junit.Test;
import org.nlpcn.es4sql.query.QueryAction;
import org.nlpcn.es4sql.query.SqlElasticRequestBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * Created by fangbb on 2016-12-12.
 */
public class NestedSortTest {
    
    @Test
    public void nestedSort4MatchQueryTest(){
        String sql = "SELECT * FROM test order by nested(a,sum(a.b),a.c=matchQuery(\"百度和谷歌\"))";
        String actual = exec(sql);
        String expected = "{\n" +
                "  \"from\" : 0,\n" +
                "  \"size\" : 200,\n" +
                "  \"sort\" : [ {\n" +
                "    \"a.b\" : {\n" +
                "      \"order\" : \"asc\",\n" +
                "      \"missing\" : \"_first\",\n" +
                "      \"mode\" : \"SUM\",\n" +
                "      \"nested_filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : {\n" +
                "            \"match\" : {\n" +
                "              \"a.c\" : {\n" +
                "                \"query\" : \"百度和谷歌\",\n" +
                "                \"type\" : \"boolean\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"nested_path\" : \"a\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void nestedSort4TermTest(){
        String sql = "SELECT * FROM test order by nested(a,sum(a.b),a.c=term(\"百度和谷歌\"))";
        String actual = exec(sql);
        String expected = "{\n" +
                "  \"from\" : 0,\n" +
                "  \"size\" : 200,\n" +
                "  \"sort\" : [ {\n" +
                "    \"a.b\" : {\n" +
                "      \"order\" : \"asc\",\n" +
                "      \"missing\" : \"_first\",\n" +
                "      \"mode\" : \"SUM\",\n" +
                "      \"nested_filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : {\n" +
                "            \"term\" : {\n" +
                "              \"a.c\" : \"百度和谷歌\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"nested_path\" : \"a\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void nestedSortDefaultTest(){
        String sql = "SELECT * FROM test order by nested(a,sum(a.b),a.c=\"百度和谷歌\")";
        String actual = exec(sql);
        String expected = "{\n" +
                "  \"from\" : 0,\n" +
                "  \"size\" : 200,\n" +
                "  \"sort\" : [ {\n" +
                "    \"a.b\" : {\n" +
                "      \"order\" : \"asc\",\n" +
                "      \"missing\" : \"_first\",\n" +
                "      \"mode\" : \"SUM\",\n" +
                "      \"nested_filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : {\n" +
                "            \"match\" : {\n" +
                "              \"a.c\" : {\n" +
                "                \"query\" : \"百度和谷歌\",\n" +
                "                \"type\" : \"phrase\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"nested_path\" : \"a\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        Assert.assertEquals(expected,actual);
    }
    @Test
    public void nestedSortRealTest(){
        String sql = "select * from test where nested(info,info.name=term(\"java\") and info.name=term(\"python\") or info.name=term(\"Hadoop\")) order by nested(info,sum(info.age),info.name=matchQuery(\"java\") and info.name=term(\"python\") and info.name=\"Hadoop\") desc,score desc";
        String actual = exec(sql);
        String expected = "{\n" +
                "  \"from\" : 0,\n" +
                "  \"size\" : 200,\n" +
                "  \"query\" : {\n" +
                "    \"bool\" : {\n" +
                "      \"must\" : {\n" +
                "        \"nested\" : {\n" +
                "          \"query\" : {\n" +
                "            \"bool\" : {\n" +
                "              \"must\" : {\n" +
                "                \"bool\" : {\n" +
                "                  \"should\" : [ {\n" +
                "                    \"bool\" : {\n" +
                "                      \"must\" : [ {\n" +
                "                        \"term\" : {\n" +
                "                          \"info.name\" : \"java\"\n" +
                "                        }\n" +
                "                      }, {\n" +
                "                        \"term\" : {\n" +
                "                          \"info.name\" : \"python\"\n" +
                "                        }\n" +
                "                      } ]\n" +
                "                    }\n" +
                "                  }, {\n" +
                "                    \"term\" : {\n" +
                "                      \"info.name\" : \"Hadoop\"\n" +
                "                    }\n" +
                "                  } ]\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"path\" : \"info\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"sort\" : [ {\n" +
                "    \"info.age\" : {\n" +
                "      \"order\" : \"desc\",\n" +
                "      \"missing\" : \"_last\",\n" +
                "      \"mode\" : \"SUM\",\n" +
                "      \"nested_filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : [ {\n" +
                "            \"match\" : {\n" +
                "              \"info.name\" : {\n" +
                "                \"query\" : \"java\",\n" +
                "                \"type\" : \"boolean\"\n" +
                "              }\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"term\" : {\n" +
                "              \"info.name\" : \"python\"\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"match\" : {\n" +
                "              \"info.name\" : {\n" +
                "                \"query\" : \"Hadoop\",\n" +
                "                \"type\" : \"phrase\"\n" +
                "              }\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      },\n" +
                "      \"nested_path\" : \"info\"\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"score\" : {\n" +
                "      \"order\" : \"desc\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void nestedSortFactTest(){
        String sql = "SELECT * FROM fact_index where nested(interest_keyword,interest_keyword.keyword=matchQuery(\"黑白\")) and nested(interest_keyword,interest_keyword.keyword=matchQuery(\"纪实\")) and province = \"北京\" order by nested(interest_keyword, sum(interest_keyword.score),interest_keyword.keyword=matchQuery(\"黑白\") and interest_keyword.keyword=matchQuery(\"纪实\")) desc";
        String actual = exec(sql);
        String expected = "{\n" +
                "  \"from\" : 0,\n" +
                "  \"size\" : 200,\n" +
                "  \"query\" : {\n" +
                "    \"bool\" : {\n" +
                "      \"must\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"must\" : [ {\n" +
                "            \"nested\" : {\n" +
                "              \"query\" : {\n" +
                "                \"bool\" : {\n" +
                "                  \"must\" : {\n" +
                "                    \"match\" : {\n" +
                "                      \"interest_keyword.keyword\" : {\n" +
                "                        \"query\" : \"黑白\",\n" +
                "                        \"type\" : \"boolean\"\n" +
                "                      }\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              },\n" +
                "              \"path\" : \"interest_keyword\"\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"nested\" : {\n" +
                "              \"query\" : {\n" +
                "                \"bool\" : {\n" +
                "                  \"must\" : {\n" +
                "                    \"match\" : {\n" +
                "                      \"interest_keyword.keyword\" : {\n" +
                "                        \"query\" : \"纪实\",\n" +
                "                        \"type\" : \"boolean\"\n" +
                "                      }\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              },\n" +
                "              \"path\" : \"interest_keyword\"\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"match\" : {\n" +
                "              \"province\" : {\n" +
                "                \"query\" : \"北京\",\n" +
                "                \"type\" : \"phrase\"\n" +
                "              }\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"sort\" : [ {\n" +
                "    \"interest_keyword.score\" : {\n" +
                "      \"order\" : \"desc\",\n" +
                "      \"missing\" : \"_last\",\n" +
                "      \"mode\" : \"SUM\",\n" +
                "      \"nested_filter\" : {\n" +
                "        \"bool\" : {\n" +
                "          \"should\" : [ {\n" +
                "            \"match\" : {\n" +
                "              \"interest_keyword.keyword\" : {\n" +
                "                \"query\" : \"'黑白'\",\n" +
                "                \"type\" : \"boolean\"\n" +
                "              }\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"match\" : {\n" +
                "              \"interest_keyword.keyword\" : {\n" +
                "                \"query\" : \"'纪实'\",\n" +
                "                \"type\" : \"boolean\"\n" +
                "              }\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      },\n" +
                "      \"nested_path\" : \"interest_keyword\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        Assert.assertEquals(expected,actual);
    }


    public static String exec(String sql){
        String jsonExplanation = "";
        try {
            TransportClient client;
            client = TransportClient.builder().addPlugin(DeleteByQueryPlugin.class).build().addTransportAddress(getTransportAddress());
            //Long now = System.currentTimeMillis();
            SearchDao searchDao = new SearchDao(client);
            QueryAction queryAction = searchDao.explain(sql);
            SqlElasticRequestBuilder xx = queryAction.explain();
            jsonExplanation = xx.explain();
            //System.out.println(System.currentTimeMillis() - now + " ms");
            //System.out.println(jsonExplanation);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  jsonExplanation;
    }

    public static InetSocketTransportAddress getTransportAddress() throws UnknownHostException {

        String host = "192.168.25.11";
        String port = "9300";
        if (host == null) {
            host = "localhost";
            System.out.println("ES_TEST_HOST enviroment variable does not exist. choose default 'localhost'");
        }

        if (port == null) {
            port = "9300";
            System.out.println("ES_TEST_PORT enviroment variable does not exist. choose default '9300'");
        }

        System.out.println(String.format("Connection details: host: %s. port:%s.", host, port));
        return new InetSocketTransportAddress(InetAddress.getByName(host), Integer.parseInt(port));
    }


}
