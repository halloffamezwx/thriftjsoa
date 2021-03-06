package com.halloffame.thriftjsoa.common;

import com.halloffame.thriftjsoa.base.ConnectionFactory;
import com.halloffame.thriftjsoa.base.ThriftJsoaException;
import com.halloffame.thriftjsoa.config.BaseClientConfig;
import com.halloffame.thriftjsoa.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.config.common.ZkConnConfig;
import com.halloffame.thriftjsoa.constant.LoadBalanceType;
import com.halloffame.thriftjsoa.constant.MsgCode;
import com.halloffame.thriftjsoa.loadbalance.base.LoadBalanceAbstract;
import com.halloffame.thriftjsoa.loadbalance.unweight.LeastConnLoadBalance;
import com.halloffame.thriftjsoa.loadbalance.unweight.PollingLoadBalance;
import com.halloffame.thriftjsoa.loadbalance.unweight.RandomLoadBalance;
import com.halloffame.thriftjsoa.loadbalance.weight.LeastConnWeightLoadBalance;
import com.halloffame.thriftjsoa.loadbalance.weight.PollingWeightLoadBalance;
import com.halloffame.thriftjsoa.loadbalance.weight.RandomWeightLoadBalance;
import com.halloffame.thriftjsoa.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
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
    public static final int SOCKET_TIME_OUT = 3000;

    /**
     * 创建客户端
     */
    public static <T extends TServiceClient> T createClient(Class<T> clazz, String host, int port) throws Exception {
        return createClient(clazz, host, port, false).getClient();
    }

    /**
     * 创建客户端
     */
    public static <T extends TServiceClient> CreateClientResult<T> createClient(
            Class<T> clazz, String host, int port, boolean inTjServer) throws Exception {

        BaseClientConfig clientConfig = new BaseClientConfig();
        clientConfig.setHost(host);
        clientConfig.setPort(port);

        List<BaseClientConfig> clientConfigs = new ArrayList<>();
        clientConfigs.add(clientConfig);
        LoadBalanceClientConfig loadBalanceClientConfig = new LoadBalanceClientConfig();
        loadBalanceClientConfig.setClientConfigs(clientConfigs);
        loadBalanceClientConfig.setInTjServer(inTjServer);

        CreateLoadBalanceResult createLoadBalanceResult = createLoadBalance(loadBalanceClientConfig);
        TProtocol tProtocol = createLoadBalanceResult.getLoadBalance().getLoadBalanceBean().getProtocolWrap().getTProtocol();

        Constructor<T> constructor = clazz.getConstructor(TProtocol.class);
        T t = constructor.newInstance(tProtocol);
        CreateClientResult<T> result = new CreateClientResult<>();
        result.setClient(t);

        return result;
    }

    /**
     * 创建负载均衡对象
     */
    public static CreateLoadBalanceResult createLoadBalance(LoadBalanceClientConfig loadBalanceClientConfig) throws Exception {

        String loadBalanceType = loadBalanceClientConfig.getLoadBalanceType();
        if (loadBalanceType != null && !"".equals(loadBalanceType.trim()) && LoadBalanceType.getByCode(loadBalanceType) == null) {
            throw new ThriftJsoaException(MsgCode.UNKNOWN_LOAD_BALANCE, loadBalanceType);
        }
        LoadBalanceAbstract loadBalance = new LoadBalanceAbstract(){};

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
        }
        CreateLoadBalanceResult result = new CreateLoadBalanceResult();
        result.setLoadBalance(loadBalance);
        result.setLoadBalanceClientConfig(loadBalanceClientConfig);

        List<BaseClientConfig> clientConfigs = loadBalanceClientConfig.getClientConfigs();
        if (clientConfigs == null || clientConfigs.isEmpty()) {
            //连接注册中心（zooKeeper）读取节点信息建立与对应服务相通的连接工厂
            ZkConnConfig zkConnConfig = loadBalanceClientConfig.getZkConnConfig();
            String zkConnStr = zkConnConfig.getZkConnStr();
            int zkSessionTimeout = zkConnConfig.getZkSessionTimeout();
            String zkRootPath = zkConnConfig.getZkRootPath();
            String zkNodePath = zkConnConfig.getZkNodePath();

            ZooKeeper zk = new ZooKeeper(zkConnStr, zkSessionTimeout, null);
            ThriftJsoaWatcher watcher = new ThriftJsoaWatcher(zk, zkRootPath, loadBalance, loadBalanceClientConfig);
            zk.register(watcher);
            result.setZk(zk);

            if (zkNodePath == null || "".equals(zkNodePath.trim())) {
                List<String> nodePaths = zk.getChildren(zkRootPath, true);
                //todo 需要递归层级获取所有子节点？
                log.info("zk-nodePaths={}", nodePaths);
                for (String it : nodePaths) {
                    loadBalance.addConnectionFactory(getConnectionFactory(zk, zkRootPath, "/" + it, loadBalanceClientConfig));
                }
            } else {
                loadBalance.addConnectionFactory(getConnectionFactory(zk, zkRootPath, zkNodePath, loadBalanceClientConfig));
            }
        } else {
            for (BaseClientConfig it : clientConfigs) {
                //建立与服务对应的连接工厂
                it.setZkConnConfig(null);
                it.setClazzs(loadBalanceClientConfig.getClazzs());
                it.setInTjServer(loadBalanceClientConfig.isInTjServer());
                loadBalance.addConnectionFactory(new ConnectionFactory(it));
            }
        }

        return result;
    }

    /**
     * 获取连接工厂
     */
    private static ConnectionFactory getConnectionFactory(ZooKeeper zk, String zkRootPath, String zkNodePath,
                                                          LoadBalanceClientConfig loadBalanceClientConfig) throws Exception {
        //取服务注册到注册中心（zooKeeper）节点里保存的相关信息
        Stat stat = new Stat();
        //todo 如果数据变化，watch为true监听并动态改变连接工厂？
        byte[] zkNodeData = zk.getData(zkRootPath + zkNodePath, false, stat);
        String zkNodeStr = new String(zkNodeData, CommonServer.ZK_NODE_CHARSET);
        BaseClientConfig zkNodeObj = JsonUtil.deserialize(zkNodeStr, BaseClientConfig.class);

        zkNodeObj.setClazzs(loadBalanceClientConfig.getClazzs());
        zkNodeObj.setInTjServer(loadBalanceClientConfig.isInTjServer());

        return new ConnectionFactory(zkNodeObj);
    }

    /**
     * 监控注册中心（zooKeeper）节点相关变动，动态新增或删除服务（创建或关闭移除连接工厂）
     */
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
