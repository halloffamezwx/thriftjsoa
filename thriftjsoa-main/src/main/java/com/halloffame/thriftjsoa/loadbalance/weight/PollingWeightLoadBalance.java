package com.halloffame.thriftjsoa.loadbalance.weight;

import com.halloffame.thriftjsoa.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceUtil;
import com.halloffame.thriftjsoa.loadbalance.base.LoadBalanceWeightAbstract;

/**
 * 负载均衡：轮询（加权）
 * @author zhuwx
 */
public class PollingWeightLoadBalance extends LoadBalanceWeightAbstract {

    /**
     * 取得负载均衡结果
     */
    @Override
    public LoadBalanceBean getLoadBalanceBean() {
        return LoadBalanceUtil.getPollingLoadBalanceBean(getWeightConnectionFactorys());
    }

}
