package com.halloffame.thriftjsoa.config;

/**
 * 基础配置
 */
public class BaseConfig {
	private boolean ssl = false; //通信是否加密
	private String transportType = "fastframed"; //buffered, framed, fastframed, http
	private String protocolType = "compact"; //binary, json, compact
	
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
