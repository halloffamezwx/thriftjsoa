package com.halloffame.thriftjsoa.common;

/**
 * 消息编码
 */
public enum MsgCode {
    THRIFTJSOA_EXCEPTION(500, "thriftjsoa exception：%s");

    private Integer code; //编码
    private String msg; //消息

    MsgCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
