### 1 简介：
thriftjsoa是一个基于`apache thrift`的`SOA`框架，其中的j代表实现语言是`java`，之前在学习apache thrift时萌生了基于apache thrift来做一个服务治理框架，
并且写了一系列博客：<http://zhuwx.iteye.com/category/365542>，大致的架构图如下：

![image](https://github.com/halloffamezwx/thriftjsoa/raw/master/doc/framework.png)

持续完善中，如果你觉得对你有所启发，帮忙star一下，谢谢！

### 2 特性：
* 接入了spring boot
* 服务调用支持traceId功能
* 支持用java代码来定义和调用接口，无需用thrift接口定义文件来生成代码
* 客户端支持连接池功能，可以用注解来自动关闭释放连接资源，类似事务注解@Transactional
* 负载均衡支持客户端和代理端，包括：随机，轮询，最小连接数。以上算法都可以选择是否加权
* 集成tomcat-embed支持http，服务模式支持netty
* 传输协议集成protostuff，kryo
* 支持用注解的方式配置客户端，客户端代理类生成支持cglib和jdk原生两种方式
* 支持优雅关机功能
* 集成了MyBatis-Plus的BaseMapper的公共方法

### 3 使用方式：
服务端例子参考thriftjsoa-boot-server-test模块代码，代理端例子参考thriftjsoa-boot-proxy-test模块代码，客户端例子参考thriftjsoa-boot-client-test模块代码，
spring-boot-starter的maven依赖如下所示：
```xml
<dependency>
    <groupId>com.halloffame</groupId>
    <artifactId>thriftjsoa-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 3.1 定义接口
直接用java代码的方式来定义一个根据id查询用户信息的接口
```java
@Data
public class User {
    private int id;
    private String name;
}
```
```java
public abstract class UserService extends BaseService<User, User, BaseMapper<User>> {
    abstract User getUser(int id);
}
```

#### 3.2 服务端（包括客户端）实现
编写服务端spring boot工程的入口类，配置文件，业务实现类以及客户端调用类，启动`zookeeper`，然后启动spring boot工程，看到日志`Starting the server on port 9090...`代表server启动成功。

① 编写业务实现类和客户端调用类，实现`3.1`用java代码定义的抽象类`UserService`。
```java
//客户端调用类
@TjClient
public interface UserClient extends BaseClient<User, User> {
    User getUser(int id);
}
```
```java
//业务实现类
@Service
@Slf4j
public class UserServiceImpl extends UserService {
    @Autowired
    private UserClient userClient;

    @TjSession
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
        //log.info("" + userClient.getUser(2));
        return null;
    }
}
```

② 编写spring boot工程入口类
```java
@SpringBootApplication
@EnableTjClients
@EnableTjSessionManagement
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public List<ProcessorConfig> processorConfigs(UserService userService) {
        List<ProcessorConfig> processorConfigs = new ArrayList<>();
        processorConfigs.add(new ProcessorConfig().setTProcessor(new ThriftJsoaSessionProcessor<UserService>(new UserServiceImpl())));
        return processorConfigs;
    }
}
```

③ 编写spring boot工程配置文件
```yaml
thriftjsoa:
  server:
    threadedSelectorServerConfig: # 服务模式，默认ThreadedSelectorServerConfig
      port: 9090 # 服务端口，默认9090
      zkRegisterConfig: # 注册中心（zookeeper）连接配置
        zkConnStr: localhost:2181 # 连接串，默认localhost:2181
```

#### 3.3 代理端实现
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
      zkRegisterConfig: # 注册中心（zookeeper）连接配置，默认ZkRegisterConfig
        zkConnStr: localhost:2181 # 连接串，默认localhost:2181
      loadBalanceType: randomWeight # 负载均衡类型：leastConn, polling, random, leastConnWeight, pollingWeight, randomWeight(建议)，默认不指定
```
