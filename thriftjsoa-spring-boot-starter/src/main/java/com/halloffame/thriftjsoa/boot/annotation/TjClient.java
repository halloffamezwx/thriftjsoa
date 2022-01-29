package com.halloffame.thriftjsoa.boot.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Tj客户端注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TjClient {

    /**
     *
     */
    @AliasFor("name")
    String value() default "";

    /**
     *
     */
    @AliasFor("value")
    String name() default "";

}
