package com.halloffame.thriftjsoa.sample.iface.session;

import com.halloffame.thriftjsoa.session.BaseService;

/**
 * 用户服务端（session）
 * @author zhuwx
 */
public abstract class UserService extends BaseService<User> {

    /**
     * 获取用户
     */
    public abstract User getUser(int id);
}
