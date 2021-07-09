package com.halloffame.thriftjsoa.loadbalance.unweight;

import com.halloffame.thriftjsoa.loadbalance.base.LoadBalanceAbstract;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceUtil;

/**
 * 负载均衡：轮询
 * @author zhuwx
 */
public class PollingLoadBalance extends LoadBalanceAbstract {

    /**
     * 取得负载均衡结果
     */
    @Override
    public LoadBalanceBean getLoadBalanceBean() {
        return LoadBalanceUtil.getPollingLoadBalanceBean(getConnectionFactorys());
    }

}
