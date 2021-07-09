package com.halloffame.thriftjsoa.sample.iface.session;

import com.halloffame.thriftjsoa.session.BaseClient;

/**
 * 用户客户端（session）
 * @author zhuwx
 */
public interface UserClient extends BaseClient<User> {

    /**
     * 获取用户
     */
    User getUser(int id);
}
