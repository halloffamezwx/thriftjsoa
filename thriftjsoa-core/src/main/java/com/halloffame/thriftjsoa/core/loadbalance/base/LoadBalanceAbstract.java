package com.halloffame.thriftjsoa.core.loadbalance.base;

import com.halloffame.thriftjsoa.core.base.ConnectionFactory;
import com.halloffame.thriftjsoa.core.base.TjApplicationException;
import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceBean;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 负载均衡抽象基类
 * @author zhuwx
 */
public abstract class LoadBalanceAbstract {

    /**
     * 请求的服务名
     */
    @Setter
    private String appName;

    /**
     * 连接工厂list，每个服务对应一个
     */
    @Getter
    private List<ConnectionFactory> connectionFactorys = new ArrayList<>();

    /**
     * 新增连接工厂
     */
    public void addConnectionFactory(ConnectionFactory connectionFactory) {
        if (Objects.nonNull(connectionFactory)) {
            connectionFactorys.add(connectionFactory);
        }
    }

    /**
     * 移除连接工厂
     */
    public void removeConnectionFactory(String path, String appId) {
        Iterator<ConnectionFactory> it = connectionFactorys.iterator();
        while (it.hasNext()) {
            ConnectionFactory connectionFactory = it.next();
            if (connectionFactory.isSame(path, appId)) {
                this.removeConnectionFactory(connectionFactory, it);
            }
        }
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
     * 检查服务列表是否可用
     */
    public void check() {
        if (connectionFactorys == null || connectionFactorys.size() <= 0) {
            throw new TjApplicationException(TjApplicationException.NO_AVAILABLE_SERVICE, "[" + appName + "]没有可用服务");
        }
    }

    /**
     * 取得负载均衡结果，不同负载均衡算法不同的实现，没有负载均衡的话就取第一个
     */
    public LoadBalanceBean getLoadBalanceBean() {
        check();
        ConnectionFactory selectConnectionFactory = connectionFactorys.get(0);

        LoadBalanceBean loadBalanceBean = new LoadBalanceBean();
        loadBalanceBean.setConnectionFactory(selectConnectionFactory);
        loadBalanceBean.setProtocolWrap(selectConnectionFactory.getWrapConnection());

        return loadBalanceBean;
    }

}
