package com.halloffame.thriftjsoa.config;

import org.apache.thrift.TServiceClient;

/**
 * 客户端配置
 */
public class ClientConfig extends BaseConfig {
	private int socketTimeout = 3000; //socket读超时时间，默认3000ms
	private String host; //服务主机
	private int port; //服务端口
	private Class<? extends TServiceClient> clazz; //客户端的class

	public ClientConfig(String host, int port, Class<? extends TServiceClient> clazz) {
		this.host = host;
		this.port = port;
		this.clazz = clazz;
	}

	public Class<? extends TServiceClient> getClazz() {
		return clazz;
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
}
