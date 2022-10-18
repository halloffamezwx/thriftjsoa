package com.halloffame.thriftjsoa.core.constant;

import lombok.Getter;
import lombok.ToString;

/**
 * 注册中心类型
 * @author zhuwx
 */
@Getter
@ToString
public enum RegisterType {

    ZOOKEEPER("zookeeper", "zookeeper");

    /**
     * 编码
     */
    private String code;

    /**
     * 描述
     */
    private String desc;

    RegisterType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
