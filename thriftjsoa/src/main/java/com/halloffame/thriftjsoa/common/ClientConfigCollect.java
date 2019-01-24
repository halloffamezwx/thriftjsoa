package com.halloffame.thriftjsoa.common;

import com.halloffame.thriftjsoa.config.ClientConfig;
import org.apache.thrift.TServiceClient;

import java.util.Map;

/**
 * 客户端配置集合
 */
public class ClientConfigCollect {
    private Map<Class<? extends TServiceClient>, ClientConfig> clientConfigClassMap;

    public Map<Class<? extends TServiceClient>, ClientConfig> getClientConfigClassMap() {
        return clientConfigClassMap;
    }

    public void setClientConfigClassMap(Map<Class<? extends TServiceClient>, ClientConfig> clientConfigClassMap) {
        this.clientConfigClassMap = clientConfigClassMap;
    }
}
