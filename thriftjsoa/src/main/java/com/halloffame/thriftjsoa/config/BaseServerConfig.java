package com.halloffame.thriftjsoa.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseServerConfig {
	private boolean ssl = false; //通信是否加密
	private String transportType = "fastframed"; //buffered, framed, fastframed
	private String protocolType = "compact"; //binary, json, compact
	private String serverType = null; //threaded-selector, nonblocking, thread-pool, simple
	
	public String getServerType() {
		return serverType;
	}
	
	public boolean isSsl() {
		return ssl;
	}
	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}
	public String getTransportType() {
		return transportType;
	}
	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}
	public String getProtocolType() {
		return protocolType;
	}
	public void setProtocolType(String protocolType) {
		this.protocolType = protocolType;
	}

}
