package com.halloffame.thriftjsoa.config.client;

import com.halloffame.thriftjsoa.config.BaseClientConfig;
import com.halloffame.thriftjsoa.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.config.common.ZkConnConfig;
import com.halloffame.thriftjsoa.constant.LoadBalanceType;
import lombok.Data;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

/**
 * 客户端配置（负载均衡）
 * @author zhuwx
 */
@Data
public class LoadBalanceClientConfig {

    /**
     * 名称，当注册中心是zookeeper，一般为请求服务注册的节点的父路径
     */
    private String name;

    /**
     * 注册中心（zookeeper）
     */
    private ZooKeeper zk;

    /**
     * 注册中心（zookeeper）连接配置
     */
    private ZkConnConfig zkConnConfig = new ZkConnConfig();

    /**
     * 忽略注册中心（zookeeper）连接配置，需要提供连接的配置数据列表，负载均衡算法从这个列表选取一个
     */
    private List<BaseClientConfig> clientConfigs;

    /**
     * 客户端的class配置列表
     */
    private List<ClientClassConfig> clazzs;

    /**
     * 负载均衡类型，建议使用：RANDOM_WEIGHT-随机（加权），clientConfigs只有一个的时候无需指定，默认不指定
     * {@link LoadBalanceType#getCode()}
     */
    private String loadBalanceType;

    /**
     * 是否在ThriftJsoa服务里发起连接请求，涉及traceId的消息头协议解析处理等
     */
    private boolean inTjServer = false;
}
