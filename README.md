# webgame-log
本项目是依赖storm实时处理框架，实时从redis中获取日志，并且记录数据库，同时实时统计相应指标

代码结构：
所有源代码都在：src/main/java/com/yuhe/szml/路径下

bolt：
LogBolt.java:日志记录入库处理的bolt代码，采用工厂模式，具体每个日志处理的逻辑都在log_modules文件夹下面实现
StaticsBolt.java:日志数据统计处理的bolt代码，同样采用工厂模式，具体实现在statics文件夹下面
	
db：
statics：统计数据入库的操作逻辑，具体下面每个文件对应一个表
log：日志数据入库的操作逻辑，具体下面每个文件对应一个表
DBManager.java：mysql连接池封装逻辑
ServerDB.java:服务器列表相关逻辑，例如获得统计服，服务器对应配置信息等
	
log_modules：
日志处理的逻辑代码，每新增一个日志处理逻辑需要在LogIndexes.java添加相关配置
	
statics_modules：
日志统计的逻辑代码，每新增一个日志统计逻辑需要在StaticsIndexes.java添加相关配置
	
utils：通用操作方法封装
DateUtils2.java：日期封装类
RegUtils.java：日志提取封装类，例如从日志中提取对应字段，时间等信息
	
spout:
RedisSpout.java:对应redis的spout类
	
StaticsTopology.java：入口类，配置相关spout和bolt的顺序以及执行线程数

部署：
与storm采用maven打包部署一样，将打包部署好的jar包之间部署至服务器，执行命令：
bin/storm jar szml-1.0-jar-with-dependencies.jar com.yuhe.szml.StaticsTopology oss-statics

停止命令：
bin/storm kill oss-statics
