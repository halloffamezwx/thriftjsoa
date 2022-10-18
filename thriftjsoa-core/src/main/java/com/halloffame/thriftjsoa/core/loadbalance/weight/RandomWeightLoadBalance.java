package com.halloffame.thriftjsoa.core.loadbalance.weight;

import com.halloffame.thriftjsoa.core.loadbalance.base.LoadBalanceWeightAbstract;
import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceUtil;

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
        check();
        return LoadBalanceUtil.getRandomLoadBalanceBean(getWeightConnectionFactorys());
    }

}
