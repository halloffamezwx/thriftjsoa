package com.halloffame.thriftjsoa.boot.aspect;

import com.halloffame.thriftjsoa.boot.annotation.OpenThriftjsoaSession;
import com.halloffame.thriftjsoa.session.ThriftJsoaSession;
import com.halloffame.thriftjsoa.session.ThriftJsoaSessionFactory;
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
     * 环绕拦截OpenThriftjsoaSession注解的方法
     */
    @Around("@annotation(openThriftjsoaSession)")
    public Object openThriftjsoaSession(ProceedingJoinPoint pjp, OpenThriftjsoaSession openThriftjsoaSession) throws Throwable {
        Object result;

        Map<Class<?>, Boolean> autoCloseMap = new HashMap<>();
        for (Class<?> it : openThriftjsoaSession.manualCloseClassArr()) {
            autoCloseMap.put(it, false);
        }
        for (Class<?> it : openThriftjsoaSession.autoCloseClassArr()) {
            autoCloseMap.put(it, true);
        }

        try (ThriftJsoaSession session = sessionFactory.openSession(autoCloseMap, openThriftjsoaSession.autoClose())) {
            result = pjp.proceed();
        }
        return result;
    }

}
