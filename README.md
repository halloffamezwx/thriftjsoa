thriftjsoa是一个基于`apache thrift`的`SOA`框架，其中的j代表实现语言是`java`，之前在学习apache thrift时萌生了基于apache thrift来做一个服务治理框架，并且写了一系列博客：<http://zhuwx.iteye.com/category/365542>，大致的架构图如下：

![image](https://github.com/halloffamezwx/thriftjsoa/raw/master/doc/framework.png)

<b>一 使用方式</b>（参考test目录下的例子）：

<b><i>1</i></b> 编写接口定义文件`ThriftTest.thrift`，定义了一个接口getUser；
`ThriftTest.thrift`：
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
<b><i>2</i></b> 使用tools目录的`thrift.exe`执行命令`thrift --gen java ThriftTest.thrift`，生成文件thrift\test\ThriftTest.java和User.java，编写getUser接口的业务实现类`TestHandler.java`；

`TestHandler.java`：
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

<b><i>3</i></b> 编写getUser接口的业务实现类`TestHandler.java`，服务端`TestServer.java`和`spring-config-server.xml`，代理`TestProxy.java`和`spring-config-proxy.xml`，客户端`TestClient.java`和`spring-config-client.xml`；</br>

`TestServer.java`：
```java
public static void main(String[] args) throws Exception {
    AbstractApplicationContext context = new ClassPathXmlApplicationContext("spring-config-server.xml");
}
```

`spring-config-server.xml`
```xml
<context:component-scan base-package="com.halloffame.thriftjsoa"/> 
    
<bean id="testProcessor" class="thrift.test.ThriftTest.Processor">
    <constructor-arg name="iface" ref="testHandler"/> <!-- 业务实现类 -->
</bean>

<bean id="thirftJsoaServer" class="com.halloffame.thriftjsoa.ThirftJsoaServer" init-method="run"> <!-- 实例化成功后运行ThirftJsoaServer的run方法 -->
    <constructor-arg name="port" value="9090"/> <!-- 服务端口 -->
    <constructor-arg name="zkConnStr" value="localhost:2181"/> <!-- zk连接串 -->
    <constructor-arg name="ip" value="localhost"/> <!-- 向zk注册本服务的ip地址 -->
    <constructor-arg name="tProcessor" ref="testProcessor"/> <!-- 业务实现类的processor -->
</bean>
```

<b><i>4</i></b> 启动`zookeeper`（tools目录下有zk的安装文件`zookeeper-3.4.10.tar.gz`，解压即可），运行`TestServer.java`，看到日志`Starting the server on port 9090...`代表启动server成功，然后运行`TestProxy.java`，看到日志`Starting the proxy on port 4567...`代表启动proxy成功，运行`TestClient.java`，日志打印`名字：另外一个烟火`，结果符合预期。