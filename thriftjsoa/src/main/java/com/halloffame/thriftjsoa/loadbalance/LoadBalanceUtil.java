package com.halloffame.thriftjsoa.loadbalance;

import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;
import com.halloffame.thriftjsoa.util.MyAtomicInteger;

/**
 * 负载均衡算法工具
 */
public class LoadBalanceUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalanceUtil.class.getName());
	private final static ReentrantLock reentrantLock = new ReentrantLock(); //最小连接数计算时的锁
	private final static MyAtomicInteger myAtomicInteger = new MyAtomicInteger(); //轮询的原子计数器

	/**
	 * 随机
	 */
	public static LoadBalanceBean getRandomLoadBalanceConnPool(List<ConnectionPoolFactory> poolFactorys) {
		Random random = new Random();
		int selNum = random.nextInt(poolFactorys.size());
		
		ConnectionPoolFactory selectPoolFactory = poolFactorys.get(selNum);
		TProtocol tProtocol = selectPoolFactory.getConnection(); 
		
		LOGGER.info("selectPoolFactory={}", selectPoolFactory);
		
		LoadBalanceBean loadBalanceBean = new LoadBalanceBean();
		loadBalanceBean.setConnectionPoolFactory(selectPoolFactory);
		loadBalanceBean.setProtocol(tProtocol);
		
		return loadBalanceBean;
	}

	/**
	 * 轮询
	 */
	public static LoadBalanceBean getPollingLoadBalanceConnPool(List<ConnectionPoolFactory> poolFactorys) {
		ConnectionPoolFactory selectPoolFactory = poolFactorys.get(myAtomicInteger.get());
		myAtomicInteger.incrementAndGet(poolFactorys.size());
		TProtocol tProtocol = selectPoolFactory.getConnection(); 
		
		LOGGER.info("selectPoolFactory={}", selectPoolFactory);
		
		LoadBalanceBean loadBalanceBean = new LoadBalanceBean();
		loadBalanceBean.setConnectionPoolFactory(selectPoolFactory);
		loadBalanceBean.setProtocol(tProtocol);
		
		return loadBalanceBean;
	}

	/**
	 * 最小连接数
	 */
	public static LoadBalanceBean getLeastConnLoadBalanceConnPool(List<ConnectionPoolFactory> poolFactorys, boolean isWeight) {
		ConnectionPoolFactory selectPoolFactory = poolFactorys.get(0);
		TProtocol tProtocol = null;
		
		reentrantLock.lock();
		try {
			double selectPoolValue = 0;
			if (isWeight) {
				selectPoolValue = selectPoolFactory.getWeight();
			} else {
				selectPoolValue = selectPoolFactory.getNumActive();
			}
			LOGGER.info("poolFactorys={}", poolFactorys); 
			
			for (int i = 1; i < poolFactorys.size(); i++) {
				ConnectionPoolFactory poolFactory = poolFactorys.get(i);
				double poolValue = 0;
				if (isWeight) {
					poolValue = poolFactory.getWeight();
				} else {
					poolValue = poolFactory.getNumActive();
				}
				LOGGER.info("selectPoolValue={}, poolValue={}", selectPoolValue, poolValue);
				
				//这里为了简单，没有实现：如果有多个后端的conns/weight(连接池最大连接数)的值同为最小的，那么对它们采用加权轮询算法
				if ( poolValue < selectPoolValue ) {
					selectPoolFactory = poolFactory;
					selectPoolValue = poolValue;
				}
			}
			
			tProtocol = selectPoolFactory.getConnection(); 
		} finally {
			reentrantLock.unlock();
        }
		
		LOGGER.info("selectPoolFactory={}", selectPoolFactory);
		
		LoadBalanceBean loadBalanceBean = new LoadBalanceBean();
		loadBalanceBean.setConnectionPoolFactory(selectPoolFactory);
		loadBalanceBean.setProtocol(tProtocol);
		
		return loadBalanceBean;
	}
	
}
