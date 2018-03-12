thriftjsoa是一个基于`apache thrift`的`SOA`框架，其中的j代表实现语言是`java`，之前在学习apache thrift时萌生了基于apache thrift来做一个服务治理框架，并且写了一系列博客：<http://zhuwx.iteye.com/category/365542>，大致的架构图如下：

![image](https://github.com/halloffamezwx/thriftjsoa/raw/master/doc/framework.png)

<b>一 使用方式：</b>

请参考test目录下的例子，例子中使用spring来实例化ThirftJsoaServer，ThirftJsoaProxy，testClient等，也可以不用spirng直接用new来实例化对象。

<b><i>1</i></b> 编写接口定义文件ThriftTest.thrift，定义了一个接口getUser。

```java
namespace java thrift.test

struct User
{
    1: i32 id,
    2: string name
}

service ThriftTest
{
    User getUser(1: i32 id)
}
```

<b><i>2</i></b> 使用tools目录的`thrift.exe`执行命令`thrift --gen java ThriftTest.thrift`，生成文件thrift\test\ThriftTest.java和User.java，编写getUser接口的业务实现类TestHandler.java。

```java
@Component //由spring容器实例化管理等
public class TestHandler implements ThriftTest.Iface {
    @Override
    public User getUser(int id) throws TException {
        System.out.println("id==>" + id); 
        if (id == 2 ) {
            User user = new User();
            user.setId(2);
            user.setName("另外一个烟火");
            return user;
        }
        return null;
    }
}
```

<b><i>3</i></b> 编写服务端TestServer.java和spring-config-server.xml。启动`zookeeper`（tools目录下有zk的安装文件`zookeeper-3.4.10.tar.gz`，解压即可），运行`TestServer.java`，看到日志`Starting the server on port 9090...`代表server启动成功。

[TestServer.java]
```java
public static void main(String[] args) throws Exception {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-server.xml");
}
```

[spring-config-server.xml]
```xml
<context:component-scan base-package="com.halloffame.thriftjsoa"/> 
    
<bean id="testProcessor" class="thrift.test.ThriftTest.Processor">
    <constructor-arg name="iface" ref="testHandler"/> <!-- 业务实现类 -->
</bean>

<bean id="thirftJsoaServer" class="com.halloffame.thriftjsoa.ThirftJsoaServer" init-method="run"> <!-- 实例化成功后运行ThirftJsoaServer的run方法 -->
    <constructor-arg name="port" value="9090"/> <!-- 服务端口 -->
    <constructor-arg name="zkConnStr" value="localhost:2181"/> <!-- zk连接串 -->
    <constructor-arg name="host" value="localhost"/> <!-- 向zk注册本服务的地址 -->
    <constructor-arg name="tProcessor" ref="testProcessor"/> <!-- 业务实现类的processor -->
</bean>
```

<b><i>4</i></b> 编写代理端TestProxy.java和spring-config-proxy.xml。运行`TestProxy.java`，看到日志`Starting the proxy on port 4567...`代表proxy启动成功。

[TestProxy.java]
```java
public static void main(String[] args) throws Exception {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-proxy.xml");
}
```

[spring-config-proxy.xml]
```xml
<bean id="thirftJsoaProxy" class="com.halloffame.thriftjsoa.ThirftJsoaProxy" init-method="run"> <!-- 实例化成功后运行ThirftJsoaProxy的run方法 -->
    <constructor-arg name="port" value="4567"/> <!-- 代理服务端口 -->
    <constructor-arg name="zkConnStr" value="localhost:2181"/> <!-- zk连接串 -->
</bean>
```

<b><i>5</i></b> 编写客户端TestClient.java和spring-config-client.xml（客户端不限语言，这里使用java）。运行`TestClient.java`，日志打印`名字：另外一个烟火`，结果符合预期。

[TestClient.java]
```java
public static final AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-client.xml");
	
public static void main(String [] args) throws Exception {
    ThriftTest.Client testClient = (ThriftTest.Client)context.getBean("testClient");
    
    //getUser就是ThriftTest.thrift所定义的接口
    User user = testClient.getUser(2); 
    System.out.println("名字："+ user.getName());

    testClient.getInputProtocol().getTransport().close();
}
```

[spring-config-client.xml]
```xml
<bean id="tSocket" class="org.apache.thrift.transport.TSocket" scope="prototype">
    <constructor-arg name="host" value="localhost"/> <!-- 连接代理服务的地址 -->
    <constructor-arg name="port" value="4567"/> <!-- 连接代理服务的端口 -->
    <property name="timeout" value="1000"/> <!-- 客户端读超时时间（毫秒） -->
</bean>

<!-- thrift的传输方式 ，实例化成功后运行TFastFramedTransport的open方法 -->
<bean id="tFastFramedTransport" class="org.apache.thrift.transport.TFastFramedTransport" init-method="open" destroy-method="close" scope="prototype">
    <constructor-arg name="underlying" ref="tSocket"/> 
</bean>

<!-- thrift的传输协议 -->
<bean id="tCompactProtocol" class="org.apache.thrift.protocol.TCompactProtocol" scope="prototype">
    <constructor-arg name="transport" ref="tFastFramedTransport"/> 
</bean>

<bean id="testClient" class="thrift.test.ThriftTest.Client" scope="prototype">
    <constructor-arg name="prot" ref="tCompactProtocol"/> 
</bean>
```

<b>二 工程目录的主要结构：</b>

|-thriftjsoa</br>
&nbsp;&nbsp;&nbsp;|-src/main/java：实现源码目录</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-com.halloffame.thriftjsoa</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-ConnectionPoolFactory.java：连接池工厂类，给ThirftJsoaProxy使用</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-ThirftJsoaProxy.java：代理的实现类</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-ThirftJsoaServer.java：服务的实现类</br>
&nbsp;&nbsp;&nbsp;|-src/test/java：测试例子</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-com.halloffame.thriftjsoa</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-TestClient.java：测试客户端</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-TestHandler.java：测试业务实现类，实现了ThriftTest.Iface接口</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-TestProxy.java：测试代理端</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-TestServer.java：测试服务端</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-thrift.test：由ThriftTest.thrift生成的代码</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-ThriftTest.java</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-User.java</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-spring-config-client.xml：测试客户端的spring配置文件</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-spring-config-proxy.xml：测试代理端的spring配置文件</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-spring-config-server.xml：测试服务端的spring配置文件</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-ThriftTest.thrift：接口定义文件</br>
&nbsp;&nbsp;&nbsp;|-pom.xml：依赖的库文件</br>
&nbsp;&nbsp;&nbsp;|-tools</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-thrift.exe：用这个来执行ThriftTest.thrift生成接口代码等</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-ZooInspector.zip：zk的一个客户端工具</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|-zookeeper-3.4.10.tar.gz：zk的安装文件，解压即可</br>
&nbsp;&nbsp;&nbsp;|-LICENSE：MIT协议

<b>三 持续完善中：</b>

server和proxy端的thrift的传输方式写死为TFastFramedTransport，传输协议写死为TCompactProtocol，服务模式写死为TThreadedSelectorServer，后续改成可配置的，包括proxy的连接池的一些配置等。proxy里面的负载均衡算法目前只有最小连接数（加权），后续扩展一下。