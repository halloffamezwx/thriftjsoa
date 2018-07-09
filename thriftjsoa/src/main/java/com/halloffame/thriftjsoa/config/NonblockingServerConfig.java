package com.halloffame.thriftjsoa.config;

public class NonblockingServerConfig extends BaseServerConfig {
	private String serverType = "nonblocking"; //单条线程非阻塞io

	public String getServerType() {
		return serverType;
	}
}
