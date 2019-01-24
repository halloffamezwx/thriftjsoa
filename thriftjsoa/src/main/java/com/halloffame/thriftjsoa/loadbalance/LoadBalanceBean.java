package com.halloffame.thriftjsoa.loadbalance;

import org.apache.thrift.protocol.TProtocol;

import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;

/**
 * 负载均衡的返回结果
 */
public class LoadBalanceBean {
	private ConnectionPoolFactory connectionPoolFactory; //选择的连接池
	private TProtocol protocol; //选择的连接池里的TProtocol
	
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
