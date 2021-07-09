package com.halloffame.thriftjsoa.constant;

import lombok.Getter;
import lombok.ToString;

/**
 * 传输协议
 * @author zhuwx
 */
@Getter
@ToString
public enum ProtocolType {

    BINARY("binary", "二进制编码格式进行数据传输"),
    COMPACT("compact", "高效率的，密集的二进制编码格式进行数据传输"),
    JSON("json", "使用JSON的数据编码协议进行数据传输");

    /**
     * 编码
     */
    private String code;

    /**
     * 描述
     */
    private String desc;

    ProtocolType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProtocolType getByCode(String code) {
        if (code == null || "".equals(code.trim())) {
            return null;
        }
        for (ProtocolType it : ProtocolType.values()) {
            if (it.getCode().equals(code)) {
                return it;
            }
        }
        return null;
    }
}
