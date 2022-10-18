package com.halloffame.thriftjsoa.boot.client.test.service.impl;

import com.halloffame.thriftjsoa.boot.annotation.TjSession;
import com.halloffame.thriftjsoa.boot.client.test.service.ClientTestService;
import com.halloffame.thriftjsoa.core.session.ThriftJsoaSessionData;
import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import com.halloffame.thriftjsoa.test.UserService;
import com.halloffame.thriftjsoa.test.session.User;
import com.halloffame.thriftjsoa.test.session.UserClient;
import lombok.SneakyThrows;
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
    @TjSession
    @Override
    @SneakyThrows
    public void clientTest() {
        UserService.Client generateUserClient = ThriftJsoaSessionData.SESSION_TL.get().createClient(UserService.Client.class);
        com.halloffame.thriftjsoa.test.User generateUser = generateUserClient.getUser(2); //getUser就是UserService.thrift所定义的接口
        //ThriftJsoaSessionData.SESSION_TL.get().close(UserService.Client.class);

        log.info("名字：{}", generateUser.getName());
        log.info("traceId：{}", ThriftJsoaUtil.getTraceId());
        log.info("appId：{}", ThriftJsoaUtil.getAppId());

        User user = userClient.getUser(2);
        //ThriftJsoaSessionData.SESSION_TL.get().close(UserClient.class);
        //userClient.close();

        log.info("名字：{}", user.getName());
        log.info("traceId：{}", ThriftJsoaUtil.getTraceId());
        log.info("appId：{}", ThriftJsoaUtil.getAppId());
    }

}
