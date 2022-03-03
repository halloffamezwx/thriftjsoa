package com.halloffame.thriftjsoa.boot.annotation;

import com.halloffame.thriftjsoa.boot.aspect.ThriftjsoaSessionAspect;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 开启Thriftjsoa会话管理切面注解
 * @author zhuwx
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ThriftjsoaSessionAspect.class)
public @interface EnableTjSessionManagement {

}
