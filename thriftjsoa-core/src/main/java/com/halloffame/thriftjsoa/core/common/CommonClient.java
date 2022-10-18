package com.halloffame.thriftjsoa.core.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.halloffame.thriftjsoa.core.base.ConnectionFactory;
import com.halloffame.thriftjsoa.core.base.TProtocolWrap;
import com.halloffame.thriftjsoa.core.base.ThriftJsoaException;
import com.halloffame.thriftjsoa.core.config.BaseClientConfig;
import com.halloffame.thriftjsoa.core.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.core.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.core.config.register.ZkRegisterConfig;
import com.halloffame.thriftjsoa.core.constant.LoadBalanceType;
import com.halloffame.thriftjsoa.core.constant.MsgCode;
import com.halloffame.thriftjsoa.core.loadbalance.base.LoadBalanceAbstract;
import com.halloffame.thriftjsoa.core.loadbalance.unweight.LeastConnLoadBalance;
import com.halloffame.thriftjsoa.core.loadbalance.unweight.PollingLoadBalance;
import com.halloffame.thriftjsoa.core.loadbalance.unweight.RandomLoadBalance;
import com.halloffame.thriftjsoa.core.loadbalance.weight.LeastConnWeightLoadBalance;
import com.halloffame.thriftjsoa.core.loadbalance.weight.PollingWeightLoadBalance;
import com.halloffame.thriftjsoa.core.loadbalance.weight.RandomWeightLoadBalance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 客户端工具类
 * @author zhuwx
 */
@Slf4j
public class CommonClient {

    /**
     * 客户端transportType为http的url模板，第一个通配符是host，第二个通配符是port，第三个通配符是path
     */
    public static final String HTTP_URL_TEMPLATE = "http://%s:%s/%s";

    /**
     * socket读超时时间，单位ms
     */
    public static final int SOCKET_TIME_OUT = 10000;

    /**
     * 创建客户端
     */
    @Deprecated
    public static <T extends TServiceClient> T createClient(Class<T> clazz, String host, int port) throws Exception {
        return createClient(clazz, host, port, false).getClient();
    }

    /**
     * 创建客户端
     */
    @Deprecated
    public static <T extends TServiceClient> CreateClientResult<T> createClient(
            Class<T> clazz, String host, int port, boolean inTjServer) throws Exception {

        BaseClientConfig clientConfig = new BaseClientConfig();
        clientConfig.setHost(host);
        clientConfig.setPort(port);

        List<ClientClassConfig> clazzs = new ArrayList<>();
        clazzs.add(new ClientClassConfig().setName(clazz));
        clientConfig.setClazzs(clazzs);

        List<BaseClientConfig> clientConfigs = new ArrayList<>();
        clientConfigs.add(clientConfig);
        LoadBalanceClientConfig loadBalanceClientConfig = new LoadBalanceClientConfig();
        loadBalanceClientConfig.setClientConfigs(clientConfigs);
        loadBalanceClientConfig.setInTjServer(inTjServer);

        CreateLoadBalanceResult createLoadBalanceResult = createLoadBalance(loadBalanceClientConfig);
        TProtocolWrap tProtocolWrap = createLoadBalanceResult.getLoadBalance().getLoadBalanceBean().getProtocolWrap();
        TProtocol inTProtocol = tProtocolWrap.getInTProtocolMap().get(clazz);
        TProtocol outTProtocol = tProtocolWrap.getOutTProtocolMap().get(clazz);

        Constructor<T> constructor = clazz.getConstructor(TProtocol.class, TProtocol.class);
        T t = constructor.newInstance(inTProtocol, outTProtocol);
        CreateClientResult<T> result = new CreateClientResult<>();
        result.setClient(t);

        return result;
    }

    /**
     * 优雅关机
     */
    public static void shutdownGraceful(LoadBalanceClientConfig loadBalanceClientConfig) throws TException {
        CreateLoadBalanceResult createLoadBalanceResult = createLoadBalance(loadBalanceClientConfig);
        for (ConnectionFactory it : createLoadBalanceResult.getLoadBalance().getConnectionFactorys()) {
            it.shutdownGraceful();
        }
    }

    /**
     * 获取服务状态
     */
    @Deprecated
    public static void getServerStatus(LoadBalanceClientConfig loadBalanceClientConfig) {
        CreateLoadBalanceResult createLoadBalanceResult = createLoadBalance(loadBalanceClientConfig);
        for (ConnectionFactory it : createLoadBalanceResult.getLoadBalance().getConnectionFactorys()) {
            it.getServerStatus();
        }
    }

    /**
     * 获取请求的服务名
     */
    public static String getAppName(LoadBalanceClientConfig loadBalanceClientConfig) {
        if (loadBalanceClientConfig == null) {
            return null;
        }
        String result = loadBalanceClientConfig.getAppName();
        if (result == null || result.length() <= 0) {
            ZkRegisterConfig zkRegisterConfig = loadBalanceClientConfig.getZkRegisterConfig();
            if (zkRegisterConfig != null) {
                return zkRegisterConfig.getZkRootPath();
            }
        }
        return null;
    }

