package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.common.ServerType;

/**
 * 服务模式：单线程阻塞io
 */
public class SimpleServerConfig extends BaseServerConfig {
	@Override
	public String getServerType() {
		return ServerType.SIMPLE.getValue();
	}
}
