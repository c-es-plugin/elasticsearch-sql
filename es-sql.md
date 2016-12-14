# es-sql

## 编译和安装  

### 编译

	mvn clean package assembly:single -DskipTests

### 安装方法： 
 
	unzip elasticsearch-sql-2.3.1.1.zip  
	mv elasticsearch-sql-2.3.1.1 sql  
	rm elasticsearch-sql-2.3.1.1.zip  

或者

	./bin/plugin -u file:///home/omershelef/IdeaProjects/elasticsearch-sql/target/elasticsearch-sql-1.3.2.zip --install sql

### es启动方法：

	../bin/elasticsearch -Des.security.manager.enabled=false


## 特性

### SQL 特性

	*  SQL Select
	*  SQL Where
	*  SQL Order By
	*  SQL Group By
	*  SQL AND & OR
	*  SQL Like
	*  SQL COUNT distinct
	*  SQL In
	*  SQL Between
	*  SQL Aliases
	*  SQL Not Null
	*  SQL(ES) Date
	*  SQL avg()
	*  SQL count()
	*  SQL last()
	*  SQL max()
	*  SQL min()
	*  SQL sum()
	*  SQL Nulls
	*  SQL isnull()
	*  SQL now()


## 增强SQL特性

	*  ES nested
	*  ES seg
	*  ES TopHits
	*  ES MISSING
	*  ES STATS
	*  ES GEO_INTERSECTS
	*  ES GEO_BOUNDING_BOX
	*  ES GEO_DISTANCE
	*  ES GEOHASH_GRID aggregation


## 接口说明:  

###  解析sql 2 sql 接口：

	http://192.168.25.11:9688/_sql/_seg?sql=select * from test where province="河北省"


### 解析sql 2 es 接口：

	http://localhost:9200/_sql/_explain?sql=select * from indexName limit 10

### 执行sql地址：

	http://localhost:9200/_sql?sql=select * from indexName limit 10

### UI 

	http://192.168.25.11:9688/_plugin/sql/

## 例子：

### 不使用函数

当不使用任何的函数时，默认是使用的match查询，match则会进行分析器处理，分析器中的分词器会将搜索关键字分割成单独的词(terms)或者标记(tokens) 。
该match的type是phrase，phrase表示确切的匹配若干个单词或短语, 如title: “brown dog”, 则查询title中包含brown和dog, 且两个是连接在一起的

例子：

	SELECT * FROM test_csdn_user_profile_12_201512_v4 where title = "brown dog"

### matchQuery()	

matchQuery()使用的type是boolean。
函数中可指定两个参数，不可指定operator和minimum_should_match，除了可以指定分词器外，和不使用函数没有区别： 

* 第一个参数是：查询词
* 第二个参数是：analyzer，有三个值可供选择：query_ansj、dic_ansj、index_ansj

例子：

	select *  from user_metric where province = matchQuery("河北")


### term()

term查找时内容精确匹配，只有一个参数，即需要查询的词

例子：

	select *  from user_metric where province = term("河北")

### IN_TERMS()

在函数中可以指定多个词，进行查询

例子：

	SELECT * FROM test_csdn_user_profile_12_201512_v4 where province = IN_TERMS("河北省","北京市")


### 根据Id查询 IDS_QUERY()

可指定多个id进行查询，函数中第一个参数是type，后边是需要指定的id，对于type加不加双引号均可

例子：

	select * from %s/dog where _id = IDS_QUERY(dog,1,2,3)
	SELECT * FROM test_csdn_user_profile_12_201512_v4 where _id = IDS_QUERY("user","azjw1989","jslp1990") 
	
### 模糊匹配 like 

* *和%代表任意个字符（包括空字符） 
* ?问号是单个字符  
请注意，此查询可能很慢，因为它需要迭代许多项。 为了防止极慢的通配符查询，通配符术语不应以通配符*或？开头。  

例子：  

	SELECT * FROM user_metric WHERE province LIKE "邯郸%%"
	
### in

使用match进行多个值查询，各个值间是或的关系  

例子：

	SELECT * FROM user_profile_12_201512_v4 where province in ("河北省","北京市")

### 嵌套类型 nested()

nested方法可在where和order by中使用。  
在where中有两个参数：
* 第一个参数是：父field
* 第二个参数是：子filed的表达式

在order by中有三个参数：
* 第一个参数是：父field
* 第二个参数是：指定排序字段，以及排序函数，可使用sum、min、max、avg四个函数
* 第三个参数是：子filed的表达式

例子：

	SELECT * FROM elasticsearch-sql_test_index where nested(message,message.info=term("c")) and nested(message,message.info=term("a")) order by nested(message, sum(message.dayOfWeek),message.info=term("a") and message.info=term("c")) desc
	
### object对象

非嵌套的object，直接使用  父field.子field 即可查询

例子：
	SELECT * FROM elasticsearch-sql_test_index where message.info=term("c")


http://192.168.25.11:9688/_sql/_seg?sql=select%20*%20from%20awhere nested(a,(a.b="x" or a.b=seg"y")and a.b=seg"z")

