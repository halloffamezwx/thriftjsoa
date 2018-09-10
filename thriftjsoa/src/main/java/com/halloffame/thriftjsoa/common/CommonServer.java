package com.halloffame.thriftjsoa.common;

import java.util.UUID;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.config.ThreadPoolServerConfig;
import com.halloffame.thriftjsoa.config.ThreadedSelectorServerConfig;

/**
 * 启动不同类型的server
 */
public class CommonServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommonServer.class.getName());
	
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
        
        //Set server event handler
        serverEngine.setServerEventHandler(new MyServerEventHandler());

        // Run it
        serverEngine.serve();
	}

	static class MyServerContext implements ServerContext {
		int connectionId;

	    public MyServerContext(int connectionId) {
	    	this.connectionId = connectionId;
	    }

	    public int getConnectionId() {
	        return connectionId;
	    }
	    public void setConnectionId(int connectionId) {
	        this.connectionId = connectionId;
	    }
	}

	static class MyServerEventHandler implements TServerEventHandler {
		private int nextConnectionId = 1;

        public void preServe() {
        	LOGGER.info("MyServerEventHandler.preServe - called only once before server starts accepting connections");
        }

        public ServerContext createContext(TProtocol input, TProtocol output) {
            //we can create some connection level data which is stored while connection is alive & served
        	MyServerContext ctx = new MyServerContext(nextConnectionId++);
        	LOGGER.info("MyServerEventHandler.createContext - connection #{} established", ctx.getConnectionId());
            return ctx;
        }

        public void processContext(ServerContext serverContext, TTransport inputTransport, TTransport outputTransport) {
        	MDC.put("mdc_trace_id", UUID.randomUUID().toString());
        	MyServerContext ctx = (MyServerContext)serverContext;
        	LOGGER.info("MyServerEventHandler.processContext - connection #{} is ready to process next request", ctx.getConnectionId());
        }
        
        public void deleteContext(ServerContext serverContext, TProtocol input, TProtocol output) {
        	MyServerContext ctx = (MyServerContext)serverContext;
        	LOGGER.info("MyServerEventHandler.deleteContext - connection #{} terminated", ctx.getConnectionId());
        }
    }
    
}
