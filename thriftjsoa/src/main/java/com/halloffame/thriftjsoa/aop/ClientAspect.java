package com.halloffame.thriftjsoa.aop;

import com.halloffame.thriftjsoa.annotation.ClientAnnotation;
import com.halloffame.thriftjsoa.common.CommonClient;
import com.halloffame.thriftjsoa.config.ClientCollectConfig;
import com.halloffame.thriftjsoa.config.ClientConfig;
import org.apache.thrift.TServiceClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 切面
 */
@Aspect
@Component
@Order(1)
public class ClientAspect {
    @Autowired
    private ClientCollectConfig clientCollectConfig;

    @Around("@annotation(clientAnnotation)")
    public Object initClient(ProceedingJoinPoint pjp, ClientAnnotation clientAnnotation) throws Throwable {

        Map<Class<? extends TServiceClient>, TServiceClient> tServiceClientMap = new HashMap<>();

        try {
            Class<? extends TServiceClient>[] clientClassArr = clientAnnotation.clientClassArr();
            for (Class<? extends TServiceClient> classIt : clientClassArr) {
                ClientConfig clientConfig = clientCollectConfig.getClientConfig(classIt);
                TServiceClient tServiceClient = CommonClient.createClient(classIt, clientConfig);
                tServiceClientMap.put(classIt, tServiceClient);
            }
            CommonClient.tServiceClientMapThreadLocal.set(tServiceClientMap);

            Object result = pjp.proceed();
            return result;
        } finally {
            CommonClient.tServiceClientMapThreadLocal.remove();
            for (TServiceClient tServiceClient : tServiceClientMap.values()) {
                tServiceClient.getInputProtocol().getTransport().close();
            }
        }
    }

}