    /**
     * 创建负载均衡对象
     */
    public static CreateLoadBalanceResult createLoadBalance(LoadBalanceClientConfig loadBalanceClientConfig) {

        String loadBalanceType = loadBalanceClientConfig.getLoadBalanceType();
        if (loadBalanceType != null && !"".equals(loadBalanceType.trim()) && LoadBalanceType.getByCode(loadBalanceType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_LOAD_BALANCE, loadBalanceType);
        }
        LoadBalanceAbstract loadBalance;

        if (LoadBalanceType.LEAST_CONN.getCode().equals(loadBalanceType)) {
            loadBalance = new LeastConnLoadBalance();
        } else if (LoadBalanceType.POLLING.getCode().equals(loadBalanceType)) {
            loadBalance = new PollingLoadBalance();
        } else if (LoadBalanceType.RANDOM.getCode().equals(loadBalanceType)) {
            loadBalance = new RandomLoadBalance();
        } else if (LoadBalanceType.LEAST_CONN_WEIGHT.getCode().equals(loadBalanceType)) {
            loadBalance = new LeastConnWeightLoadBalance();
        } else if (LoadBalanceType.POLLING_WEIGHT.getCode().equals(loadBalanceType)) {
            loadBalance = new PollingWeightLoadBalance();
        } else if (LoadBalanceType.RANDOM_WEIGHT.getCode().equals(loadBalanceType)) {
            loadBalance = new RandomWeightLoadBalance();
        } else {
            loadBalance = new LoadBalanceAbstract(){};
        }
        loadBalance.setAppName(getAppName(loadBalanceClientConfig));

        CreateLoadBalanceResult result = new CreateLoadBalanceResult();
        result.setLoadBalance(loadBalance);
        result.setLoadBalanceClientConfig(loadBalanceClientConfig);

        List<BaseClientConfig> clientConfigs = loadBalanceClientConfig.getClientConfigs();
        if (clientConfigs == null || clientConfigs.isEmpty()) {
            //连接注册中心读取节点信息建立与对应服务相通的连接工厂
            ZkRegisterConfig zkRegisterConfig = loadBalanceClientConfig.getZkRegisterConfig();
            String zkRootPath = zkRegisterConfig.getZkRootPath();

            CuratorFramework zkCf = loadBalanceClientConfig.getZkCf();
            if (Objects.isNull(zkCf)) {
                zkCf = CommonServer.connZkCf(zkRegisterConfig);
            }
            result.setZkCf(zkCf);
            //todo 优雅关机 zkCf.close();

            CuratorCache curatorCache = CuratorCache.builder(zkCf, zkRootPath).build();
            curatorCache.listenable().addListener((type, oldData, newData) -> {
                log.info("注册中心（zookeeper）监听事件：type={}, oldData={}, newData={}", type, oldData, newData);
                switch (type) {
                    case NODE_CREATED:
                        loadBalance.addConnectionFactory(getConnectionFactory(newData.getData(), loadBalanceClientConfig));
                        break;
                    case NODE_DELETED:
                        removeConnectionFactory(loadBalance, oldData.getPath(), oldData.getData());
                        break;
                    case NODE_CHANGED:
                        removeConnectionFactory(loadBalance, oldData.getPath(), oldData.getData());
                        loadBalance.addConnectionFactory(getConnectionFactory(newData.getData(), loadBalanceClientConfig));
                        break;
                    default:
                        break;
                }
            });
            curatorCache.start();

            /* ThriftJsoaWatcher watcher = new ThriftJsoaWatcher(zk, zkRootPath, loadBalance, loadBalanceClientConfig);
            zk.register(watcher);
            result.setZk(zk);
            String zkNodePath = zkConnConfig.getZkNodePath();
            if (zkNodePath == null || "".equals(zkNodePath.trim())) {
                List<String> nodePaths = zk.getChildren(zkRootPath, true);

                log.info("zk-nodePaths={}", nodePaths);
                for (String it : nodePaths) {
                    loadBalance.addConnectionFactory(getConnectionFactory(
                            zk, zkRootPath, "/" + it, loadBalanceClientConfig));
                }
            } else {
                loadBalance.addConnectionFactory(getConnectionFactory(zk, zkRootPath, zkNodePath, loadBalanceClientConfig));
            } */
        } else {
            for (BaseClientConfig it : clientConfigs) {
                //建立与服务对应的连接工厂
                it.setZkRegisterConfig(null);
                it.setClazzs(loadBalanceClientConfig.getClazzs());
                it.setInTjServer(loadBalanceClientConfig.isInTjServer());
                loadBalance.addConnectionFactory(new ConnectionFactory(it));
            }
        }

        return result;
    }

