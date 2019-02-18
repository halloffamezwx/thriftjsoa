package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.common.CommonServer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * 服务端保存到ZK的配置
 */
public class ServerZkConfig {
	private GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig(); //proxy的连接池配置，默认GenericObjectPoolConfig
	private int socketTimeout = 3000; //proxy连接server的读超时时间，默认3000ms
	//proxy和server的transportType，protocolType，ssl，serverType等配置，默认ThreadedSelectorServerConfig
	private BaseServerConfig serverConfig = new ThreadedSelectorServerConfig();
	private String poolValidateMethodName = CommonServer.CONN_VALIDATE_METHOD_NAME; //proxy和server的连接池检查对象的有效性请求的不存在的接口名

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

	public String getPoolValidateMethodName() {
		return poolValidateMethodName;
	}

	public void setPoolValidateMethodName(String poolValidateMethodName) {
		this.poolValidateMethodName = poolValidateMethodName;
	}
}
