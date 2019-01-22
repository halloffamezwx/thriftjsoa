package com.halloffame.thriftjsoa.config;

public class BaseServerConfig extends BaseConfig {
	private String serverType = null; //threaded-selector, nonblocking, thread-pool, simple
	
	public String getServerType() {
		return serverType;
	}

}