    /**
     * 移除连接工厂
     */
    @SneakyThrows
    private static void removeConnectionFactory(LoadBalanceAbstract loadBalance, String path, byte[] zkNodeData) {
        String appId = null;
        if (Objects.nonNull(zkNodeData) && zkNodeData.length > 0) {
            String zkNodeStr = new String(zkNodeData, CommonServer.ZK_NODE_CHARSET);
            BaseClientConfig zkNodeObj = JSON.parseObject(zkNodeStr, new TypeReference<BaseClientConfig>(){});
            appId = zkNodeObj.getAppId();
        }
        loadBalance.removeConnectionFactory(path, appId);
    }

    /**
     * 获取连接工厂
     */
    @SneakyThrows
    private static ConnectionFactory getConnectionFactory(byte[] zkNodeData, LoadBalanceClientConfig loadBalanceClientConfig) {
        if (Objects.isNull(zkNodeData) || zkNodeData.length <= 0) {
            return null;
        }
        String zkNodeStr = new String(zkNodeData, CommonServer.ZK_NODE_CHARSET);
        BaseClientConfig zkNodeObj = JSON.parseObject(zkNodeStr, new TypeReference<BaseClientConfig>(){});

        zkNodeObj.setClazzs(loadBalanceClientConfig.getClazzs());
        zkNodeObj.setInTjServer(loadBalanceClientConfig.isInTjServer());

        return new ConnectionFactory(zkNodeObj);
    }

    /**
     * 获取连接工厂
     */
    @Deprecated
    private static ConnectionFactory getConnectionFactory(ZooKeeper zk, String zkRootPath, String zkNodePath,
                                                          LoadBalanceClientConfig loadBalanceClientConfig) throws Exception {
        //取服务注册到注册中心（zooKeeper）节点里保存的相关信息
        Stat stat = new Stat();
        //todo 如果数据变化，watch为true监听并动态改变连接工厂？
        byte[] zkNodeData = zk.getData(zkRootPath + zkNodePath, false, stat);
        String zkNodeStr = new String(zkNodeData, CommonServer.ZK_NODE_CHARSET);
        BaseClientConfig zkNodeObj = JSON.parseObject(zkNodeStr, new TypeReference<BaseClientConfig>(){});

        zkNodeObj.setClazzs(loadBalanceClientConfig.getClazzs());
        zkNodeObj.setInTjServer(loadBalanceClientConfig.isInTjServer());

        return new ConnectionFactory(zkNodeObj);
    }

    /**
     * 监控注册中心（zooKeeper）节点相关变动，动态新增或删除服务（创建或关闭移除连接工厂）
     */
    @Deprecated
    static class ThriftJsoaWatcher implements Watcher {
        private ZooKeeper zk;
        private String zkRootPath;
        private LoadBalanceAbstract loadBalance;
        private LoadBalanceClientConfig loadBalanceClientConfig;

        public ThriftJsoaWatcher(ZooKeeper zk, String zkRootPath, LoadBalanceAbstract loadBalance, LoadBalanceClientConfig loadBalanceClientConfig) {
            this.zk = zk;
            this.zkRootPath = zkRootPath;
            this.loadBalance = loadBalance;
            this.loadBalanceClientConfig = loadBalanceClientConfig;
        }

        @Override
        public void process(WatchedEvent event) {
            log.info("ThriftJsoaWatcher 已经触发了{}事件！", event.getType());

            //子节点变动：新增或删除
            if (event.getType() == Event.EventType.NodeChildrenChanged) {
                try {
                    List<String> nodePaths = zk.getChildren(zkRootPath, true);
                    List<ConnectionFactory> connectionFactorys = loadBalance.getConnectionFactorys();

                    for (String it : nodePaths) {
                        boolean isFind = false;
                        for (ConnectionFactory connectionFactory : connectionFactorys) {
                            if ( (zkRootPath + "/" + it).equals(connectionFactory.toString()) ) {
                                isFind = true;
                                break;
                            }
                        }
                        if (!isFind) { //新增服务
                            log.info("ThriftJsoaWatcher 上线{}", it);
                            loadBalance.addConnectionFactory(getConnectionFactory(zk, zkRootPath, "/" + it, loadBalanceClientConfig));
                        }
                    }

                    Iterator<ConnectionFactory> it = connectionFactorys.iterator();
                    while (it.hasNext()) {
                        ConnectionFactory connectionFactory = it.next();
                        boolean isFind = false;

                        for (String nodePathIt : nodePaths) {
                            if ( connectionFactory.toString().equals(zkRootPath + "/" + nodePathIt) ) {
                                isFind = true;
                                break;
                            }
                        }
                        if (!isFind) { //下线服务
                            log.info("ThriftJsoaWatcher 下线{}", connectionFactory);
                            loadBalance.removeConnectionFactory(connectionFactory, it);
                        }
                    }
                } catch (Exception e) {
                    log.error("ThriftJsoaWatcher zk NodeChildrenChanged process exception:", e);
                }
            }
        }
    }

}
