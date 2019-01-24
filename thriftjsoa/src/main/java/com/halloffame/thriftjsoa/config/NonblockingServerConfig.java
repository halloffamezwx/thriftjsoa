package com.halloffame.thriftjsoa.config;

/**
 * 服务模式：单条线程非阻塞io
 */
public class NonblockingServerConfig extends BaseServerConfig {
	private String serverType = "nonblocking"; //单条线程非阻塞io

	public String getServerType() {
		return serverType;
	}
}
