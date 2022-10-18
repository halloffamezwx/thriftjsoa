package com.halloffame.thriftjsoa.test.session;

import com.halloffame.thriftjsoa.core.session.BaseClient;

/**
 * 用户客户端（session）
 * @author zhuwx
 */
public interface UserClient extends BaseClient<User, User> {

    /**
     * 获取用户
     */
    User getUser(int id);
}
