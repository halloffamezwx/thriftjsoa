package com.halloffame.thriftjsoa.boot.server.test;

import com.halloffame.thriftjsoa.boot.annotation.TjClient;
import com.halloffame.thriftjsoa.core.session.BaseClient;
import com.halloffame.thriftjsoa.test.session.User;

/**
 * 用户注解客户端（session）
 * @author zhuwx
 */
@TjClient("/thriftJsoaServer")
public interface UserClient extends BaseClient<User, User> {

    /**
     * 获取用户
     */
    User getUser(int id);
}
