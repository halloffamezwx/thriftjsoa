package com.halloffame.thriftjsoa.core.loadbalance.unweight;

import com.halloffame.thriftjsoa.core.loadbalance.base.LoadBalanceAbstract;
import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceUtil;

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
        check();
        return LoadBalanceUtil.getPollingLoadBalanceBean(getConnectionFactorys());
    }

}
