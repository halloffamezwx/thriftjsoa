package com.halloffame.thriftjsoa.core.loadbalance.base;

import com.halloffame.thriftjsoa.core.base.ConnectionFactory;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 负载均衡抽象基类（加权）
 * @author zhuwx
 */
public abstract class LoadBalanceWeightAbstract extends LoadBalanceAbstract {

    /**
     * 加权的连接工厂list，权重是连接工厂的最大连接数
     */
    @Getter
    private List<ConnectionFactory> weightConnectionFactorys = new ArrayList<>();

    /**
     * 新增连接工厂
     */
    @Override
    public void addConnectionFactory(ConnectionFactory connectionFactory) {
        if (Objects.nonNull(connectionFactory)) {
            super.addConnectionFactory(connectionFactory);

            for (int i = 0; i < connectionFactory.getMaxTotal(); i++) {
                weightConnectionFactorys.add(connectionFactory);
            }
        }
    }

    /**
     * 移除连接工厂
     */
    @Override
    public void removeConnectionFactory(String path, String appId) {
        Iterator<ConnectionFactory> it = getConnectionFactorys().iterator();
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
    @Override
    public void removeConnectionFactory(ConnectionFactory connectionFactory, Iterator<ConnectionFactory> it) {
        super.removeConnectionFactory(connectionFactory, it);
        Iterator<ConnectionFactory> weightIt = weightConnectionFactorys.iterator();

        while (weightIt.hasNext()) {
            ConnectionFactory weightConnectionFactory = weightIt.next();

            if (connectionFactory.equals(weightConnectionFactory)) {
                weightConnectionFactory.destroy();
                weightIt.remove();
                weightConnectionFactory = null;
            }
        }
    }

}
