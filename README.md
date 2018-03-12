thriftjsoa是一个基于`apache thrift`的`SOA`框架，其中的j代表实现语言是`java`，之前在学习apache thrift时萌生了基于apache thrift来做一个服务治理框架，并且写了一系列博客：<http://zhuwx.iteye.com/category/365542>，大致的架构图如下：

![image](https://github.com/halloffamezwx/thriftjsoa/raw/master/doc/framework.png)

<b>一 使用方式</b>（参考test目录下的例子）：</br>
<b><i>1</i></b> 编写接口定义文件`ThriftTest.thrift`，定义了一个接口getUser；</br>
<b><i>2</i></b> 使用tools目录的`thrift.exe`执行命令`thrift --gen java ThriftTest.thrift`，生成文件thrift\test\ThriftTest.java，User.java；</br>
<b><i>3</i></b> 编写getUser接口的业务实现类TestHandler.java，服务端TestServer.java和spring-config-server.xml，代理TestProxy.java和spring-config-proxy.xml，客户端TestClient.java和spring-config-client.xml；</br>
<b><i>4</i></b> 启动zookeeper（tools目录下有zk的安装文件zookeeper-3.4.10.tar.gz，解压即可），运行TestServer.java，看到日志`Starting the server on port 9090...`代表启动server成功，然后运行TestProxy.java，看到日志`Starting the proxy on port 4567...`代表启动proxy成功，运行TestClient.java，日志打印`名字：另外一个烟火`，结果符合预期。

