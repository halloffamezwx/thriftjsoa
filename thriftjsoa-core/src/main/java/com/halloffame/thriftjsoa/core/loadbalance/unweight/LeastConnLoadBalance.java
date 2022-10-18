package com.halloffame.thriftjsoa.core.loadbalance.unweight;

import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceUtil;
import com.halloffame.thriftjsoa.core.loadbalance.base.LoadBalanceAbstract;

/**
 * 负载均衡：最小连接数
 * @author zhuwx
 */
public class LeastConnLoadBalance extends LoadBalanceAbstract {

    /**
     * 取得负载均衡结果
     */
    @Override
    public LoadBalanceBean getLoadBalanceBean() {
        check();
        return LoadBalanceUtil.getLeastConnLoadBalanceBean(getConnectionFactorys(), false);
    }

}
