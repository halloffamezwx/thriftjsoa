package com.halloffame.thriftjsoa.core.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TjLog {

    /**
     * 客户端打印入参日志
     */
    boolean clientIn() default true;

    /**
     * 客户端打印出参日志
     */
    boolean clientOut() default true;

    /**
     * 服务端打印入参日志
     */
    boolean serverIn() default true;

    /**
     * 服务端打印出参日志
     */
    boolean serverOut() default true;
}
