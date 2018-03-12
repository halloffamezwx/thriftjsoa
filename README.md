thriftjsoa是一个基于`apache thrift`的`SOA`框架，其中的j代表实现语言是`java`，之前在学习apache thrift时萌生了基于apache thrift来做一个服务治理框架，并且写了一系列博客：<http://zhuwx.iteye.com/category/365542>，大致的架构图如下：

![image](https://github.com/halloffamezwx/thriftjsoa/raw/master/doc/framework.png)

<b>一 使用方式</b>（请参考test目录下的例子）：

<b><i>1</i></b> 编写接口定义文件ThriftTest.thrift，定义了一个接口getUser。

<b><i>`ThriftTest.thrift`：
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

<b><i>`TestHandler.java`：
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

<b><i>3</i></b> 编写服务端TestServer.java和spring-config-server.xml，代理端TestProxy.java和spring-config-proxy.xml，客户端TestClient.java和spring-config-client.xml。

<b><i>`[TestServer.java]`</i></b>：
```java
public static void main(String[] args) throws Exception {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-server.xml");
}
```

<b><i>`[spring-config-server.xml]`</i></b>：
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

<b><i>`[TestProxy.java]`</i></b>：
```java
public static void main(String[] args) throws Exception {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-proxy.xml");
}
```

<b><i>`[spring-config-proxy.xml]`</i></b>：
```xml
<bean id="thirftJsoaProxy" class="com.halloffame.thriftjsoa.ThirftJsoaProxy" init-method="run"> <!-- 实例化成功后运行ThirftJsoaProxy的run方法 -->
    <constructor-arg name="port" value="4567"/> <!-- 代理服务端口 -->
    <constructor-arg name="zkConnStr" value="localhost:2181"/> <!-- zk连接串 -->
</bean>
```

<b><i>`[TestClient.java]`</i></b>：
```java
public static final AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-client.xml");
	
public static void main(String [] args) throws Exception {
    ThriftTest.Client testClient = (ThriftTest.Client)context.getBean("testClient");
    
    //getUser就是ThriftTest.thrift所定义的接口
    User user = testClient.getUser(2); 
    System.out.println("名字："+ user.getName());
    
    //context.registerShutdownHook();
    //context.close();
    testClient.getInputProtocol().getTransport().close();
}
```

<b><i>`[spring-config-client.xml]`</i></b>：
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

<b><i>4</i></b> 启动`zookeeper`（tools目录下有zk的安装文件`zookeeper-3.4.10.tar.gz`，解压即可），运行`TestServer.java`，看到日志`Starting the server on port 9090...`代表server启动成功，然后运行`TestProxy.java`，看到日志`Starting the proxy on port 4567...`代表proxy启动成功，运行`TestClient.java`，日志打印`名字：另外一个烟火`，结果符合预期。

<b>二 例子中使用spring来实例化ThirftJsoaServer，ThirftJsoaProxy，testClient等，也可以不用spirng直接new一个</b>

<b>三 server和proxy端的thrift的传输方式写死为TFastFramedTransport，传输协议写死为TCompactProtocol，服务模式写死为TThreadedSelectorServer，后续改成可配置的，包括proxy的连接池的一些配置等，持续完善中。</b>
