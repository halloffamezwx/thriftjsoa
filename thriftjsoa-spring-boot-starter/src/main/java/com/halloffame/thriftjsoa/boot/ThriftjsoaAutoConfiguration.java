package com.halloffame.thriftjsoa.boot;

import com.halloffame.thriftjsoa.ThriftJsoaProxy;
import com.halloffame.thriftjsoa.ThriftJsoaServer;
import com.halloffame.thriftjsoa.boot.annotation.EnableTjClients;
import com.halloffame.thriftjsoa.boot.annotation.TjClient;
import com.halloffame.thriftjsoa.boot.annotation.TjClientScan;
import com.halloffame.thriftjsoa.boot.config.ThriftJsoaProxyConfig;
import com.halloffame.thriftjsoa.boot.config.ThriftJsoaServerConfig;
import com.halloffame.thriftjsoa.boot.config.TjExecutorService;
import com.halloffame.thriftjsoa.boot.config.TjProxyExecutorService;
import com.halloffame.thriftjsoa.boot.constant.ThriftjsoaConstant;
import com.halloffame.thriftjsoa.boot.properties.ThriftjsoaProperties;
import com.halloffame.thriftjsoa.boot.runner.ThriftjsoaProxyRunner;
import com.halloffame.thriftjsoa.boot.runner.ThriftjsoaServerRunner;
import com.halloffame.thriftjsoa.common.CommonServer;
import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.config.client.ThriftJsoaClientConfig;
import com.halloffame.thriftjsoa.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.config.common.ZkConnConfig;
import com.halloffame.thriftjsoa.session.ThriftJsoaSessionFactory;
import com.halloffame.thriftjsoa.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.thrift.TProcessor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * spring boot starter的AutoConfiguration
 * @author zhuwx
 */
