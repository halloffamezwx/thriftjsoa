package com.halloffame.thriftjsoa.boot.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Tj客户端注解
 * @author zhuwx
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TjClient {

    /**
     * 请求的Tj服务端名
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 请求的Tj服务端名
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 用于TMultiplexedProtocol的SERVICE_NAME的自定义名称
     */
    String multipleServiceName();

}
