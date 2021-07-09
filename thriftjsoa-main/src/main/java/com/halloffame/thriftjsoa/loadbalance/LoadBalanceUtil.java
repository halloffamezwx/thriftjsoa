package com.halloffame.thriftjsoa.loadbalance;

import com.halloffame.thriftjsoa.base.ConnectionFactory;
import com.halloffame.thriftjsoa.base.TProtocolWrap;
import com.halloffame.thriftjsoa.util.ThriftJsoaAtomicInteger;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 负载均衡算法工具
 * @author zhuwx
 */
@Slf4j
public class LoadBalanceUtil {

    /**
     * 最小连接数计算时的锁
     */
    private final static ReentrantLock reentrantLock = new ReentrantLock();

    /**
     * 轮询的原子计数器
     */
    private final static ThriftJsoaAtomicInteger thriftJsoaAtomicInteger = new ThriftJsoaAtomicInteger(-1);

    /**
     * 随机
     */
    public static LoadBalanceBean getRandomLoadBalanceBean(List<ConnectionFactory> connectionFactorys) {

        Random random = new Random();
        int selNum = random.nextInt(connectionFactorys.size());

        ConnectionFactory selectConnectionFactory = connectionFactorys.get(selNum);
        TProtocolWrap tProtocolWrap = selectConnectionFactory.getWrapConnection();

        log.debug("LoadBalanceUtil getRandomLoadBalanceBean selectConnectionFactory={}", selectConnectionFactory);

        LoadBalanceBean loadBalanceBean = new LoadBalanceBean();
        loadBalanceBean.setConnectionFactory(selectConnectionFactory);
        loadBalanceBean.setProtocolWrap(tProtocolWrap);

        return loadBalanceBean;
    }

    /**
     * 轮询
     */
    public static LoadBalanceBean getPollingLoadBalanceBean(List<ConnectionFactory> connectionFactorys) {

        ConnectionFactory selectConnectionFactory = connectionFactorys.get(thriftJsoaAtomicInteger.incrementAndGet(connectionFactorys.size()));
        TProtocolWrap tProtocolWrap = selectConnectionFactory.getWrapConnection();

        log.debug("LoadBalanceUtil getPollingLoadBalanceBean selectConnectionFactory={}", selectConnectionFactory);

        LoadBalanceBean loadBalanceBean = new LoadBalanceBean();
        loadBalanceBean.setConnectionFactory(selectConnectionFactory);
        loadBalanceBean.setProtocolWrap(tProtocolWrap);

        return loadBalanceBean;
    }

    /**
     * 最小连接数
     */
    public static LoadBalanceBean getLeastConnLoadBalanceBean(List<ConnectionFactory> connectionFactorys, boolean isWeight) {

        ConnectionFactory selectConnectionFactory = connectionFactorys.get(0);
        TProtocolWrap tProtocolWrap;

        try {
            /**
             * 如果不加锁会出现一种极端情况：并发大批量请求过来的时候，所有请求算出的connectionValue值一样，将会指向到同一个服务上
             * 因为connectionValue值是根据连接工厂的numActive和maxTotal算出来的，而连接工厂调用getConnection后，numActive会改变（累加1）
             * 调用releaseConnection后，numActive也会改变（累减1）
             */
            reentrantLock.lock();

            double selectConnectionValue = 0;

            if (isWeight) { //是否加权
                selectConnectionValue = selectConnectionFactory.getWeight();
            } else {
                selectConnectionValue = selectConnectionFactory.getNumActive();
            }
            log.debug("LoadBalanceUtil getLeastConnLoadBalanceBean connectionFactorys={}", connectionFactorys);

            for (int i = 1; i < connectionFactorys.size(); i++) {

                ConnectionFactory connectionFactory = connectionFactorys.get(i);
                double connectionValue = 0;

                if (isWeight) {
                    connectionValue = connectionFactory.getWeight();
                } else {
                    connectionValue = connectionFactory.getNumActive();
                }
                log.debug("LoadBalanceUtil getLeastConnLoadBalanceBean selectConnectionValue={}, connectionValue={}",
                        selectConnectionValue, connectionValue);

                //todo 这里为了简单，没有实现：如果有多个算出的值同为最小，那么对它们采用加权轮询算法
                if ( connectionValue < selectConnectionValue ) {
                    selectConnectionFactory = connectionFactory;
                    selectConnectionValue = connectionValue;
                }
            }

            tProtocolWrap = selectConnectionFactory.getWrapConnection();
        } finally {
            if (reentrantLock.isLocked()) {
                reentrantLock.unlock();
            }
        }
        log.debug("LoadBalanceUtil getLeastConnLoadBalanceBean selectConnectionFactory={}", selectConnectionFactory);

        LoadBalanceBean loadBalanceBean = new LoadBalanceBean();
        loadBalanceBean.setConnectionFactory(selectConnectionFactory);
        loadBalanceBean.setProtocolWrap(tProtocolWrap);

        return loadBalanceBean;
    }

}
