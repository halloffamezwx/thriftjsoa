package com.halloffame.thriftjsoa.loadbalance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;

public abstract class WeightLoadBalanceAbstract extends LoadBalanceAbstract {
	//加权的连接池list，权重是连接池的最大连接数
	private List<ConnectionPoolFactory> weightPoolFactorys = new ArrayList<ConnectionPoolFactory>();
	
	public List<ConnectionPoolFactory> getWeightPoolFactorys() {
		return weightPoolFactorys;
	}
	
	@Override
	public void addPoolFactory(ConnectionPoolFactory poolFactory) {
		super.addPoolFactory(poolFactory);
		
		for (int i = 0; i < poolFactory.getMaxTotal(); i++) {
			weightPoolFactorys.add(poolFactory);
		}
	}
	
	@Override
	public void removePoolFactory(ConnectionPoolFactory poolFactory, Iterator<ConnectionPoolFactory> it) {
		super.removePoolFactory(poolFactory, it); 
		Iterator<ConnectionPoolFactory> weightIt = weightPoolFactorys.iterator();
		
		while (weightIt.hasNext()) {
			ConnectionPoolFactory weightPoolFactory = weightIt.next();
			
		    if (poolFactory.equals(weightPoolFactory)) {
		    	weightPoolFactory.close();
		    	weightIt.remove();
		    	weightPoolFactory = null;
		    }
		}
		
	}
	
}
