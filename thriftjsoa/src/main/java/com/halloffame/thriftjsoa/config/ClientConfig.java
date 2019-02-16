package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.List;

/**
 * 客户端配置
 */
public class ClientConfig extends BaseConfig {
	private int socketTimeout = 3000; //socket读超时时间，默认3000ms
	private String host; //服务主机
	private int port; //服务端口
	private List<ClientClassConfig> clientClassConfigs; //客户端的class list
	private GenericObjectPoolConfig poolConfig; //连接池配置
	private ConnectionPoolFactory poolFactory; //连接池

	public ClientConfig(String host, int port, List<ClientClassConfig> clientClassConfigs) {
		this.host = host;
		this.port = port;
		this.clientClassConfigs = clientClassConfigs;
	}

	public List<ClientClassConfig> getClientClassConfigs() {
		return clientClassConfigs;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
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
		poolFactory = new ConnectionPoolFactory(poolConfig, host, port,
				socketTimeout, isSsl(), getTransportType(), getProtocolType());
	}

	public ConnectionPoolFactory getPoolFactory() {
		return poolFactory;
	}
}
