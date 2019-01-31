package com.halloffame.thriftjsoa.common;

/**
 * 通信协议
 */
public enum ProtocolType {
    BINARY("binary", "二进制编码格式进行数据传输"),
    COMPACT("compact", "高效率的，密集的二进制编码格式进行数据传输"),
    JSON("json", "使用JSON的数据编码协议进行数据传输");

    private String value;
    private String desc;

    ProtocolType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

}
