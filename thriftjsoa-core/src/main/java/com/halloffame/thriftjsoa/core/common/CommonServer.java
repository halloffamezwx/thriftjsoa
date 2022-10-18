package com.halloffame.thriftjsoa.core.common;

import com.alibaba.fastjson2.JSON;
import com.halloffame.thriftjsoa.core.base.ThriftJsoaException;
import com.halloffame.thriftjsoa.core.base.ThriftJsoaProcessor;
import com.halloffame.thriftjsoa.core.base.ThriftJsoaProtocol;
import com.halloffame.thriftjsoa.core.config.BaseClientConfig;
import com.halloffame.thriftjsoa.core.config.BaseConfig;
import com.halloffame.thriftjsoa.core.config.BaseServerConfig;
import com.halloffame.thriftjsoa.core.config.common.ProcessorConfig;
import com.halloffame.thriftjsoa.core.config.register.ZkRegisterConfig;
import com.halloffame.thriftjsoa.core.config.server.NettyServerConfig;
import com.halloffame.thriftjsoa.core.config.server.ThreadPoolServerConfig;
import com.halloffame.thriftjsoa.core.config.server.ThreadedSelectorServerConfig;
import com.halloffame.thriftjsoa.core.config.server.TomcatServerConfig;
import com.halloffame.thriftjsoa.core.constant.MsgCode;
import com.halloffame.thriftjsoa.core.constant.ProtocolType;
import com.halloffame.thriftjsoa.core.constant.ServerType;
import com.halloffame.thriftjsoa.core.constant.TransportType;
import com.halloffame.thriftjsoa.core.protocol.TAvroProtocol;
import com.halloffame.thriftjsoa.core.protocol.TKryoProtocol;
import com.halloffame.thriftjsoa.core.protocol.TProtostuffProtocol;
import com.halloffame.thriftjsoa.core.server.TNettyServer;
import com.halloffame.thriftjsoa.core.server.TTomcatServer;
import com.halloffame.thriftjsoa.core.session.ThriftJsoaSessionProcessor;
import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.MDC;

import java.lang.reflect.ParameterizedType;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * 启动不同类型的server
 * @author zhuwx
 */
@Slf4j
public class CommonServer {

    /**
     * 服务监听端口
     */
    public static int PORT = 9090;

    /**
     * 注册中心（zookeeper）根路径
     */
    public static final String ZK_ROOT_PATH = "/thriftJsoaServer";

    /**
     * 向注册中心（zookeeper）注册本服务节点名称的分隔符
     */
    public static final String ZK_NODE_SEPARATOR = "-";

    /**
     * 向注册中心（zookeeper）注册本服务节点的path
     */
    public static final String ZK_NODE_PATH = "/localhost" + ZK_NODE_SEPARATOR + PORT;

    /**
     * 服务的唯一标识，默认ZK_ROOT_PATH + ZK_NODE_PATH
     */
    public static String appId;

    /**
     * 注册中心（zookeeper）连接串
     */
    public static final String ZK_CONN_STR = "localhost:2181";

    /**
     * 注册中心（zookeeper）会话的有效时间，单位是毫秒
     */
    public static final int ZK_SESSION_TIMEOUT = 5000;

    /**
     * 向注册中心（zookeeper）注册本服务节点保存信息的字符编码
     */
    public static final String ZK_NODE_CHARSET = "UTF-8";

    /**
     * 链接连通性检查的请求的不存在的接口名
     */
    public static final String CONN_VALIDATE_METHOD_NAME = "thriftJsoaConnValidateMethod";

    /**
     * 优雅关机的请求的不存在的接口名
     */
    public static final String SHUTDOWN_GRACEFUL_METHOD_NAME = "thriftJsoaShutdownGracefulMethod";

    /**
     * 获取服务状态的请求的不存在的接口名
     */
    public static final String GET_SERVER_STATUS_METHOD_NAME = "thriftJsoaGetServerStatusMethod";

    @Getter
    private static volatile Thread gracefulShutdownThread; //优雅关机线程
    @Getter
    private static volatile TServer serverEngine; //服务模式

