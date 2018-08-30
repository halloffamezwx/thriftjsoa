package com.halloffame.thriftjsoa.loadbalance;

//负载均衡：随机
public class RandomLoadBalance extends LoadBalanceAbstract {

	@Override
	public LoadBalanceBean getLoadBalanceConnPool() {
		return LoadBalanceUtil.getRandomLoadBalanceConnPool(getPoolFactorys());
	}

}
