package com.halloffame.thriftjsoa.loadbalance;

//负载均衡：最小连接数
public class LeastConnLoadBalance extends LoadBalanceAbstract {

	@Override
	public LoadBalanceBean getLoadBalanceConnPool() {
		return LoadBalanceUtil.getLeastConnLoadBalanceConnPool(getPoolFactorys(), false);
	}

}
