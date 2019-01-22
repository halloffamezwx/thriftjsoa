package com.halloffame.thriftjsoa.annotation;

import org.apache.thrift.TServiceClient;

import java.lang.annotation.*;

/**
 * 注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ClientAnnotation {

    /**
     *
     */
    Class<? extends TServiceClient>[] clientClassArr() default {};

}
