package com.halloffame.thriftjsoa.common;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportFactory;

import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.config.ThreadPoolServerConfig;
import com.halloffame.thriftjsoa.config.ThreadedSelectorServerConfig;

/**
 * 启动不同类型的server
 */
public class CommonServer {
	
	public static void serve(int port, BaseServerConfig serverConfig, TProcessor tProcessor) throws Exception {
		boolean ssl = serverConfig.isSsl();
		String transport_type = serverConfig.getTransportType(); 
		String protocol_type = serverConfig.getProtocolType(); 
		String server_type = serverConfig.getServerType();
		
		//检查传入的变量值是否正确
        if (server_type.equals("simple")) {
        } else if (server_type.equals("thread-pool")) {
        } else if (server_type.equals("nonblocking")) {
        	if (ssl == true) {
        		throw new Exception("SSL is not supported over nonblocking servers!");
        	}
        } else if (server_type.equals("threaded-selector")) {
        	if (ssl == true) {
        		throw new Exception("SSL is not supported over nonblocking servers!");
        	}
        } else {
        	throw new Exception("Unknown server type! " + server_type);
        }
        
        if (protocol_type.equals("binary")) {
        } else if (protocol_type.equals("json")) {
        } else if (protocol_type.equals("compact")) {
        } else {
        	throw new Exception("Unknown protocol type! " + protocol_type);
        }
        
        if (transport_type.equals("buffered")) {
        } else if (transport_type.equals("framed")) {
        } else if (transport_type.equals("fastframed")) {
        } else {
        	throw new Exception("Unknown transport type! " + transport_type);
        }

        // Protocol factory
        TProtocolFactory tProtocolFactory = null; //指定的通信协议
        
        if (protocol_type.equals("json")) {
        	tProtocolFactory = new TJSONProtocol.Factory();
        } else if (protocol_type.equals("compact")) {
        	tProtocolFactory = new TCompactProtocol.Factory();
        } else {
        	tProtocolFactory = new TBinaryProtocol.Factory();
        }

        TTransportFactory tTransportFactory = null; //指定的通信方式
        
        if (transport_type.equals("framed")) {
        	tTransportFactory = new TFramedTransport.Factory();
        } else if (transport_type.equals("fastframed")) {
        	tTransportFactory = new TFastFramedTransport.Factory();
        } else { // .equals("buffered") => default value
        	tTransportFactory = new TTransportFactory();
        }

        TServer serverEngine = null; //指定的服务器模式	

        if (server_type.equals("nonblocking") || server_type.equals("threaded-selector")) {
        	// Nonblocking servers
        	TNonblockingServerSocket tNonblockingServerSocket =
        			new TNonblockingServerSocket(new TNonblockingServerSocket.NonblockingAbstractServerSocketArgs().port(port));

	        if (server_type.equals("nonblocking")) {
	        	// Nonblocking Server
	        	TNonblockingServer.Args tNonblockingServerArgs
	              	= new TNonblockingServer.Args(tNonblockingServerSocket);
	        	tNonblockingServerArgs.processor(tProcessor);
	        	tNonblockingServerArgs.protocolFactory(tProtocolFactory);
	        	tNonblockingServerArgs.transportFactory(tTransportFactory);
	
	        	serverEngine = new TNonblockingServer(tNonblockingServerArgs);
	        } else { // server_type.equals("threaded-selector")
	        	// ThreadedSelector Server
	        	TThreadedSelectorServer.Args tThreadedSelectorServerArgs
	              	= new TThreadedSelectorServer.Args(tNonblockingServerSocket);
	        	tThreadedSelectorServerArgs.processor(tProcessor);
	        	tThreadedSelectorServerArgs.protocolFactory(tProtocolFactory);
	        	tThreadedSelectorServerArgs.transportFactory(tTransportFactory);
	        	
	        	ThreadedSelectorServerConfig threadedSelectorServerConfig = (ThreadedSelectorServerConfig)serverConfig;
	        	tThreadedSelectorServerArgs.selectorThreads(threadedSelectorServerConfig.getSelectorThreads());
	        	tThreadedSelectorServerArgs.workerThreads(threadedSelectorServerConfig.getWorkerThreads());
	        	tThreadedSelectorServerArgs.acceptQueueSizePerThread(threadedSelectorServerConfig.getAcceptQueueSizePerThread());
	
	        	serverEngine = new TThreadedSelectorServer(tThreadedSelectorServerArgs);
	        }
        } else {
        	// Blocking servers	
        	// SSL socket
        	TServerSocket tServerSocket = null;
        	if (ssl) {
        		tServerSocket = TSSLTransportFactory.getServerSocket(port, 0);
        	} else {
        		tServerSocket = new TServerSocket(new TServerSocket.ServerSocketTransportArgs().port(port));
        	}

        	if (server_type.equals("simple")) {
        		// Simple Server
        		TServer.Args tServerArgs = new TServer.Args(tServerSocket);
        		tServerArgs.processor(tProcessor);
        		tServerArgs.protocolFactory(tProtocolFactory);
        		tServerArgs.transportFactory(tTransportFactory);

        		serverEngine = new TSimpleServer(tServerArgs);
        	} else { // server_type.equals("threadpool")
        		// ThreadPool Server
        		TThreadPoolServer.Args tThreadPoolServerArgs
        			= new TThreadPoolServer.Args(tServerSocket);
        		tThreadPoolServerArgs.processor(tProcessor);
        		tThreadPoolServerArgs.protocolFactory(tProtocolFactory);
        		tThreadPoolServerArgs.transportFactory(tTransportFactory);
        		
        		ThreadPoolServerConfig threadPoolServerConfig = (ThreadPoolServerConfig)serverConfig;
        		tThreadPoolServerArgs.minWorkerThreads(threadPoolServerConfig.getMinWorkerThreads());
        		tThreadPoolServerArgs.maxWorkerThreads(threadPoolServerConfig.getMaxWorkerThreads());

        		serverEngine = new TThreadPoolServer(tThreadPoolServerArgs);
        	}
        }

        // Run it
        serverEngine.serve();
	}
	
}
