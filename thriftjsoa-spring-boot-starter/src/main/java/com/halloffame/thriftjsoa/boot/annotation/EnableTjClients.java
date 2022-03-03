package com.halloffame.thriftjsoa.boot.annotation;

import java.lang.annotation.*;

/**
 * 开启Tj客户端注解
 * @author zhuwx
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface EnableTjClients {

}
