package com.halloffame.thriftjsoa.server;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class ServerConfig {
	private GenericObjectPoolConfig genericObjectPoolConfig;
	private int socketTimeout;

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public GenericObjectPoolConfig getGenericObjectPoolConfig() {
		return genericObjectPoolConfig;
	}

	public void setGenericObjectPoolConfig(GenericObjectPoolConfig genericObjectPoolConfig) {
		this.genericObjectPoolConfig = genericObjectPoolConfig;
	}
	
}
