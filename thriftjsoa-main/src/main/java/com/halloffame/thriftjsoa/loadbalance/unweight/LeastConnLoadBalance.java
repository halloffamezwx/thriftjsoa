package com.halloffame.thriftjsoa.loadbalance.unweight;

import com.halloffame.thriftjsoa.loadbalance.LoadBalanceBean;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceUtil;
import com.halloffame.thriftjsoa.loadbalance.base.LoadBalanceAbstract;

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
        return LoadBalanceUtil.getLeastConnLoadBalanceBean(getConnectionFactorys(), false);
    }

}
