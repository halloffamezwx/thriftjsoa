package com.halloffame.thriftjsoa.loadbalance.base;

import com.halloffame.thriftjsoa.base.ConnectionFactory;
import com.halloffame.thriftjsoa.loadbalance.LoadBalanceBean;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 负载均衡抽象基类
 * @author zhuwx
 */
public abstract class LoadBalanceAbstract {

    /**
     * 连接工厂list，每个服务对应一个
     */
    @Getter
    private List<ConnectionFactory> connectionFactorys = new ArrayList<>();

    /**
     * 新增连接工厂
     */
    public void addConnectionFactory(ConnectionFactory connectionFactory) {
        connectionFactorys.add(connectionFactory);
    }

    /**
     * 移除连接工厂
     */
    public void removeConnectionFactory(ConnectionFactory connectionFactory, Iterator<ConnectionFactory> it) {
        connectionFactory.destroy();
        it.remove();
        connectionFactory = null;
    }

    /**
     * 取得负载均衡结果，不同负载均衡算法不同的实现，没有负载均衡的话就取第一个
     */
    public LoadBalanceBean getLoadBalanceBean() {
        ConnectionFactory selectConnectionFactory = connectionFactorys.get(0);

        LoadBalanceBean loadBalanceBean = new LoadBalanceBean();
        loadBalanceBean.setConnectionFactory(selectConnectionFactory);
        loadBalanceBean.setProtocolWrap(selectConnectionFactory.getWrapConnection());

        return loadBalanceBean;
    }

}
