package com.halloffame.thriftjsoa.base;

import com.halloffame.thriftjsoa.constant.MsgCode;
import lombok.Getter;

/**
 * ThriftJsoa异常
 * @author zhuwx
 */
@Getter
public class ThriftJsoaException extends RuntimeException {

    /**
     * 消息内容
     */
    private String msg;

    /**
     * 消息编码
     */
    private MsgCode msgCode;

    public ThriftJsoaException(MsgCode msgCode, Object... args) {
        super(String.format(msgCode.getMsg(), args));
        this.msgCode = msgCode;
        this.msg = super.getMessage();
    }

    public ThriftJsoaException(Throwable cause) {
        super(cause);
    }
}
