package com.halloffame.thriftjsoa.core.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TjFieldId {

    /**
     * 字段id编号，为0无效
     */
    int value() default 0;
}
