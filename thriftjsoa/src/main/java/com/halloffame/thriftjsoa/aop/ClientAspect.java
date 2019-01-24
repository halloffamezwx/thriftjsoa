package com.halloffame.thriftjsoa.aop;

import com.halloffame.thriftjsoa.common.ClientConfigCollect;
import com.halloffame.thriftjsoa.common.CommonClient;
import com.halloffame.thriftjsoa.config.ClientConfig;
import org.apache.thrift.TServiceClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<Class<? extends TServiceClient>, TServiceClient> tServiceClientMap = new HashMap<>();

        try {
            //根据注解配置的客户端class数组创建相应的客户端
            for (Class<? extends TServiceClient> classIt : clientAnnotation.clientClassArr()) {
                ClientConfig clientConfig = clientConfigCollect.getClientConfigClassMap().get(classIt);
                TServiceClient tServiceClient = CommonClient.createClient(classIt, clientConfig);
                tServiceClientMap.put(classIt, tServiceClient);
            }
            //把创建的客户端放到ThreadLocal里，供方法内部取得对应的客户端进行业务逻辑调用
            CommonClient.tServiceClientMapThreadLocal.set(tServiceClientMap);

            return pjp.proceed();
        } finally { //释放客户端占用的资源
            CommonClient.tServiceClientMapThreadLocal.remove();
            for (TServiceClient tServiceClient : tServiceClientMap.values()) {
                tServiceClient.getInputProtocol().getTransport().close();
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
        Map<Class<? extends TServiceClient>, ClientConfig> clientConfigClassMap = clientConfigMap.entrySet().stream()
                .map(x -> x.getValue()).collect(Collectors.toMap(ClientConfig::getClazz, x -> x, (oldValue, newValue) -> oldValue, HashMap::new));

        ClientConfigCollect clientConfigCollect = new ClientConfigCollect();
        clientConfigCollect.setClientConfigClassMap(clientConfigClassMap);
        return clientConfigCollect;
    }

}
