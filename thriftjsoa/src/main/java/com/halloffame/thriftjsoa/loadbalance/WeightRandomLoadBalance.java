package com.halloffame.thriftjsoa.loadbalance;

//负载均衡：随机（加权）
public class WeightRandomLoadBalance extends WeightLoadBalanceAbstract {
	
	@Override
	public LoadBalanceBean getLoadBalanceConnPool() {
		return LoadBalanceUtil.getRandomLoadBalanceConnPool(getWeightPoolFactorys());
	}

}
