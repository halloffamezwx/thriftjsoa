package com.halloffame.thriftjsoa.boot.annotation;

import com.halloffame.thriftjsoa.common.CommonServer;
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
     * 请求的Tj服务端在注册中心（zooKeeper）的根路径
     */
    @AliasFor("zkRootPath")
    String value() default CommonServer.ZK_ROOT_PATH;

    /**
     * 请求的Tj服务端在注册中心（zooKeeper）的根路径
     */
    @AliasFor("value")
    String zkRootPath() default CommonServer.ZK_ROOT_PATH;

    /**
     * 用于TMultiplexedProtocol的SERVICE_NAME的自定义名称
     */
    String multipleServiceName();

}
