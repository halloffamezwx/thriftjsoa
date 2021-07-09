package com.halloffame.thriftjsoa.common;

import com.halloffame.thriftjsoa.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.loadbalance.base.LoadBalanceAbstract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.zookeeper.ZooKeeper;

/**
 * 创建负载均衡对象返回结果
 * @author zhuwx
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateLoadBalanceResult {

    /**
     * 注册中心（zookeeper对象），没有有配置的话，则为null
     */
    private ZooKeeper zk;

    /**
     * 负载均衡对象
     */
    private LoadBalanceAbstract loadBalance;

    /**
     * 对应的配置
     */
    private LoadBalanceClientConfig loadBalanceClientConfig;
}
