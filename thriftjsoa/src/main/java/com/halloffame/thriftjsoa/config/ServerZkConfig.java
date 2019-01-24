package com.halloffame.thriftjsoa.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * 服务端保存到ZK的配置
 */
public class ServerZkConfig {
	private GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig(); //proxy的连接池配置
	private int socketTimeout = 3000; //proxy连接server的读超时时间
	//proxy和server的transportType，protocolType，ssl，serverType等配置
	private BaseServerConfig serverConfig = new ThreadedSelectorServerConfig(); 

	public BaseServerConfig getServerConfig() {
		return serverConfig;
	}

	public void setServerConfig(BaseServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public GenericObjectPoolConfig getPoolConfig() {
		return poolConfig;
	}

	public void setPoolConfig(GenericObjectPoolConfig poolConfig) {
		this.poolConfig = poolConfig;
	}
	
}
