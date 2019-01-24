package com.halloffame.thriftjsoa.config;

/**
 * 服务模式：单线程阻塞io
 */
public class SimpleServerConfig extends BaseServerConfig {
	private String serverType = "simple"; //单线程阻塞io

	public String getServerType() {
		return serverType;
	}
}
