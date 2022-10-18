package com.halloffame.thriftjsoa.core.config.client;

import com.halloffame.thriftjsoa.core.config.BaseClientConfig;
import com.halloffame.thriftjsoa.core.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.core.config.register.ZkRegisterConfig;
import com.halloffame.thriftjsoa.core.constant.LoadBalanceType;
import lombok.Data;
import org.apache.curator.framework.CuratorFramework;

import java.util.List;

/**
 * 客户端配置（负载均衡）
 * @author zhuwx
 */
@Data
public class LoadBalanceClientConfig {

    /**
     * 请求的服务名
     */
    private String appName;

    /**
     * 注册中心（zookeeper）
     */
    //private ZooKeeper zk;

    /**
     * 注册中心（zookeeper）- CuratorFramework
     */
    private CuratorFramework zkCf;

    /**
     * 注册中心配置
     */
    private ZkRegisterConfig zkRegisterConfig = new ZkRegisterConfig();

    /**
     * 忽略注册中心配置，需要提供连接的配置数据列表，负载均衡算法从这个列表选取一个
     */
    private List<BaseClientConfig> clientConfigs;

    /**
     * 客户端的class配置列表
     */
    private List<ClientClassConfig> clazzs;

    /**
     * 负载均衡类型，建议使用：RANDOM_WEIGHT-随机（加权），clientConfigs只有一个的时候无需指定，默认不指定
     * {@link LoadBalanceType#getCode()}
     * @see LoadBalanceType#getCode()
     */
    private String loadBalanceType;

    /**
     * 是否在ThriftJsoa服务里发起连接请求，涉及traceId的消息头协议解析处理等
     */
    private boolean inTjServer = true;
}
