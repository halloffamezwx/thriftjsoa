package com.halloffame.thriftjsoa.test.session;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.halloffame.thriftjsoa.core.session.BaseService;

/**
 * 用户服务端（session）
 * @author zhuwx
 */
public abstract class UserService extends BaseService<User, User, BaseMapper<User>> {

    /**
     * 获取用户
     */
    abstract User getUser(int id);
}
