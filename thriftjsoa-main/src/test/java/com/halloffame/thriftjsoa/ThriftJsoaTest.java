package com.halloffame.thriftjsoa;

import com.halloffame.thriftjsoa.common.CommonClient;
import com.halloffame.thriftjsoa.sample.iface.User;
import com.halloffame.thriftjsoa.sample.iface.UserService;
import com.halloffame.thriftjsoa.sample.iface.UserServiceImpl;
import com.halloffame.thriftjsoa.util.ThriftJsoaUtil;

/**
 * 单元测试
 * @author zhuwx
 */
public class ThriftJsoaTest {

    /**
     * 客户端
     */
    //@Test
    public void testClient() throws Exception {
        UserService.Client userClient = CommonClient.createClient(UserService.Client.class, "localhost", 4567);
        User user = userClient.getUser(2); //getUser就是UserService.thrift所定义的接口
        userClient.getInputProtocol().getTransport().close();

        System.out.println("名字：" + user.getName());
        System.out.println("traceId：" + ThriftJsoaUtil.getTraceId());
        System.out.println("appId：" + ThriftJsoaUtil.getAppId());
    }

    /**
     * 代理
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
                new UserService.Processor(new UserServiceImpl()));
        thriftJsoaServer.run();
    }

}
