package com.halloffame.thriftjsoa.aop;

import com.halloffame.thriftjsoa.common.ClientConfigCollect;
import com.halloffame.thriftjsoa.common.CommonClient;
import com.halloffame.thriftjsoa.common.ConnectionPoolFactory;
import com.halloffame.thriftjsoa.config.ClientClassConfig;
import com.halloffame.thriftjsoa.config.ClientConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端开启切面（自动创建和回收资源）
 */
@Aspect
@Component
//@Order(1)
public class ClientAspect {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ClientConfigCollect clientConfigCollect;

    /**
     * 环绕拦截ClientAnnotation注解的方法
     */
    @Around("@annotation(clientAnnotation)")
    public Object autoClient(ProceedingJoinPoint pjp, ClientAnnotation clientAnnotation) throws Throwable {
        Map<TProtocol, ConnectionPoolFactory> tProtocolPoolMap = new HashMap<>(); //最后需要释放资源的TProtocol和配置的连接池（可能为null代表没有配置）

        try {
            Map<Class<? extends TServiceClient>, TServiceClient> tServiceClientMap = new HashMap<>(); //保存在ThreadLocal里的客户端
            Map<Class<? extends TServiceClient>, TProtocol> tServiceClientProtocolMap = new HashMap<>(); //如果是TMultiplexedProtocol，多个客户端对应同一个TProtocol

            //根据注解配置的客户端class数组创建相应的客户端
            for (Class<? extends TServiceClient> classIt : clientAnnotation.clientClassArr()) {
                ClientConfig clientConfig = clientConfigCollect.getClientConfigClassMap().get(classIt);
                TServiceClient tServiceClient = CommonClient.createClient(classIt, clientConfig, tServiceClientProtocolMap, tProtocolPoolMap);
                tServiceClientMap.put(classIt, tServiceClient);
            }
            //把创建的客户端放到ThreadLocal里，供方法内部取得对应的客户端进行业务逻辑调用
            CommonClient.T_SERVICE_CLIENT_MAP_THREAD_LOCAL.set(tServiceClientMap);

            return pjp.proceed();
        } finally { //释放客户端占用的资源
            CommonClient.T_SERVICE_CLIENT_MAP_THREAD_LOCAL.remove();

            for (Map.Entry<TProtocol, ConnectionPoolFactory> entry : tProtocolPoolMap.entrySet()) {
                TProtocol tProtocol = entry.getKey();
                ConnectionPoolFactory poolFactory = entry.getValue();
                if (poolFactory != null) { //该客户端有配置连接池
                    poolFactory.releaseConnection(tProtocol);
                } else {
                    tProtocol.getTransport().close();
                }
            }
        }
    }

    /**
     * 转换客户端配置格式map，key是客户端class，value是客户端配置
     */
    @Bean
    public ClientConfigCollect clientConfigCollect() {
        //取得spring配置文件里配置的客户端
        Map<String, ClientConfig> clientConfigMap = applicationContext.getBeansOfType(ClientConfig.class);
        //转换客户端配置格式，key改为客户端的class
        Map<Class<? extends TServiceClient>, ClientConfig> clientConfigClassMap = new HashMap<>();

        for (ClientConfig clientConfig : clientConfigMap.values()) {
            for (ClientClassConfig clientClassConfig : clientConfig.getClientClassConfigs()) {
                clientConfigClassMap.put(clientClassConfig.getClazz(), clientConfig);
            }
        }

        ClientConfigCollect clientConfigCollect = new ClientConfigCollect();
        clientConfigCollect.setClientConfigClassMap(clientConfigClassMap);

        return clientConfigCollect;
    }

}
