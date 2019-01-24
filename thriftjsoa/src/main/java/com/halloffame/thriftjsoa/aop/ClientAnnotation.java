package com.halloffame.thriftjsoa.aop;

import org.apache.thrift.TServiceClient;

import java.lang.annotation.*;

/**
 * 客户端开启注解（自动创建和回收资源）
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ClientAnnotation {

    /**
     * 要开启的客户端的class数组
     */
    Class<? extends TServiceClient>[] clientClassArr() default {};

}
