package com.halloffame.thriftjsoa.loadbalance;

/**
 * 负载均衡：轮询
 */
public class PollingLoadBalance extends LoadBalanceAbstract {

	@Override
	public LoadBalanceBean getLoadBalanceConnPool() {
		return LoadBalanceUtil.getPollingLoadBalanceConnPool(getPoolFactorys());
	}

}
