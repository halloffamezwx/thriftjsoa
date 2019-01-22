package com.halloffame.thriftjsoa.config;

public class ClientConfig extends BaseConfig {
	private int socketTimeout = 1000;
	private String host;
	private int port;

	public ClientConfig(String host, int port) {
		this.host = host;
		this.port = port;
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
