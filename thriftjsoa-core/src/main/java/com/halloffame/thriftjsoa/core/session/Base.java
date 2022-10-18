package com.halloffame.thriftjsoa.core.session;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * BaseClient或BaseService的基础接口
 * @author zhuwx
 */
public interface Base<T, E> extends BaseMapper<E> {

    T selectById(Long id);
}
