# daydayfund
天天基金网爬虫，可多ip节点部署，按ip节点动态调整并发数，可设置代理ip反爬，定时爬取，定时通过邮件反馈基金行情统计数据

# 有什么疑问可以加我微信 da20120901


#启动步骤  
1.设置 application.properties 中的配置信息  
2.设置 quartz-config.xml 中爬虫任务的执行时间  
3.启动redis,将resources目录下的邮件模板代码放到redis中  
4.启动爬虫程序，等待爬取任务结束，接收邮件(爬取时间跟网速带宽相关，一般10-15分钟左右，若有多个服务器，爬取速度更快)  

#更新信息
2020.02.06  
1.解决多节点部署时误删已有队列数据的问题  
2.监控线程添加进度百分比日志打印  
3.删除不必要的多线程并发控制代码  
4.删除不必要的日志打印

#更新信息
2020.03.08  
1.优化并发控制，通过修改redis中并发数量配置实时调整不同实例的并发数量
 
#更新信息
2021.02.16  
1.新增利用redis命令setnx的特性，防止多实例同时启动时重复拉取基金列表的问题  
2.优化线程池满载后的处理策略，利用线程池自定义拒绝策略将元素重新放回队列，  
并设置等待时间暂缓往线程池提交任务  
3.优化线程池最大线程数的动态配置方式，守护线程定时读取redis配置，  
利用线程池自身提供的setMaximumPoolSize方法动态更改线程池最大线程数大小  
4.多个线程使用同个HttpClient进行请求，减少HttpClient实例的创建数量，减少内存消耗；  
设置HttpClient连接池配置，使其支持更大的并发请求操作  
5.新增errorDataQueue队列，存放爬取过程中发生异常的基金信息  


#如果你想一键三连，可以通过以下方式  
![image](http://assets.processon.com/chart_image/602b9bba07912934224ab163.png)
