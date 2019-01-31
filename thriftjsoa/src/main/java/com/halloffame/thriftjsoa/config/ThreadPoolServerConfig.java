package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.common.ServerType;

/**
 * 服务模式：多线程（池）阻塞io
 */
public class ThreadPoolServerConfig extends BaseServerConfig {
	private int minWorkerThreads = 5; //线程池的最小线程数，默认5
	private int maxWorkerThreads = Integer.MAX_VALUE; //线程池的最大线程数，默认int最大值

	@Override
	public String getServerType() {
		return ServerType.THREAD_POOL.getValue();
	}
	
	public int getMinWorkerThreads() {
		return minWorkerThreads;
	}
	public void setMinWorkerThreads(int minWorkerThreads) {
		this.minWorkerThreads = minWorkerThreads;
	}
	public int getMaxWorkerThreads() {
		return maxWorkerThreads;
	}
	public void setMaxWorkerThreads(int maxWorkerThreads) {
		this.maxWorkerThreads = maxWorkerThreads;
	}
	
}
