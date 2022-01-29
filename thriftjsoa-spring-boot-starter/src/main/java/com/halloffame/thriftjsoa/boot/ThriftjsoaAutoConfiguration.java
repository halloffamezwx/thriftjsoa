package com.halloffame.thriftjsoa.boot;

import com.halloffame.thriftjsoa.ThriftJsoaProxy;
import com.halloffame.thriftjsoa.ThriftJsoaServer;
import com.halloffame.thriftjsoa.boot.annotation.TjClient;
import com.halloffame.thriftjsoa.boot.annotation.TjClientScan;
import com.halloffame.thriftjsoa.boot.config.TjExecutorService;
import com.halloffame.thriftjsoa.boot.constant.ThriftjsoaConstant;
import com.halloffame.thriftjsoa.boot.properties.ThriftjsoaProperties;
import com.halloffame.thriftjsoa.boot.runner.ThriftjsoaProxyRunner;
import com.halloffame.thriftjsoa.boot.runner.ThriftjsoaServerRunner;
import com.halloffame.thriftjsoa.config.BaseServerConfig;
import com.halloffame.thriftjsoa.session.ThriftJsoaSessionFactory;
import com.halloffame.thriftjsoa.util.ClassUtil;
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

import java.util.Map;

/**
 * spring boot starter的AutoConfiguration
 * @author zhuwx
 */
@Configuration
@ConditionalOnClass({ThriftJsoaServer.class, ThriftJsoaProxy.class})
@EnableConfigurationProperties(ThriftjsoaProperties.class)
@ConditionalOnProperty(value = ThriftjsoaConstant.THRIFTJSOA_PREFIX)
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

    /**
     * 服务端runner bean
     */
    @Bean
    @ConditionalOnMissingBean(ThriftjsoaServerRunner.class)
    @ConditionalOnProperty(value = ThriftjsoaConstant.THRIFTJSOA_SERVER_PREFIX)
    public ThriftjsoaServerRunner thriftjsoaServerRunner() {
        BaseServerConfig serverConfig = null;

        if (thriftjsoaProperties.getServer().getThreadedSelectorServerConfig() != null) {
            thriftjsoaProperties.getServer().getThreadedSelectorServerConfig().setExecutorService(tjExecutorService.getExecutorService());
            serverConfig = thriftjsoaProperties.getServer().getThreadedSelectorServerConfig();

        } else if (thriftjsoaProperties.getServer().getNonblockingServerConfig() != null) {
            serverConfig = thriftjsoaProperties.getServer().getNonblockingServerConfig();

        } else if (thriftjsoaProperties.getServer().getThreadPoolServerConfig() != null) {
            serverConfig = thriftjsoaProperties.getServer().getThreadPoolServerConfig();

        } else if (thriftjsoaProperties.getServer().getSimpleServerConfig() != null) {
            serverConfig = thriftjsoaProperties.getServer().getSimpleServerConfig();
        }
        serverConfig.setProcessor(tProcessor);

        ThriftjsoaServerRunner runner = new ThriftjsoaServerRunner(new ThriftJsoaServer(serverConfig));
        return runner;
    }

    /**
     * 代理端runner bean
     */
    @Bean
    @ConditionalOnMissingBean(ThriftjsoaProxyRunner.class)
    @ConditionalOnProperty(value = ThriftjsoaConstant.THRIFTJSOA_PROXY_PREFIX)
    public ThriftjsoaProxyRunner thriftjsoaProxyRunner() {

        if (thriftjsoaProperties.getProxy().getThreadedSelectorServerConfig() != null) {
            thriftjsoaProperties.getProxy().getThreadedSelectorServerConfig().setExecutorService(tjExecutorService.getExecutorService());
            thriftjsoaProperties.getProxy().setServerConfig(thriftjsoaProperties.getProxy().getThreadedSelectorServerConfig());

        } else if (thriftjsoaProperties.getProxy().getNonblockingServerConfig() != null) {
            thriftjsoaProperties.getProxy().setServerConfig(thriftjsoaProperties.getProxy().getNonblockingServerConfig());

        } else if (thriftjsoaProperties.getProxy().getThreadPoolServerConfig() != null) {
            thriftjsoaProperties.getProxy().setServerConfig(thriftjsoaProperties.getProxy().getThreadPoolServerConfig());

        } else if (thriftjsoaProperties.getProxy().getSimpleServerConfig() != null) {
            thriftjsoaProperties.getProxy().setServerConfig(thriftjsoaProperties.getProxy().getSimpleServerConfig());
        }

        ThriftjsoaProxyRunner runner = new ThriftjsoaProxyRunner(new ThriftJsoaProxy(thriftjsoaProperties.getProxy()));
        return runner;
    }

    /**
     * 客户端 ThriftJsoaSessionFactory bean
     */
    @Bean
    @ConditionalOnMissingBean(ThriftJsoaSessionFactory.class)
    @ConditionalOnProperty(value = ThriftjsoaConstant.THRIFTJSOA_CLIENT_PREFIX)
    public ThriftJsoaSessionFactory thriftJsoaSessionFactory() throws Exception {
        if (thriftjsoaProperties.getServer() != null) {
            thriftjsoaProperties.getClient().setInTjServer(true);
        }

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
                if (tjClient != null) {
                    tjClient.value();
                }

                thriftjsoaProperties.getClient().getList();
            }
        }

        ThriftJsoaSessionFactory sessionFactory = new ThriftJsoaSessionFactory(thriftjsoaProperties.getClient());

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
