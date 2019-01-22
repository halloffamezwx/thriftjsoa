package com.halloffame.thriftjsoa.config;

import org.apache.thrift.TServiceClient;

import java.util.Map;

public class ClientCollectConfig {
	private Map<Class<? extends TServiceClient>, ClientConfig> clientConfigMap;

	public ClientCollectConfig(Map<Class<? extends TServiceClient>, ClientConfig> clientConfigMap) {
		this.clientConfigMap = clientConfigMap;
	}

	public ClientConfig getClientConfig(Class<? extends TServiceClient> clazz) {
		return clientConfigMap.get(clazz);
	}
}
