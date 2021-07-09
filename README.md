###1 简介：
thriftjsoa是一个基于`apache thrift`的`SOA`框架，其中的j代表实现语言是`java`，之前在学习apache thrift时萌生了基于apache thrift来做一个服务治理框架，
并且写了一系列博客：<http://zhuwx.iteye.com/category/365542>，大致的架构图如下：

![image](https://github.com/halloffamezwx/thriftjsoa/raw/master/doc/framework.png)

持续完善中，如果你觉得对你有所启发，帮忙star一下，谢谢！

###2 特性：
* 接入了spring boot
* 服务调用支持traceId功能
* 支持用java代码来定义接口，无需用thrift接口定义文件来生成代码
* 客户端支持连接池功能，可以用注解来自动关闭释放连接资源，类似事务注解@Transactional
* 负载均衡支持客户端和代理端，包括：随机，轮询，最小连接数。以上算法都可以选择是否加权
* 集成tomcat支持http（开发中。。。）
* 传输协议支持Protocol Buffers以及服务模式支持Netty（开发中。。。）
* 支持用注解的方式配置客户端（开发中。。。）

###3 使用方式：
服务端例子参考thriftjsoa-boot-server-test模块代码，代理端例子参考thriftjsoa-boot-proxy-test模块代码，客户端例子参考thriftjsoa-boot-client-test模块代码，
spring-boot-starter的maven依赖如下所示：
```xml
<dependency>
    <groupId>com.halloffame</groupId>
    <artifactId>thriftjsoa-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

####3.1 定义接口（两种实现方式）
① 编写接口定义文件UserService.thrift，定义了一个接口getUser。 使用tools目录的`thrift.exe`执行命令`thrift --gen java UserService.thrift`，
生成文件UserService.java和User.java。
```thrift
namespace java com.halloffame.thriftjsoa.sample.iface

struct User
{
    1: i32 id,
    2: string name
}

service UserService
{
    User getUser(1: i32 id)
}
```

② 直接用java代码的方式来定义接口，包括客户端和服务端的
```java
@Data
public class User {

    /**
     * id
     */
    private int id;

    /**
     * 名称
     */
    private String name;
}
```
```java
//客户端
public interface UserClient extends BaseClient<User> {

    /**
     * 获取用户
     */
    User getUser(int id);
}
```
```java
//服务端
public abstract class UserService extends BaseService<User> {

    /**
     * 获取用户
     */
    public abstract User getUser(int id);
}
```

####3.2 服务端实现
编写服务端spring boot工程的入口类，配置文件以及业务实现类，启动`zookeeper`（tools目录下有zk的安装文件`zookeeper-3.4.10.tar.gz`，解压即可），
然后启动spring boot工程，看到日志`Starting the server on port 9090...`代表server启动成功。

① 编写业务实现类，有两种方式：第一种是实现根据接口定义文件生成的接口`UserService.Iface`， 第二种是继承实现用java代码定义的抽象类`UserService`。
分别对应`3.1`的两种接口定义方式
```java
@Service
@Slf4j
//public class UserServiceImpl extends UserService { //java代码定义的接口
public class UserServiceImpl implements UserService.Iface {

    /**
     * 获取用户
     */
    @Override
    public User getUser(int id) {
        log.info("id={}", id);
        log.info("traceId={}", ThriftJsoaUtil.getTraceId());
        log.info("appId={}", ThriftJsoaUtil.getAppId());

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

② 编写spring boot工程入口类，其中TProcessor这个bean的定义可以用接口定义文件生成的也可以用java代码定义的，对应`3.2.1`的两种不同实现方式
```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public TProcessor tProcessor(UserService.Iface userService) {
        TProcessor tProcessor = new UserService.Processor(userService); //根据UserService.thrift生成的Processor
        /** tProcessor = new ThriftJsoaSessionProcessor<com.halloffame.thriftjsoa.sample.iface.session.UserService>(
         new com.halloffame.thriftjsoa.sample.iface.session.UserServiceImpl()); */
        return tProcessor;
    }
}
```

③ 编写spring boot工程配置文件
```yaml
thriftjsoa:
  server:
    threadedSelectorServerConfig: # 服务模式，默认ThreadedSelectorServerConfig
      port: 9090 # 服务端口，默认9090
      zkConnConfig: # 注册中心（zookeeper）连接配置
        zkConnStr: localhost:2181 # 连接串，默认localhost:2181
```

###3.3 代理端实现
编写代理端spring boot工程的入口类和配置文件，启动运行工程，看到日志`Starting the proxy on port 4567...`代表proxy启动成功。也可以不需要代理端，直接由客户端连接服务端。

① 编写spring boot工程入口类
```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

② 编写spring boot工程配置文件
```yaml
thriftjsoa:
  proxy:
    threadedSelectorServerConfig: # 服务模式，默认ThreadedSelectorServerConfig
      port: 4567 # 服务端口，默认9090
    loadBalanceClientConfig: # 负载均衡客户端配置，默认LoadBalanceClientConfig
      zkConnConfig: # 注册中心（zookeeper）连接配置，默认ZkConnConfig
        zkConnStr: localhost:2181 # 连接串，默认localhost:2181
      loadBalanceType: randomWeight # 负载均衡类型：leastConn, polling, random, leastConnWeight, pollingWeight, randomWeight(建议)，默认不指定
```

###3.4 客户端实现
编写客户端spring boot工程的入口类，配置文件，业务类以及测试用例。其中业务类引用client调用服务端接口的方式有两种，对应`3.1`的两种实现方式。
启动运行测试用例，日志打印`名字：另外一个烟火`，结果符合预期。

① 编写spring boot工程入口类
```java
@SpringBootApplication
@EnableThriftjsoaSession
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

② 编写spring boot工程配置文件
```yaml
thriftjsoa:
  client:
    list:
      - clazzs: # 客户端类列表，指定多个就是TMultiplexedProtocol
          - name: com.halloffame.thriftjsoa.sample.iface.UserService.Client # 客户端类的全路径
            sessionName: com.halloffame.thriftjsoa.sample.iface.session.UserClient # 非接口定义文件生成的client类
        clientConfigs: # 客户端配置列表，可以指定多个进行负载均衡
          - host: localhost # 主机地址，默认localhost
            port: 4567 # 主机端口，默认9090   
```

③ 编写业务类，包括接口
```java
public interface ClientTestService {

    /**
     * 客户端测试
     */
    void clientTest() throws Exception;
}
```
```java
@Service
@Slf4j
public class ClientTestServiceImpl implements ClientTestService {

    /**
     * 非接口定义文件生成的client对象
     */
    @Autowired
    private UserClient userClient;

    /**
     * 客户端测试
     */
    @OpenThriftjsoaSession
    @Override
    public void clientTest() throws Exception {
        UserService.Client generateUserClient = ThriftJsoaSessionData.SESSION_TL.get().createClient(UserService.Client.class);
        User generateUser = generateUserClient.getUser(2); //getUser就是UserService.thrift所定义的接口
        //ThriftJsoaSessionData.SESSION_TL.get().close(UserService.Client.class, true);
        log.info("名字：{}", generateUser.getName());
        log.info("traceId：{}", ThriftJsoaUtil.getTraceId());
        log.info("appId：{}", ThriftJsoaUtil.getAppId());

        com.halloffame.thriftjsoa.sample.iface.session.User user = userClient.getUser(2);
        //ThriftJsoaSessionData.SESSION_TL.get().close(UserClient.class, true);
        log.info("名字：{}", user.getName());
        log.info("traceId：{}", ThriftJsoaUtil.getTraceId());
        log.info("appId：{}", ThriftJsoaUtil.getAppId());
    }
}
```

④ 编写spring boot工程测试用例
```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

    @Autowired
    private ClientTestService clientTestService;

    @Test
    public void clientTest() throws Exception {
        clientTestService.clientTest();
    }
}
```