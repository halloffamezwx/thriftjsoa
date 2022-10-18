package com.halloffame.thriftjsoa.core.base;

import lombok.Getter;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

/**
 * Tj业务异常
 * @author zhuwx
 */
public class TjApplicationException extends RuntimeException {

    public static final int VALIDATE_ERROR = 11; //字段校验错误
    public static final int NO_AVAILABLE_SERVICE = 12; //没有可用服务
    public static final int CREATE_CLIENT_ERROR = 13; //创建客户端错误

    @Getter
    private TApplicationException underException;

    public TjApplicationException(TApplicationException underException) {
        this.underException = underException;
    }

    public TjApplicationException(int type, String message) {
        underException = new TApplicationException(type, message);
    }

    public TjApplicationException(int type, String message, Throwable cause) {
        super(message, cause);
        underException = new TApplicationException(type, message);
    }

    public static TjApplicationException read(TProtocol iprot) throws TException {
        TApplicationException underException = TApplicationException.read(iprot);
        return new TjApplicationException(underException);
    }

    public void write(TProtocol oprot) throws TException {
        underException.write(oprot);
    }
}
