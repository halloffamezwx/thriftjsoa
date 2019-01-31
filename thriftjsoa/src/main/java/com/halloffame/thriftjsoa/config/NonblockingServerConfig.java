package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.common.ServerType;

/**
 * 服务模式：单条线程非阻塞io
 */
public class NonblockingServerConfig extends BaseServerConfig {
	@Override
	public String getServerType() {
		return ServerType.NONBLOCKING.getValue();
	}
}
