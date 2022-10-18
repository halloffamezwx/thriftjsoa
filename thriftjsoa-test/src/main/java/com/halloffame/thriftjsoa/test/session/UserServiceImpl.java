package com.halloffame.thriftjsoa.test.session;

import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务端实现类（session）
 * @author zhuwx
 */
@Slf4j
public class UserServiceImpl extends UserService {

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
