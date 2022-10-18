package com.halloffame.thriftjsoa.core.constant;

import lombok.Getter;
import lombok.ToString;

/**
 * 负载均衡类型
 * @author zhuwx
 */
@Getter
@ToString
public enum LoadBalanceType {

    LEAST_CONN("leastConn", "最小连接数"),

    POLLING("polling", "轮询"),

    RANDOM("random", "随机"),

    LEAST_CONN_WEIGHT("leastConnWeight", "最小连接数（加权）"),

    POLLING_WEIGHT("pollingWeight", "轮询（加权）"),

    RANDOM_WEIGHT("randomWeight", "随机（加权）");

    /**
     * 编码
     */
    private String code;

    /**
     * 描述
     */
    private String desc;

    LoadBalanceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static LoadBalanceType getByCode(String code) {
        if (code == null || "".equals(code.trim())) {
            return null;
        }
        for (LoadBalanceType it : LoadBalanceType.values()) {
            if (it.getCode().equals(code)) {
                return it;
            }
        }
        return null;
    }
}
