package com.halloffame.thriftjsoa.sample.client.service.impl;

import com.halloffame.thriftjsoa.boot.annotation.OpenThriftjsoaSession;
import com.halloffame.thriftjsoa.sample.client.service.ClientTestService;
import com.halloffame.thriftjsoa.sample.iface.User;
import com.halloffame.thriftjsoa.sample.iface.UserService;
import com.halloffame.thriftjsoa.sample.iface.session.UserClient;
import com.halloffame.thriftjsoa.session.ThriftJsoaSessionData;
import com.halloffame.thriftjsoa.util.ThriftJsoaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 客户端测试
 */
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