    /**
     * 根据配置启动不同类型的server
     */
    public static void serve(BaseServerConfig serverConfig) throws Exception {
        if (Objects.isNull(serverConfig.getHost()) || "".equals(serverConfig.getHost().trim())) {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostAddress = inetAddress.getHostAddress(); //本地ip
            serverConfig.setHost(hostAddress);
        }
        appId = genAppId(serverConfig);
        //serverConfig.setAppId(appId);

        int port = serverConfig.getPort();
        boolean ssl = serverConfig.isSsl(); //传输是否加密
        String inTTransportType = serverConfig.getInTransportType() == null ? serverConfig.getGeneralTransportType() : serverConfig.getInTransportType();
        String outTTransportType = serverConfig.getOutTransportType() == null ? serverConfig.getGeneralTransportType() : serverConfig.getOutTransportType();

        String inTProtocolType = serverConfig.getInProtocolType() == null ? serverConfig.getGeneralProtocolType() : serverConfig.getInProtocolType();
        String outTProtocolType = serverConfig.getOutProtocolType() == null ? serverConfig.getGeneralProtocolType() : serverConfig.getOutProtocolType();

        String serverType = serverConfig.getServerType(); //服务模式
        String connValidateMethodName = serverConfig.getConnValidateMethodName(); //链接连通性检查的请求的不存在的接口名
        String shutdownGracefulMethodName = serverConfig.getShutdownGracefulMethodName();
        String getServerStatusMethodName = serverConfig.getGetServerStatusMethodName();

        //检查传入的变量值是否正确
        if (ServerType.SIMPLE.getCode().equals(serverType)) {
        } else if (ServerType.THREAD_POOL.getCode().equals(serverType)) {
        } else if (ServerType.NETTY.getCode().equals(serverType)) {
        } else if (ServerType.NONBLOCKING.getCode().equals(serverType)) {
            if (ssl == true) {
                throw new ThriftJsoaException(MsgCode.SSL_NOT_SUPPORT);
            }
        } else if (serverType.equals(ServerType.THREADED_SELECTOR.getCode())) {
            if (ssl == true) {
                throw new ThriftJsoaException(MsgCode.SSL_NOT_SUPPORT);
            }
        } else {
            if (ServerType.getByCode(serverType) == null) {
                throw new ThriftJsoaException(MsgCode.UNKNOWN_SERVER, serverType);
            }
        }

        if (ProtocolType.getByCode(inTProtocolType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_PROTOCOL, inTProtocolType);
        }
        if (ProtocolType.getByCode(outTProtocolType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_PROTOCOL, outTProtocolType);
        }

        if (TransportType.getByCode(inTTransportType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_TRANSPORT, inTTransportType);
        }
        if (TransportType.getByCode(outTTransportType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_TRANSPORT, outTTransportType);
        }

        TProtocolFactory inTProtocolFactory; //传输协议
        TProtocolFactory outTProtocolFactory;

        TKryoProtocol.Factory inTKryoProtocolFactory = null;
        if (ProtocolType.JSON.getCode().equals(inTProtocolType)) {
            inTProtocolFactory = new TJSONProtocol.Factory();
        } else if (ProtocolType.COMPACT.getCode().equals(inTProtocolType)) {
            inTProtocolFactory = new TCompactProtocol.Factory();
        } else if (ProtocolType.KRYO.getCode().equals(inTProtocolType)) {
            inTKryoProtocolFactory = new TKryoProtocol.Factory();
            inTProtocolFactory = inTKryoProtocolFactory;

        } else if (ProtocolType.PROTOSTUFF.getCode().equals(inTProtocolType)) {
            inTProtocolFactory = new TProtostuffProtocol.Factory();
        } else if (ProtocolType.AVRO.getCode().equals(inTProtocolType)) {
            inTProtocolFactory = new TAvroProtocol.Factory();
        } else {
            inTProtocolFactory = new TBinaryProtocol.Factory();
        }
        inTProtocolFactory = new ThriftJsoaProtocol.Factory(inTProtocolFactory);

        TKryoProtocol.Factory outTKryoProtocolFactory = null;
        if (ProtocolType.JSON.getCode().equals(outTProtocolType)) {
            outTProtocolFactory = new TJSONProtocol.Factory();
        } else if (ProtocolType.COMPACT.getCode().equals(outTProtocolType)) {
            outTProtocolFactory = new TCompactProtocol.Factory();
        } else if (ProtocolType.KRYO.getCode().equals(outTProtocolType)) {
            outTKryoProtocolFactory = new TKryoProtocol.Factory();
            outTProtocolFactory = outTKryoProtocolFactory;

        } else if (ProtocolType.PROTOSTUFF.getCode().equals(outTProtocolType)) {
            outTProtocolFactory = new TProtostuffProtocol.Factory();
        } else if (ProtocolType.AVRO.getCode().equals(outTProtocolType)) {
            outTProtocolFactory = new TAvroProtocol.Factory();
        } else {
            outTProtocolFactory = new TBinaryProtocol.Factory();
        }
        outTProtocolFactory = new ThriftJsoaProtocol.Factory(outTProtocolFactory);

        TTransportFactory inTTransportFactory; //传输方式
        TTransportFactory outTTransportFactory;

        if (TransportType.FRAMED.getCode().equals(inTTransportType)) {
            inTTransportFactory = new TFramedTransport.Factory();
        } else if (TransportType.FASTFRAMED.getCode().equals(inTTransportType)) {
            inTTransportFactory = new TFastFramedTransport.Factory();
        } else if (TransportType.KRYO.getCode().equals(inTTransportType)) {
            inTTransportFactory = new TKryoTransport.Factory();
        } else { // .equals("buffered") => default value
            inTTransportFactory = new TTransportFactory();
        }

        if (TransportType.FRAMED.getCode().equals(outTTransportType)) {
            outTTransportFactory = new TFramedTransport.Factory();
        } else if (TransportType.FASTFRAMED.getCode().equals(outTTransportType)) {
            outTTransportFactory = new TFastFramedTransport.Factory();
        } else if (TransportType.KRYO.getCode().equals(outTTransportType)) {
            outTTransportFactory = new TKryoTransport.Factory();
        } else { // .equals("buffered") => default value
            outTTransportFactory = new TTransportFactory();
        }

        TProcessor tProcessor = null;
        TMultiplexedProcessor tMultiplexedProcessor = null;
        if (serverConfig.getProcessorConfigs().size() > 1) {
            tMultiplexedProcessor = new TMultiplexedProcessor();
            tProcessor = tMultiplexedProcessor;
        }

        for (ProcessorConfig it : serverConfig.getProcessorConfigs()) {
            TProcessor value = it.getTProcessor();

            if (tMultiplexedProcessor != null) {
                tMultiplexedProcessor.registerProcessor(it.getServiceName(), value);
            } else {
                tProcessor = value;
            }

            if (value instanceof ThriftJsoaSessionProcessor) {
                ThriftJsoaSessionProcessor<?> thriftJsoaSessionProcessor = (ThriftJsoaSessionProcessor<?>) value;
                serverConfig.getServiceClazzs().add(thriftJsoaSessionProcessor.getT().getClass());
            } else {
                ParameterizedType parameterizedType = (ParameterizedType) value.getClass().getGenericSuperclass();
                if (parameterizedType != null) {
                    serverConfig.getServiceClazzs().add((Class<?>) parameterizedType.getActualTypeArguments()[0]);
                }
            }
        }

        for (Class<?> it : serverConfig.getServiceClazzs()) {
            Set<Class<?>> classSet = ThriftJsoaUtil.getMethodStructTypes(it);

            if (inTKryoProtocolFactory != null) {
                inTKryoProtocolFactory.addClazzs(classSet);
            }
            if (outTKryoProtocolFactory != null) {
                outTKryoProtocolFactory.addClazzs(classSet);
            }
        }

        tProcessor = new ThriftJsoaProcessor(tProcessor, connValidateMethodName,
                shutdownGracefulMethodName, getServerStatusMethodName);

        //TServer serverEngine; //服务模式
        if (ServerType.NONBLOCKING.getCode().equals(serverType) || ServerType.THREADED_SELECTOR.getCode().equals(serverType)) {
            // Nonblocking servers
            TNonblockingServerSocket tNonblockingServerSocket = new TNonblockingServerSocket(
                    new TNonblockingServerSocket.NonblockingAbstractServerSocketArgs().port(port).clientTimeout(serverConfig.getSocketTimeOut()));

            if (ServerType.NONBLOCKING.getCode().equals(serverType)) {
                // Nonblocking Server
                TNonblockingServer.Args tNonblockingServerArgs
                        = new TNonblockingServer.Args(tNonblockingServerSocket);
                tNonblockingServerArgs.processor(tProcessor);
                tNonblockingServerArgs.inputProtocolFactory(inTProtocolFactory);
                tNonblockingServerArgs.outputProtocolFactory(outTProtocolFactory);
                tNonblockingServerArgs.inputTransportFactory(inTTransportFactory);
                tNonblockingServerArgs.outputTransportFactory(outTTransportFactory);

                serverEngine = new TNonblockingServer(tNonblockingServerArgs);
            } else { // "threaded-selector".equals(server_type)
                // ThreadedSelector Server
                TThreadedSelectorServer.Args tThreadedSelectorServerArgs
                        = new TThreadedSelectorServer.Args(tNonblockingServerSocket);
                tThreadedSelectorServerArgs.processor(tProcessor);

                tThreadedSelectorServerArgs.inputProtocolFactory(inTProtocolFactory);
                tThreadedSelectorServerArgs.outputProtocolFactory(outTProtocolFactory);
                tThreadedSelectorServerArgs.inputTransportFactory(inTTransportFactory);
                tThreadedSelectorServerArgs.outputTransportFactory(outTTransportFactory);

                ThreadedSelectorServerConfig threadedSelectorServerConfig = (ThreadedSelectorServerConfig) serverConfig;
                tThreadedSelectorServerArgs.selectorThreads(threadedSelectorServerConfig.getSelectorThreads());
                tThreadedSelectorServerArgs.workerThreads(threadedSelectorServerConfig.getWorkerThreads());
                tThreadedSelectorServerArgs.acceptQueueSizePerThread(threadedSelectorServerConfig.getAcceptQueueSizePerThread());
                tThreadedSelectorServerArgs.executorService(threadedSelectorServerConfig.getExecutorService());

                serverEngine = new TThreadedSelectorServer(tThreadedSelectorServerArgs);
            }
        } else if (ServerType.SIMPLE.getCode().equals(serverType) || ServerType.THREAD_POOL.getCode().equals(serverType)) {
            // Blocking servers
            // SSL socket
            TServerSocket tServerSocket;
            if (ssl) {
                tServerSocket = TSSLTransportFactory.getServerSocket(port, serverConfig.getSocketTimeOut());
            } else {
                tServerSocket = new TServerSocket(new TServerSocket.ServerSocketTransportArgs().port(port).clientTimeout(serverConfig.getSocketTimeOut()));
            }

            if (ServerType.SIMPLE.getCode().equals(serverType)) {
                // Simple Server
                TServer.Args tServerArgs = new TServer.Args(tServerSocket);
                tServerArgs.processor(tProcessor);

                tServerArgs.inputProtocolFactory(inTProtocolFactory);
                tServerArgs.outputProtocolFactory(outTProtocolFactory);
                tServerArgs.inputTransportFactory(inTTransportFactory);
                tServerArgs.outputTransportFactory(outTTransportFactory);

                serverEngine = new TSimpleServer(tServerArgs);
            } else { // "threadpool".equals(server_type)
                // ThreadPool Server
                TThreadPoolServer.Args tThreadPoolServerArgs
                        = new TThreadPoolServer.Args(tServerSocket);
                tThreadPoolServerArgs.processor(tProcessor);

                tThreadPoolServerArgs.inputProtocolFactory(inTProtocolFactory);
                tThreadPoolServerArgs.outputProtocolFactory(outTProtocolFactory);
                tThreadPoolServerArgs.inputTransportFactory(inTTransportFactory);
                tThreadPoolServerArgs.outputTransportFactory(outTTransportFactory);

                ThreadPoolServerConfig threadPoolServerConfig = (ThreadPoolServerConfig) serverConfig;
                tThreadPoolServerArgs.minWorkerThreads(threadPoolServerConfig.getMinWorkerThreads());
                tThreadPoolServerArgs.maxWorkerThreads(threadPoolServerConfig.getMaxWorkerThreads());

                serverEngine = new TThreadPoolServer(tThreadPoolServerArgs);
            }
        } else if (ServerType.HTTP_TOMCAT.getCode().equals(serverType)) {
            TTomcatServer.Args tTomcatServerArgs = new TTomcatServer.Args(null);
            tTomcatServerArgs.processor(tProcessor);

            tTomcatServerArgs.inputProtocolFactory(inTProtocolFactory);
            tTomcatServerArgs.outputProtocolFactory(outTProtocolFactory);
            tTomcatServerArgs.inputTransportFactory(inTTransportFactory);
            tTomcatServerArgs.outputTransportFactory(outTTransportFactory);

            TomcatServerConfig tomcatServerConfig = (TomcatServerConfig) serverConfig;
            tTomcatServerArgs.baseDir(tomcatServerConfig.getBasedir());
            tTomcatServerArgs.port(port);
            tTomcatServerArgs.maxConnections(tomcatServerConfig.getMaxConnections());
            tTomcatServerArgs.acceptCount(tomcatServerConfig.getAcceptCount());
            tTomcatServerArgs.maxThreads(tomcatServerConfig.getMaxThreads());
            tTomcatServerArgs.minSpareThreads(tomcatServerConfig.getMinSpareThreads());
            tTomcatServerArgs.connectionTimeout(serverConfig.getSocketTimeOut());
            tTomcatServerArgs.httpPath(serverConfig.getHttpPath());

            serverEngine = new TTomcatServer(tTomcatServerArgs);
        } else { //ServerType.NETTY.getCode().equals(serverType)
            TNettyServer.Args tNettyServerArgs = new TNettyServer.Args(null);
            tNettyServerArgs.processor(tProcessor);

            tNettyServerArgs.inputProtocolFactory(inTProtocolFactory);
            tNettyServerArgs.outputProtocolFactory(outTProtocolFactory);
            tNettyServerArgs.inputTransportFactory(inTTransportFactory);
            tNettyServerArgs.outputTransportFactory(outTTransportFactory);

            NettyServerConfig nettyServerConfig = (NettyServerConfig) serverConfig;
            tNettyServerArgs.port(port);
            tNettyServerArgs.soTimeout(nettyServerConfig.getSocketTimeOut());
            tNettyServerArgs.nThreads(nettyServerConfig.getNThreads());
            tNettyServerArgs.ssl(nettyServerConfig.isSsl());
            tNettyServerArgs.executorService(nettyServerConfig.getExecutorService());

            serverEngine = new TNettyServer(tNettyServerArgs);
        }

        //Set server event handler
        serverEngine.setServerEventHandler(new ThriftJsoaServerEventHandler());

        final CuratorFramework zkCf;
        if (Objects.nonNull(serverConfig.getZkCf())) {
            zkCf = serverConfig.getZkCf();
        } else {
            if (serverConfig.getZkRegisterConfig() != null) {
                zkCf = connZkCf(serverConfig.getZkRegisterConfig()); //创建一个与ZooKeeper服务器的连接
            } else {
                zkCf = null;
            }
        }

        gracefulShutdownThread = new GracefulShutdownThread(zkCf, serverEngine, serverConfig);
        gracefulShutdownThread.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(gracefulShutdownThread);

        Thread servingThread = new Thread(() -> {
            int i = 0;
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            } while (!serverEngine.isServing() && i++ < 50);

            if (serverEngine.isServing()) {
                if (Objects.nonNull(zkCf)) {
                    register(zkCf, serverConfig);
                    log.info("注册节点完毕");
                }
            } else {
                log.info("服务启动超时");
                serverEngine.stop();
            }
        });
        servingThread.setDaemon(true);
        servingThread.start();

