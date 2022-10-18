package com.halloffame.thriftjsoa.core.session;

/**
 * 基础Client
 * @author zhuwx
 */
public interface BaseClient<T, E> extends Base<T, E> {

    default void close() {
    }

}
