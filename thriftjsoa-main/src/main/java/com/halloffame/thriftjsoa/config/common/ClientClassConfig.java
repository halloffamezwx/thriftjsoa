package com.halloffame.thriftjsoa.config.common;

import lombok.Data;
import org.apache.thrift.TServiceClient;

/**
 * 客户端class配置
 * @author zhuwx
 */
@Data
public class ClientClassConfig {

    /**
     * 客户端class（根据接口定义文件生成的client）
     */
    private Class<? extends TServiceClient> name;

    /**
     * 客户端class（session）
     */
    private Class<?> sessionName;

    /**
     * 自定义名称，用于TMultiplexedProtocol的SERVICE_NAME
     */
    private String serviceName;

}
