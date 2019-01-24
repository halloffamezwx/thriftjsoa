package com.halloffame.thriftjsoa.loadbalance;

/**
 * 负载均衡：最小连接数（加权）
 */
public class WeightLeastConnLoadBalance extends LoadBalanceAbstract {

	@Override
	public LoadBalanceBean getLoadBalanceConnPool() {
		return LoadBalanceUtil.getLeastConnLoadBalanceConnPool(getPoolFactorys(), true);
	}

}
