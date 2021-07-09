package com.halloffame.thriftjsoa.loadbalance.weight;

import com.halloffame.thriftjsoa.loadbalance.base.LoadBalanceWeightAbstract;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceUtil;

/**
 * 负载均衡：随机（加权）
 * @author zhuwx
 */
public class RandomWeightLoadBalance extends LoadBalanceWeightAbstract {

    /**
     * 取得负载均衡结果
     */
    @Override
    public LoadBalanceBean getLoadBalanceBean() {
        return LoadBalanceUtil.getRandomLoadBalanceBean(getWeightConnectionFactorys());
    }

}
