package com.halloffame.thriftjsoa.boot.server.test;

import com.halloffame.thriftjsoa.boot.annotation.TjSession;
import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import com.halloffame.thriftjsoa.test.User;
import com.halloffame.thriftjsoa.test.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户业务逻辑实现类
 * @author zhuwx
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService.Iface {

    @Autowired
    private UserClient userClient;

    /**
     * 实现UserService.thrift里的getUser接口
     */
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
