package com.halloffame.thriftjsoa.loadbalance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;

/**
 * 负载均衡抽象基类
 */
public abstract class LoadBalanceAbstract {
	//连接池list，每个服务对应一个连接池
	private List<ConnectionPoolFactory> poolFactorys = new ArrayList<>();
	
	public List<ConnectionPoolFactory> getPoolFactorys() {
		return poolFactorys;
	}
	
	public void addPoolFactory(ConnectionPoolFactory poolFactory) {
		poolFactorys.add(poolFactory);
	}
	
	public void removePoolFactory(ConnectionPoolFactory poolFactory, Iterator<ConnectionPoolFactory> it) {
		poolFactory.close();
		it.remove();
		poolFactory = null;
	}

	/**
	 * 取得负载均衡结果，不同负载均衡算法不同的实现
	 */
	public abstract LoadBalanceBean getLoadBalanceConnPool();
}
