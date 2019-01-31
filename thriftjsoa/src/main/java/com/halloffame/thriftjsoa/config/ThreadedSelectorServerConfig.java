package com.halloffame.thriftjsoa.config;

import com.halloffame.thriftjsoa.common.ServerType;

/**
 * 服务模式：非阻塞io，有一条线程专门负责accept，若干条Selector线程处理网络IO，一个Worker线程池处理消息
 */
public class ThreadedSelectorServerConfig extends BaseServerConfig {
	private int selectorThreads = 2; //用来处理已经连接的网络IO的线程数，默认2
    //用来处理请求消息的工作线程数（未指定executorService），设为0时，将在selectorThreads里处理，TNonblockingServer就是这样处理的，默认5
    private int workerThreads = 5;
    //accept线程的连接传到每个selector线程的阻塞队列的大小，默认4
    private int acceptQueueSizePerThread = 4;

	@Override
	public String getServerType() {
		return ServerType.THREADED_SELECTOR.getValue();
	}
	
	public int getSelectorThreads() {
		return selectorThreads;
	}
	public void setSelectorThreads(int selectorThreads) {
		this.selectorThreads = selectorThreads;
	}
	public int getWorkerThreads() {
		return workerThreads;
	}
	public void setWorkerThreads(int workerThreads) {
		this.workerThreads = workerThreads;
	}
	public int getAcceptQueueSizePerThread() {
		return acceptQueueSizePerThread;
	}
	public void setAcceptQueueSizePerThread(int acceptQueueSizePerThread) {
		this.acceptQueueSizePerThread = acceptQueueSizePerThread;
	} 
    
}
