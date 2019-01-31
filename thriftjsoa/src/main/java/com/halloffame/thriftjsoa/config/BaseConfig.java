package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.common.ProtocolType;
import com.halloffame.thriftjsoa.common.TransportType;

/**
 * 基础配置
 */
public class BaseConfig {
	private boolean ssl = false; //通信是否加密，默认false
	private String transportType = TransportType.FASTFRAMED.getValue(); //buffered, framed, fastframed(default), http
	private String protocolType = ProtocolType.COMPACT.getValue(); //binary, json, compact(default)

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
