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
/**
 * 具体的业务逻辑类
 * 实现ThriftTest.thrift里的getUser接口
 */
@Component //由spring容器实例化管理等
public class TestHandler implements ThriftTest.Iface {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());
	
	@Override
	public User getUser(int id) {
		LOGGER.info("id==>{}", id);
		System.out.println("traceId=" + MDC.get(ThirftJsoaProtocol.TRACE_KEY));
		System.out.println("appId=" + MDC.get(ThirftJsoaProtocol.APP_KEY));
		if (id == 2) {
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
public static void main(String[] args) {
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
    <constructor-arg name="tProcessor" ref="testProcessor"/> <!-- 业务实现类的processor -->
</bean>
```

<b><i>4</i></b> 编写代理端TestProxy.java和spring-config-proxy.xml。运行`TestProxy.java`，看到日志`Starting the proxy on port 4567...`代表proxy启动成功。

[TestProxy.java]
```java
public static void main(String[] args) {
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
/**
 * 客户端（测试）
 */
@Component
public class TestClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class.getName());

    public static void main(String [] args) throws Exception {
        AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-client.xml");
        TestClient testClient = context.getBean(TestClient.class);
        testClient.test();
    }

    /**
     * test方法会被ClientAspect拦截，然后根据配置创建ThriftTest.Client对象保存在CommonClient的ThreadLocal变量里
     * CommonClient.getClient将从ThreadLocal变量里取得ThriftTest.Client对象
     * ThriftTest.Client对象的资源释放将在ClientAspect的finally块里进行
     */
    @ClientAnnotation(clientClassArr = {ThriftTest.Client.class})
    public void test() throws Exception {
        ThriftTest.Client thriftTestClient = CommonClient.getClient(ThriftTest.Client.class);
        User user = thriftTestClient.getUser(2); //getUser就是ThriftTest.thrift所定义的接口
        LOGGER.info("名字：{}", user.getName());
    }
}
```

[spring-config-client.xml]
```xml
<context:component-scan base-package="com.halloffame.thriftjsoa"/>
<!-- 启用Spring对基于@AspectJ aspects的配置支持 -->
<!-- 激活自动代理功能 -->
<aop:aspectj-autoproxy proxy-target-class="true"/>

<bean id="clientClassConfig" class="com.halloffame.thriftjsoa.config.ClientClassConfig">
    <constructor-arg name="clazz" value="thrift.test.ThriftTest.Client"/> <!-- 客户端class -->
</bean>

<bean id="clientConfig" class="com.halloffame.thriftjsoa.config.ClientConfig">
    <constructor-arg name="host" value="localhost"/> <!-- 连接代理服务的地址 -->
    <constructor-arg name="port" value="4567"/> <!-- 连接代理服务的端口 -->
    <constructor-arg name="clientClassConfigs"> <!-- 生成的客户端的class list -->
        <list>
            <ref bean="clientClassConfig"/>
        </list>
    </constructor-arg>
</bean>
```

<b>二 持续完善中：如果你觉得对你有所启发，star一下，谢谢！</b>