package com.halloffame.thriftjsoa.loadbalance;

import org.apache.thrift.protocol.TProtocol;

import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;

public class LoadBalanceBean {
	private ConnectionPoolFactory connectionPoolFactory;
	private TProtocol protocol;
	
	public ConnectionPoolFactory getConnectionPoolFactory() {
		return connectionPoolFactory;
	}
	public void setConnectionPoolFactory(ConnectionPoolFactory connectionPoolFactory) {
		this.connectionPoolFactory = connectionPoolFactory;
	}
	public TProtocol getProtocol() {
		return protocol;
	}
	public void setProtocol(TProtocol protocol) {
		this.protocol = protocol;
	}
	
}