@Configuration
@ConditionalOnClass({ThriftJsoaServer.class, ThriftJsoaProxy.class})
@EnableConfigurationProperties(ThriftjsoaProperties.class)
@ConditionalOnProperty(value = ThriftjsoaConstant.THRIFTJSOA_PREFIX)
@Slf4j
public class ThriftjsoaAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ThriftjsoaProperties thriftjsoaProperties;

    @Autowired
    //@Qualifier("thriftjsoaProcessor")
    private TProcessor tProcessor; //需要使用该starter的业务工程实例化一个TProcessor类型的bean

    @Autowired(required = false)
    private TjExecutorService tjExecutorService;

    @Autowired(required = false)
    private TjProxyExecutorService tjProxyExecutorService;

    private BaseServerConfig serverConfig;

    @PostConstruct
    public void init() throws Exception {
        CuratorFramework zkCf = null;
        ZkConnConfig zkConnConfig = null;
        ThriftJsoaServerConfig server = thriftjsoaProperties.getServer();
        ThriftJsoaProxyConfig proxy = thriftjsoaProperties.getProxy();

        if (Objects.nonNull(server)) {
            if (server.getSimpleServerConfig() != null) {
                serverConfig = server.getSimpleServerConfig();
            } else if (server.getThreadPoolServerConfig() != null) {
                serverConfig = server.getThreadPoolServerConfig();
            } else if (server.getNonblockingServerConfig() != null) {
                serverConfig = server.getNonblockingServerConfig();
            } else {
                serverConfig = server.getThreadedSelectorServerConfig().setExecutorService(
                        tjExecutorService != null ? tjExecutorService.getExecutorService() : null);
            }
            serverConfig.setProcessor(tProcessor);
            zkConnConfig = serverConfig.getZkConnConfig();
            zkCf = CommonServer.connZkCf(zkConnConfig);
            serverConfig.setZkCf(zkCf);
        }
        if (Objects.nonNull(proxy)) {
            if (proxy.getSimpleServerConfig() != null) {
                proxy.setServerConfig(proxy.getSimpleServerConfig());
            } else if (proxy.getThreadPoolServerConfig() != null) {
                proxy.setServerConfig(proxy.getThreadPoolServerConfig());
            } else if (proxy.getNonblockingServerConfig() != null) {
                proxy.setServerConfig(proxy.getNonblockingServerConfig());
            } else {
                proxy.setServerConfig(proxy.getThreadedSelectorServerConfig().setExecutorService(
                        tjProxyExecutorService != null ? tjProxyExecutorService.getExecutorService() : null));
            }
            BaseServerConfig baseServerConfig = proxy.getServerConfig();
            baseServerConfig.setProcessor(tProcessor);

            CuratorFramework proxyZkCf = zkCf;
            ZkConnConfig proxyZkConnConfig = zkConnConfig;
            LoadBalanceClientConfig loadBalanceClientConfig = proxy.getLoadBalanceClientConfig();
            if (Objects.nonNull(loadBalanceClientConfig.getZkConnConfig())) {
                proxyZkConnConfig = loadBalanceClientConfig.getZkConnConfig();
                proxyZkCf = CommonServer.connZkCf(proxyZkConnConfig);
            }
            loadBalanceClientConfig.setZkConnConfig(proxyZkConnConfig);
            loadBalanceClientConfig.setZkCf(proxyZkCf);

            if (Objects.isNull(baseServerConfig.getZkConnConfig())) {
                baseServerConfig.setZkConnConfig(proxyZkConnConfig);
                baseServerConfig.setZkCf(proxyZkCf);
            } else {
                baseServerConfig.setZkCf(CommonServer.connZkCf(baseServerConfig.getZkConnConfig()));
            }
        }
    }

    /**
     * 服务端runner bean
     */
    @Bean
    @ConditionalOnMissingBean(ThriftjsoaServerRunner.class)
    @ConditionalOnProperty(value = ThriftjsoaConstant.THRIFTJSOA_SERVER_PREFIX)
    public ThriftjsoaServerRunner thriftjsoaServerRunner() {
        return new ThriftjsoaServerRunner(new ThriftJsoaServer(serverConfig));
    }

    /**
     * 代理端runner bean
     */
    @Bean
    @ConditionalOnMissingBean(ThriftjsoaProxyRunner.class)
    @ConditionalOnProperty(value = ThriftjsoaConstant.THRIFTJSOA_PROXY_PREFIX)
    public ThriftjsoaProxyRunner thriftjsoaProxyRunner() {
        ThriftjsoaProxyRunner runner = new ThriftjsoaProxyRunner(new ThriftJsoaProxy(thriftjsoaProperties.getProxy()));
        return runner;
    }

    /**
     * 客户端 ThriftJsoaSessionFactory bean
     */
    @Bean
    @ConditionalOnMissingBean(ThriftJsoaSessionFactory.class)
    //@ConditionalOnProperty(value = ThriftjsoaConstant.THRIFTJSOA_CLIENT_PREFIX)
    public ThriftJsoaSessionFactory thriftJsoaSessionFactory() throws Exception {
        if (Objects.isNull(thriftjsoaProperties.getServer()) &&
            Objects.isNull(thriftjsoaProperties.getClient())
        ) {
            return new ThriftJsoaSessionFactory();
        }
        ThriftJsoaClientConfig client = thriftjsoaProperties.getClient();
        if (Objects.isNull(client)) {
            client = new ThriftJsoaClientConfig();
        }
        if (Objects.nonNull(thriftjsoaProperties.getServer())) {
            client.setInTjServer(true);
        }

        List<Class<?>> tjClientClassArr = new ArrayList<>();
        if (!applicationContext.getBeansWithAnnotation(EnableTjClients.class).isEmpty()) {
            String[] packageNames = null;
            Map<String, Object> tjClientScanBeanMap = applicationContext.getBeansWithAnnotation(TjClientScan.class);
            if (!tjClientScanBeanMap.isEmpty()) {
                TjClientScan tjClientScan = tjClientScanBeanMap.values().toArray()[0].getClass().getAnnotation(TjClientScan.class);
                packageNames = tjClientScan.value();
            }
            if (packageNames == null || packageNames.length == 0) {
                packageNames = new String[1];
                packageNames[0] = applicationContext.getBeansWithAnnotation(SpringBootApplication.class).values().toArray()[0]
                        .getClass().getPackage().getName();
            }

            for (String packageName : packageNames) {
                for (Class<?> clazz : ClassUtil.getClzFromPkg(packageName)) {
                    TjClient tjClient = clazz.getAnnotation(TjClient.class);
                    if (Objects.nonNull(tjClient)) {
                        tjClientClassArr.add(clazz);
                    }
                }
            }
        }

        List<LoadBalanceClientConfig> loadBalanceClientConfigs = client.getList();
        if (Objects.isNull(loadBalanceClientConfigs)) {
            loadBalanceClientConfigs = new ArrayList<>();
            client.setList(loadBalanceClientConfigs);
        }

        Map<String, List<Class<?>>> tjClientClassMap = new HashMap<>();
        for (Class<?> tjClientClass : tjClientClassArr) {
            TjClient tjClient = tjClientClass.getAnnotation(TjClient.class);
            if (StringUtils.isEmpty(tjClient.value())) {
                continue;
            }

            List<Class<?>> tjClientClassSubArr = tjClientClassMap.computeIfAbsent(tjClient.value(), k -> new ArrayList<>());
            tjClientClassSubArr.add(tjClientClass);
        }

        for (Map.Entry<String, List<Class<?>>> entry : tjClientClassMap.entrySet()) {
            List<Class<?>> tjClientClassSubArr = entry.getValue();

            boolean isFindCfg = false;
            for (LoadBalanceClientConfig loadBalanceClientConfigIt : loadBalanceClientConfigs) {
                ZkConnConfig zkConnConfig = loadBalanceClientConfigIt.getZkConnConfig();

                if (Objects.isNull(zkConnConfig) || StringUtils.isEmpty(zkConnConfig.getZkRootPath()) ||
                    Objects.equals(entry.getKey(), zkConnConfig.getZkRootPath()))
                {
                    if (Objects.isNull(zkConnConfig) || StringUtils.isEmpty(zkConnConfig.getZkRootPath())) {
                        if (Objects.isNull(zkConnConfig)) {
                            zkConnConfig = new ZkConnConfig().setZkRootPath(entry.getKey());
                            loadBalanceClientConfigIt.setZkConnConfig(zkConnConfig);
                        }
                        zkConnConfig.setZkRootPath(entry.getKey());
                    }

                    for (Class<?> tjClientClass : tjClientClassSubArr) {
                        TjClient tjClient = tjClientClass.getAnnotation(TjClient.class);

                        boolean isFindClass = false;
                        for (ClientClassConfig clientClassConfig : loadBalanceClientConfigIt.getClazzs()) {
                            if (Objects.equals(tjClient.multipleServiceName(), clientClassConfig.getServiceName())) {
                                if (StringUtils.isEmpty(clientClassConfig.getSessionName())) {
                                    clientClassConfig.setSessionName(tjClientClass);
                                }
                                isFindClass = true;
                                break;
                            }
                        }
                        if (!isFindClass) {
                            loadBalanceClientConfigIt.getClazzs().add(ClientClassConfig.builder()
                                    .sessionName(tjClientClass).serviceName(tjClient.multipleServiceName()).build());
                        }
                    }

                    isFindCfg = true;
                    break;
                }
            }

            if (!isFindCfg && Objects.nonNull(thriftjsoaProperties.getServer())) {
                LoadBalanceClientConfig loadBalanceClientConfig = new LoadBalanceClientConfig();
                List<ClientClassConfig> clazzs = new ArrayList<>();
                for (Class<?> tjClientClass : tjClientClassSubArr) {
                    TjClient tjClient = tjClientClass.getAnnotation(TjClient.class);
                    clazzs.add(ClientClassConfig.builder().sessionName(tjClientClass).serviceName(tjClient.multipleServiceName()).build());
                }

                loadBalanceClientConfig.setClazzs(clazzs);
                loadBalanceClientConfig.setLoadBalanceType(thriftjsoaProperties.getServer().getLoadBalanceType());
                loadBalanceClientConfig.setZkConnConfig(new ZkConnConfig().setZkRootPath(entry.getKey()));
                loadBalanceClientConfigs.add(loadBalanceClientConfig);
            }
        }

        if (Objects.nonNull(serverConfig)) {
            for (LoadBalanceClientConfig loadBalanceClientConfig : loadBalanceClientConfigs) {
                ZkConnConfig zkConnConfig = loadBalanceClientConfig.getZkConnConfig();
                if (Objects.isNull(zkConnConfig) || StringUtils.isEmpty(zkConnConfig.getZkConnStr())) {
                    loadBalanceClientConfig.setZkCf(serverConfig.getZkCf());
                }
            }
        }

        ThriftJsoaSessionFactory sessionFactory = new ThriftJsoaSessionFactory(client);

        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();

        for (Object obj : sessionFactory.getClientMap().values()) {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ThriftJsoaSessionClientFactoryBean.class);
            AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(obj);

            beanFactory.registerBeanDefinition(obj.getClass().getSimpleName(), beanDefinition);
        }

        return sessionFactory;
    }

    static class ThriftJsoaSessionClientFactoryBean implements FactoryBean {

        private Object obj;

        public ThriftJsoaSessionClientFactoryBean(Object obj) {
            this.obj = obj;
        }

        @Nullable
        @Override
        public Object getObject() throws Exception {
            return obj;
        }

        @Nullable
        @Override
        public Class<?> getObjectType() {
            return obj.getClass();
        }
    }

}
