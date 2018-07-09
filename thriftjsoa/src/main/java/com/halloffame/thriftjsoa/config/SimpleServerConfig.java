package com.halloffame.thriftjsoa.config;

public class SimpleServerConfig extends BaseServerConfig {
	private String serverType = "simple"; //单线程阻塞io

	public String getServerType() {
		return serverType;
	}
}
