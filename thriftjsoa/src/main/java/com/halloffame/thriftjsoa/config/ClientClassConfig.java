package com.halloffame.thriftjsoa.config;

import org.apache.thrift.TServiceClient;

/**
 * 客户端class配置
 */
public class ClientClassConfig {
    private String name; //自定义名称，用于TMultiplexedProtocol的SERVICE_NAME
    private Class<? extends TServiceClient> clazz; //客户端class

    public ClientClassConfig(Class<? extends TServiceClient> clazz) {
        this.clazz = clazz;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends TServiceClient> getClazz() {
        return clazz;
    }

}
