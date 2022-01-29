package com.halloffame.thriftjsoa.boot.annotation;

import java.lang.annotation.*;

/**
 * Tj客户端扫描注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface TjClientScan {

    /**
     * 包路径数组
     */
    String[] value() default {};
}
