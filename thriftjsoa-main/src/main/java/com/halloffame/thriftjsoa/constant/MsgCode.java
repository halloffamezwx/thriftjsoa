package com.halloffame.thriftjsoa.constant;

import lombok.Getter;
import lombok.ToString;

/**
 * 消息编码
 * @author zhuwx
 */
@Getter
@ToString
public enum MsgCode {

    THRIFTJSOA_EXCEPTION(500, "系统异常", "thriftJsoa exception：%s"),
    SSL_NOT_SUPPORT(501, "不支持SSL", "SSL is not supported over nonblocking servers!"),
    UNKNOWN_SERVER(502, "不支持的服务类型", "Unknown server type! %s"),
    UNKNOWN_PROTOCOL(503, "不支持的传输协议", "Unknown protocol type! %s"),
    UNKNOWN_TRANSPORT(504, "不支持的传输方式", "Unknown transport type! %s"),
    HTTPS_NOT_SUPPORT(505, "不支持HTTPS", "SSL is not supported over http."),
    UNKNOWN_LOAD_BALANCE(506, "不支持的负载均衡类型", "Unknown loadBalance type! %s"),
    ;

    /**
     * 编码
     */
    private int code;

    /**
     * 描述
     */
    private String desc;

    /**
     * 内容
     */
    private String msg;

    MsgCode(int code, String desc, String msg) {
        this.code = code;
        this.desc = desc;
        this.msg = msg;
    }

}
