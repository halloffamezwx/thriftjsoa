package com.halloffame.thriftjsoa.server;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class ServerConfig {
	private GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
	private int socketTimeout = 3000;
	
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
