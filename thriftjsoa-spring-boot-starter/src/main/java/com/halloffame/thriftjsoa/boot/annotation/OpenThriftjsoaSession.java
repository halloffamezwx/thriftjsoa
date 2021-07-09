package com.halloffame.thriftjsoa.boot.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 打开Thriftjsoa会话（自动创建和回收资源）
 * @author zhuwx
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface OpenThriftjsoaSession {

    /**
     * 根据客户端class自动关闭连接资源等
     */
    Class<?>[] autoCloseClassArr() default {};

    /**
     * 根据客户端class手动关闭连接资源等
     */
    Class<?>[] manualCloseClassArr() default {};

    /**
     * 对未指定的客户端class是否自动关闭连接资源等
     */
    @AliasFor("value")
    boolean autoClose() default true;

    @AliasFor("autoClose")
    boolean value() default true;

}
