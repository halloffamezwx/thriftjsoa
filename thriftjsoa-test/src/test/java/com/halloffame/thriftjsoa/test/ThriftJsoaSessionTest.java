package com.halloffame.thriftjsoa.test;

import com.halloffame.thriftjsoa.core.ThriftJsoaProxy;
import com.halloffame.thriftjsoa.core.ThriftJsoaServer;
import com.halloffame.thriftjsoa.core.config.BaseClientConfig;
import com.halloffame.thriftjsoa.core.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.core.config.client.ThriftJsoaClientConfig;
import com.halloffame.thriftjsoa.core.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.test.session.User;
import com.halloffame.thriftjsoa.test.session.UserClient;
import com.halloffame.thriftjsoa.test.session.UserService;
import com.halloffame.thriftjsoa.test.session.UserServiceImpl;
import com.halloffame.thriftjsoa.core.session.ThriftJsoaSession;
import com.halloffame.thriftjsoa.core.session.ThriftJsoaSessionFactory;
import com.halloffame.thriftjsoa.core.session.ThriftJsoaSessionProcessor;
import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

/**
 * 单元测试（session）
 * @author zhuwx
 */
public class ThriftJsoaSessionTest {

    private static final ThriftJsoaSessionFactory sessionFactory = initThriftJsoaSessionFactory();

    @SneakyThrows
    public static ThriftJsoaSessionFactory initThriftJsoaSessionFactory() {
        List<LoadBalanceClientConfig> loadBalanceClientConfigs = new ArrayList<>();
        LoadBalanceClientConfig loadBalanceClientConfig = new LoadBalanceClientConfig();

        BaseClientConfig clientConfig = new BaseClientConfig();
        clientConfig.setHost("localhost");
        clientConfig.setPort(4567);
        List<BaseClientConfig> clientConfigs = new ArrayList<>();
        clientConfigs.add(clientConfig);
        loadBalanceClientConfig.setClientConfigs(clientConfigs);

        List<ClientClassConfig> clazzs = new ArrayList<>();
        ClientClassConfig clazz = new ClientClassConfig();
        clazz.setSessionName(UserClient.class);
        clazz.setName(com.halloffame.thriftjsoa.test.UserService.Client.class);
        clazzs.add(clazz);
        loadBalanceClientConfig.setClazzs(clazzs);

        loadBalanceClientConfigs.add(loadBalanceClientConfig);
        ThriftJsoaClientConfig client = new ThriftJsoaClientConfig();
        client.setList(loadBalanceClientConfigs);

        return new ThriftJsoaSessionFactory(client);
    }

    /**
     * 客户端
     */
    //@Test
    public void testClient() throws Exception {
        try (ThriftJsoaSession session = sessionFactory.openSession()) {

            UserClient userClient = session.getClient(UserClient.class);
            User user = userClient.getUser(2);
            //session.close(UserClient.class);
            //userClient.close();

            System.out.println("名字：" + user.getName());
            System.out.println("traceId：" + ThriftJsoaUtil.getTraceId());
            System.out.println("appId：" + ThriftJsoaUtil.getAppId());

            com.halloffame.thriftjsoa.test.UserService.Client generateUserClient = session.createClient(
                    com.halloffame.thriftjsoa.test.UserService.Client.class);
            com.halloffame.thriftjsoa.test.User generateUser = generateUserClient.getUser(2); //getUser就是UserService.thrift所定义的接口
            //session.close(com.halloffame.thriftjsoa.test.UserService.Client.class);

            System.out.println("名字：" + generateUser.getName());
            System.out.println("traceId：" + ThriftJsoaUtil.getTraceId());
            System.out.println("appId：" + ThriftJsoaUtil.getAppId());
        }

    }

    /**
     * 代理端
     */
    //@Test
    public void testProxy() throws Exception {
        ThriftJsoaProxy thriftJsoaProxy = new ThriftJsoaProxy(4567, "localhost:2181");
        thriftJsoaProxy.run();
    }

    /**
     * 服务端
     */
    //@Test
    public void testServer() throws Exception {
        ThriftJsoaServer thriftJsoaServer = new ThriftJsoaServer(9090, "localhost:2181",
                new ThriftJsoaSessionProcessor<UserService>(new UserServiceImpl()));
        thriftJsoaServer.run();
    }

}
