package com.halloffame.thriftjsoa.sample.server;

import com.halloffame.thriftjsoa.sample.iface.User;
import com.halloffame.thriftjsoa.sample.iface.UserService;
import com.halloffame.thriftjsoa.util.ThriftJsoaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 业务逻辑实现类
 * @author zhuwx
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService.Iface {

    /**
     * 实现UserService.thrift里的getUser接口
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
