package com.halloffame.thriftjsoa.core.common;

import com.halloffame.thriftjsoa.core.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.core.loadbalance.base.LoadBalanceAbstract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.curator.framework.CuratorFramework;

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
     * 注册中心（zookeeper），没有有配置的话，则为null
     */
    private CuratorFramework zkCf;

    /**
     * 负载均衡对象
     */
    private LoadBalanceAbstract loadBalance;

    /**
     * 对应的配置
     */
    private LoadBalanceClientConfig loadBalanceClientConfig;
}