        // Run it
        serverEngine.serve();
    }

    /**
     * 生成appId
     */
    public static String genAppId(BaseConfig baseConfig) {
        if (Objects.nonNull(baseConfig.getAppId()) && !"".equals(baseConfig.getAppId().trim())) {
            return baseConfig.getAppId();
        }

        if (baseConfig.getZkRegisterConfig() != null) {
            ZkRegisterConfig zkRegisterConfig = baseConfig.getZkRegisterConfig();
            if (zkRegisterConfig != null) {
                return zkRegisterConfig.getZkRootPath() + zkRegisterConfig.getZkNodePath();
            }
        }

        return baseConfig.getHost() + ":" + baseConfig.getPort();
    }

    /**
     * 优雅停机线程
     */
    static class GracefulShutdownThread extends Thread {
        private CuratorFramework zkCf;
        private TServer serverEngine;
        private BaseServerConfig serverConfig;

        public GracefulShutdownThread(CuratorFramework zkCf, TServer serverEngine, BaseServerConfig serverConfig) {
            this.zkCf = zkCf;
            this.serverEngine = serverEngine;
            this.serverConfig = serverConfig;
        }

        @Override
        public void run() {
            log.info("收到关闭服务的信号");
            if (Objects.nonNull(zkCf)) {
                try {
                    ZkRegisterConfig zkRegisterConfig = serverConfig.getZkRegisterConfig();
                    String path = zkRegisterConfig.getZkRootPath() + zkRegisterConfig.getZkNodePath();
                    zkCf.delete().forPath(path);
                    zkCf.close();
                } catch (Exception e) {
                    log.error("删除注册中心（zookeeper）节点异常：", e);
                    serverEngine.stop();
                    return;
                }
            }
            serverEngine.setShouldStop(true);

            int i = 0;
            while (serverEngine.isServing() && i++ < serverConfig.getShutdownCheckFrequency()) {
                log.info("服务正在忙{}x{}...", i, serverConfig.getShutdownCheckIntervalTime());
                try {
                    Thread.sleep(serverConfig.getShutdownCheckIntervalTime());
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            }
            if (serverEngine.isServing()) {
                log.info("服务关闭超时");
                serverEngine.stop();
            }
            //System.exit(0);
        }
    }

    /**
     * 连接注册中心（zooKeeper）
     */
    @Deprecated
    public static ZooKeeper connZk(ZkRegisterConfig zkRegisterConfig) throws Exception {
        if (Objects.isNull(zkRegisterConfig) || Objects.isNull(zkRegisterConfig.getZkConnStr()) || "".equals(zkRegisterConfig.getZkConnStr().trim())) {
            return null;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ZooKeeper zk = new ZooKeeper(zkRegisterConfig.getZkConnStr(), zkRegisterConfig.getZkSessionTimeout(), event -> {
            if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        log.info("zookeeper连接状态：{}", zk.getState()); //CONNECTED
        return zk;
    }

    /**
     * 连接注册中心（zooKeeper）- CuratorFramework
     */
    public static CuratorFramework connZkCf(ZkRegisterConfig zkRegisterConfig) {
        if (Objects.isNull(zkRegisterConfig) || Objects.isNull(zkRegisterConfig.getZkConnStr()) || "".equals(zkRegisterConfig.getZkConnStr().trim())) {
            return null;
        }
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(zkRegisterConfig.getZkConnStr()).sessionTimeoutMs(zkRegisterConfig.getZkSessionTimeout())
                .retryPolicy(new ExponentialBackoffRetry(1000,3))
                .namespace("").build();
        curatorFramework.start();
        return curatorFramework;
    }

    /**
     * 注册节点
     */
    @SneakyThrows
    private static void register(CuratorFramework zkCf, BaseServerConfig serverConfig) {
        if (Objects.nonNull(zkCf)) { //需要在zooKeeper创建节点
            ZkRegisterConfig zkRegisterConfig = serverConfig.getZkRegisterConfig();
            String zkRootPath = zkRegisterConfig.getZkRootPath();
            String zkNodePath = zkRegisterConfig.getZkNodePath();
            String path = zkRootPath + zkNodePath;

            //Stat stat = zk.exists(zkRootPath, false);
            Stat stat = zkCf.checkExists().forPath(zkRootPath);
            if (stat == null) { //不存在就创建根节点
                //zk.create(zkRootPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                zkCf.create().creatingParentsIfNeeded().forPath(zkRootPath);
            }

            //注册中心节点保存的数据，client/proxy将会读取这些数据来进行一些操作，例如创建client/proxy连接server的连接工厂等
            BaseClientConfig registerClientConnServerConfig = serverConfig.getRegisterClientConnServerConfig();
            if (registerClientConnServerConfig == null) {
                registerClientConnServerConfig = new BaseClientConfig();
            }
            registerClientConnServerConfig.setHost(serverConfig.getHost());
            //registerClientConnServerConfig.setSocketTimeOut(0);
            //registerClientConnServerConfig.setPoolConfig(null);
            registerClientConnServerConfig.setAppId(appId);
            registerClientConnServerConfig.setPort(serverConfig.getPort());
            registerClientConnServerConfig.setSsl(serverConfig.isSsl());

            registerClientConnServerConfig.setGeneralTransportType(serverConfig.getGeneralTransportType());
            registerClientConnServerConfig.setInTransportType(serverConfig.getInTransportType());
            registerClientConnServerConfig.setOutTransportType(serverConfig.getOutTransportType());
            registerClientConnServerConfig.setGeneralProtocolType(serverConfig.getGeneralProtocolType());
            registerClientConnServerConfig.setInProtocolType(serverConfig.getInProtocolType());
            registerClientConnServerConfig.setOutProtocolType(serverConfig.getOutProtocolType());

            registerClientConnServerConfig.setConnValidateMethodName(serverConfig.getConnValidateMethodName());
            registerClientConnServerConfig.setShutdownGracefulMethodName(serverConfig.getShutdownGracefulMethodName());
            registerClientConnServerConfig.setGetServerStatusMethodName(serverConfig.getGetServerStatusMethodName());
            registerClientConnServerConfig.setHttpPath(serverConfig.getHttpPath());
            registerClientConnServerConfig.setZkRegisterConfig(serverConfig.getZkRegisterConfig());

            //创建一个子节点
            //zk.create(path, JsonUtil.serialize(registerClientConnServerConfig).getBytes(CommonServer.ZK_NODE_CHARSET),
            //        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            zkCf.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path,
                    JSON.toJSONString(registerClientConnServerConfig).getBytes(CommonServer.ZK_NODE_CHARSET));
        }
    }

    /**
     * 调用接口前后触发事件（传递的数据结构）
     */
    @Data
    static class ThriftJsoaServerContext implements ServerContext {

        int connectionId;

        public ThriftJsoaServerContext(int connectionId) {
            this.connectionId = connectionId;
        }
    }

    /**
     * 调用接口前后触发事件
     */
    static class ThriftJsoaServerEventHandler implements TServerEventHandler {

        private int nextConnectionId = 1;

        public void preServe() {
            log.debug("ThriftJsoaServerEventHandler.preServe - called only once before server starts accepting connections");
        }

        public ServerContext createContext(TProtocol input, TProtocol output) {
            //we can create some connection level data which is stored while connection is alive & served
            ThriftJsoaServerContext ctx = new ThriftJsoaServerContext(nextConnectionId++);
            log.debug("ThriftJsoaServerEventHandler.createContext - connection #{} established", ctx.getConnectionId());
            return ctx;
        }

        public void processContext(ServerContext serverContext, TTransport inputTransport, TTransport outputTransport) {
            ThriftJsoaServerContext ctx = (ThriftJsoaServerContext) serverContext;
            log.debug("ThriftJsoaServerEventHandler.processContext - connection #{} is ready to process next request", ctx.getConnectionId());
            MDC.remove(ThriftJsoaProtocol.TRACE_KEY);
            MDC.remove(ThriftJsoaProtocol.APP_KEY);
        }

        public void deleteContext(ServerContext serverContext, TProtocol input, TProtocol output) {
            ThriftJsoaServerContext ctx = (ThriftJsoaServerContext) serverContext;
            log.debug("ThriftJsoaServerEventHandler.deleteContext - connection #{} terminated", ctx.getConnectionId());
            MDC.remove(ThriftJsoaProtocol.TRACE_KEY);
            MDC.remove(ThriftJsoaProtocol.APP_KEY);

            //if (output instanceof TKryoProtocol) {
            //    TKryoProtocol tKryoProtocol = (TKryoProtocol) output;
            //    tKryoProtocol.closeOutput();
            //}
        }
    }

}
