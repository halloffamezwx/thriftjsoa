package com.halloffame.thriftjsoa.base;

import com.halloffame.thriftjsoa.common.MsgCode;

/**
 * ThirftJsoa异常
 */
public class ThirftJsoaException extends Exception {
    private String msg; //消息
    private MsgCode msgCode; //编码

    public ThirftJsoaException(MsgCode msgCode, Object... args) {
        super(String.format(msgCode.getMsg(), args));
        this.msgCode = msgCode;
        this.msg = String.format(msgCode.getMsg(), args);
    }

    public String getMsg() {
        return msg;
    }

    public MsgCode getMsgCode() {
        return msgCode;
    }
}
