package com.halloffame.thriftjsoa.config;

/**
 * 基础配置（服务端）
 */
public class BaseServerConfig extends BaseConfig {
	private String serverType = null; //threaded-selector, nonblocking, thread-pool, simple
	
	public String getServerType() {
		return serverType;
	}

}
