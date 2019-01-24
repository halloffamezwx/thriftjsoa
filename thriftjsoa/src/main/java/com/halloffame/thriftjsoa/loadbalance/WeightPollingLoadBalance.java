package com.halloffame.thriftjsoa.loadbalance;

/**
 * 负载均衡：轮询（加权）
 */
public class WeightPollingLoadBalance extends WeightLoadBalanceAbstract {

	@Override
	public LoadBalanceBean getLoadBalanceConnPool() {
		return LoadBalanceUtil.getPollingLoadBalanceConnPool(getWeightPoolFactorys());
	}

}
