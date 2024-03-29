package com.halloffame.thriftjsoa.boot.aspect;

import com.halloffame.thriftjsoa.boot.annotation.TjSession;
import com.halloffame.thriftjsoa.core.session.ThriftJsoaSession;
import com.halloffame.thriftjsoa.core.session.ThriftJsoaSessionFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Thriftjsoa会话切面（自动创建和回收资源）
 * @author zhuwx
 */
@Aspect
//@Order(1)
public class ThriftjsoaSessionAspect {

    @Autowired
    private ThriftJsoaSessionFactory sessionFactory;

    /**
     * 环绕拦截TjSession注解的方法
     */
    @Around("@annotation(tjSession)")
    public Object openTjSession(ProceedingJoinPoint pjp, TjSession tjSession) throws Throwable {
        Object result;

        Map<Class<?>, Boolean> autoCloseMap = new HashMap<>();
        for (Class<?> it : tjSession.manualCloseClassArr()) {
            autoCloseMap.put(it, false);
        }
        for (Class<?> it : tjSession.autoCloseClassArr()) {
            autoCloseMap.put(it, true);
        }

        try (ThriftJsoaSession session = sessionFactory.openSession(autoCloseMap, tjSession.autoClose())) {
            result = pjp.proceed();
        }
        return result;
    }

}
