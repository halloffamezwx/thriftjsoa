package com.halloffame.thriftjsoa.config;

/**
 * 服务模式：多线程（池）阻塞io
 */
public class ThreadPoolServerConfig extends BaseServerConfig {
	private String serverType = "thread-pool"; //多线程（池）阻塞io

	private int minWorkerThreads = 5; //线程池的最小线程数
	private int maxWorkerThreads = Integer.MAX_VALUE; //线程池的最大线程数
	
	public String getServerType() {
		return serverType;
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
