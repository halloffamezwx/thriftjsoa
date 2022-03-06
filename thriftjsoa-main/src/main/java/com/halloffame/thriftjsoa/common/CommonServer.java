package com.halloffame.thriftjsoa.common;

import com.halloffame.thriftjsoa.base.ThriftJsoaException;
import com.halloffame.thriftjsoa.base.ThriftJsoaProcessor;
import com.halloffame.thriftjsoa.base.ThriftJsoaProtocol;
import com.halloffame.thriftjsoa.config.BaseClientConfig;
import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.config.common.ZkConnConfig;
import com.halloffame.thriftjsoa.config.server.ThreadPoolServerConfig;
import com.halloffame.thriftjsoa.config.server.ThreadedSelectorServerConfig;
import com.halloffame.thriftjsoa.constant.MsgCode;
import com.halloffame.thriftjsoa.constant.ProtocolType;
import com.halloffame.thriftjsoa.constant.ServerType;
import com.halloffame.thriftjsoa.constant.TransportType;
import com.halloffame.thriftjsoa.util.JsonUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.util.Objects;
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
     * 根据配置启动不同类型的server
     */
    public static void serve(BaseServerConfig serverConfig) throws Exception {
        ZkConnConfig zkConnConfig = serverConfig.getZkConnConfig();
        ZooKeeper zk = serverConfig.getZk();

        InetAddress inetAddress = InetAddress.getLocalHost();
        String hostAddress = inetAddress.getHostAddress(); //本地ip

        String zkRootPath = null;
        String zkNodePath = null;
        if (zkConnConfig != null) {
            zkRootPath = zkConnConfig.getZkRootPath();
            zkNodePath = zkConnConfig.getZkNodePath();

            if (zkNodePath == null || "".equals(zkNodePath.trim())) {
                zkNodePath = "/" + hostAddress + ZK_NODE_SEPARATOR + serverConfig.getPort();
            }
        }
        String path = zkRootPath + zkNodePath;

        String appId = serverConfig.getAppId();
        if (appId != null && !"".equals(appId.trim())) {
            CommonServer.appId = appId;
        } else {
            if (zkConnConfig != null) {
                CommonServer.appId = path;
            } else {
                CommonServer.appId = hostAddress + ZK_NODE_SEPARATOR + serverConfig.getPort();
            }
        }

        if (zk != null || zkConnConfig != null) { //需要连接zooKeeper创建节点
            if (Objects.isNull(zk)) {
                zk = connZk(zkConnConfig); //创建一个与ZooKeeper服务器的连接
            }

            Stat stat = zk.exists(zkRootPath, false);
            if (stat == null) { //不存在就创建根节点
                zk.create(zkRootPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            //注册中心（zookeeper）节点保存的数据，client/proxy将会读取这些数据来进行一些操作，例如创建client/proxy连接server的连接工厂等
            BaseClientConfig zkClientConnServerConfig = serverConfig.getZkClientConnServerConfig();
            if (zkClientConnServerConfig == null) {
                zkClientConnServerConfig = new BaseClientConfig();
            }
            zkClientConnServerConfig.setHost(hostAddress);
            //zkClientConnServerConfig.setSocketTimeOut(0);
            //zkClientConnServerConfig.setPoolConfig(null);
            zkClientConnServerConfig.setAppId(CommonServer.appId);
            zkClientConnServerConfig.setPort(serverConfig.getPort());
            zkClientConnServerConfig.setSsl(serverConfig.isSsl());
            zkClientConnServerConfig.setTransportType(serverConfig.getTransportType());
            zkClientConnServerConfig.setProtocolType(serverConfig.getProtocolType());
            zkClientConnServerConfig.setConnValidateMethodName(serverConfig.getConnValidateMethodName());
            zkClientConnServerConfig.setHttpPath(serverConfig.getHttpPath());
            zkClientConnServerConfig.setZkConnConfig(serverConfig.getZkConnConfig());

            //创建一个子节点
            zk.create(path, JsonUtil.serialize(zkClientConnServerConfig).getBytes(CommonServer.ZK_NODE_CHARSET),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        }

        int port = serverConfig.getPort();
        boolean ssl = serverConfig.isSsl(); //传输是否加密
        String transportType = serverConfig.getTransportType(); //传输方式
        String protocolType = serverConfig.getProtocolType(); //传输协议
        String serverType = serverConfig.getServerType(); //服务模式
        String connValidateMethodName = serverConfig.getConnValidateMethodName(); //链接连通性检查的请求的不存在的接口名

        //检查传入的变量值是否正确
        if (ServerType.SIMPLE.getCode().equals(serverType)) {
        } else if (ServerType.THREAD_POOL.getCode().equals(serverType)) {
        } else if (ServerType.NONBLOCKING.getCode().equals(serverType)) {
            if (ssl == true) {
                throw new ThriftJsoaException(MsgCode.SSL_NOT_SUPPORT);
            }
        } else if (serverType.equals(ServerType.THREADED_SELECTOR.getCode())) {
            if (ssl == true) {
                throw new ThriftJsoaException(MsgCode.SSL_NOT_SUPPORT);
            }
        } else {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_SERVER, serverType);
        }

        if (ProtocolType.getByCode(protocolType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_PROTOCOL, protocolType);
        }
        if (TransportType.getByCode(transportType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_TRANSPORT, transportType);
        }

        TProtocolFactory tProtocolFactory; //传输协议

        if (ProtocolType.JSON.getCode().equals(protocolType)) {
            tProtocolFactory = new TJSONProtocol.Factory();
        } else if (ProtocolType.COMPACT.getCode().equals(protocolType)) {
            tProtocolFactory = new TCompactProtocol.Factory();
        } else {
            tProtocolFactory = new TBinaryProtocol.Factory();
        }
        tProtocolFactory = new ThriftJsoaProtocol.Factory(tProtocolFactory);

        TTransportFactory tTransportFactory; //传输方式

        if (TransportType.FRAMED.getCode().equals(transportType)) {
            tTransportFactory = new TFramedTransport.Factory();
        } else if (TransportType.FASTFRAMED.getCode().equals(transportType)) {
            tTransportFactory = new TFastFramedTransport.Factory();
        } else { // .equals("buffered") => default value
            tTransportFactory = new TTransportFactory();
        }
        TProcessor tProcessor = new ThriftJsoaProcessor(serverConfig.getProcessor(), connValidateMethodName);
        TServer serverEngine; //服务模式

        if (ServerType.NONBLOCKING.getCode().equals(serverType) || ServerType.THREADED_SELECTOR.getCode().equals(serverType)) {
            // Nonblocking servers
            TNonblockingServerSocket tNonblockingServerSocket =
                    new TNonblockingServerSocket(new TNonblockingServerSocket.NonblockingAbstractServerSocketArgs().port(port));

            if (ServerType.NONBLOCKING.getCode().equals(serverType)) {
                // Nonblocking Server
                TNonblockingServer.Args tNonblockingServerArgs
                        = new TNonblockingServer.Args(tNonblockingServerSocket);
                tNonblockingServerArgs.processor(tProcessor);
                tNonblockingServerArgs.protocolFactory(tProtocolFactory);
                tNonblockingServerArgs.transportFactory(tTransportFactory);

                serverEngine = new TNonblockingServer(tNonblockingServerArgs);
            } else { // "threaded-selector".equals(server_type)
                // ThreadedSelector Server
                TThreadedSelectorServer.Args tThreadedSelectorServerArgs
                        = new TThreadedSelectorServer.Args(tNonblockingServerSocket);
                tThreadedSelectorServerArgs.processor(tProcessor);
                tThreadedSelectorServerArgs.protocolFactory(tProtocolFactory);
                tThreadedSelectorServerArgs.transportFactory(tTransportFactory);

                ThreadedSelectorServerConfig threadedSelectorServerConfig = (ThreadedSelectorServerConfig) serverConfig;
                tThreadedSelectorServerArgs.selectorThreads(threadedSelectorServerConfig.getSelectorThreads());
                tThreadedSelectorServerArgs.workerThreads(threadedSelectorServerConfig.getWorkerThreads());
                tThreadedSelectorServerArgs.acceptQueueSizePerThread(threadedSelectorServerConfig.getAcceptQueueSizePerThread());
                tThreadedSelectorServerArgs.executorService(threadedSelectorServerConfig.getExecutorService());

                serverEngine = new TThreadedSelectorServer(tThreadedSelectorServerArgs);
            }
        } else {
            // Blocking servers
            // SSL socket
            TServerSocket tServerSocket;
            if (ssl) {
                tServerSocket = TSSLTransportFactory.getServerSocket(port, 0);
            } else {
                tServerSocket = new TServerSocket(new TServerSocket.ServerSocketTransportArgs().port(port));
            }

            if (ServerType.SIMPLE.getCode().equals(serverType)) {
                // Simple Server
                TServer.Args tServerArgs = new TServer.Args(tServerSocket);
                tServerArgs.processor(tProcessor);
                tServerArgs.protocolFactory(tProtocolFactory);
                tServerArgs.transportFactory(tTransportFactory);

                serverEngine = new TSimpleServer(tServerArgs);
            } else { // "threadpool".equals(server_type)
                // ThreadPool Server
                TThreadPoolServer.Args tThreadPoolServerArgs
                        = new TThreadPoolServer.Args(tServerSocket);
                tThreadPoolServerArgs.processor(tProcessor);
                tThreadPoolServerArgs.protocolFactory(tProtocolFactory);
                tThreadPoolServerArgs.transportFactory(tTransportFactory);

                ThreadPoolServerConfig threadPoolServerConfig = (ThreadPoolServerConfig) serverConfig;
                tThreadPoolServerArgs.minWorkerThreads(threadPoolServerConfig.getMinWorkerThreads());
                tThreadPoolServerArgs.maxWorkerThreads(threadPoolServerConfig.getMaxWorkerThreads());

                serverEngine = new TThreadPoolServer(tThreadPoolServerArgs);
            }
        }

        //Set server event handler
        serverEngine.setServerEventHandler(new ThriftJsoaServerEventHandler());

        // Run it
        serverEngine.serve();
    }

    /**
     * 连接注册中心（zooKeeper）
     */
    public static ZooKeeper connZk(ZkConnConfig zkConnConfig) throws Exception {
        if (Objects.isNull(zkConnConfig)) {
            return null;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ZooKeeper zk = new ZooKeeper(zkConnConfig.getZkConnStr(), zkConnConfig.getZkSessionTimeout(), event -> {
            if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        log.info("zookeeper连接状态：{}", zk.getState()); //CONNECTED
        return zk;
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
        }
    }

}
