package com.halloffame.thriftjsoa.session;

import com.halloffame.thriftjsoa.common.CreateLoadBalanceResult;

import java.util.HashMap;
import java.util.Map;

/**
 * ThriftJsoaSession数据
 * @author zhuwx
 */
public class ThriftJsoaSessionData {

    /**
     * 是否在ThriftJsoa服务里发起连接请求，涉及traceId的消息头协议解析处理等
     */
    public static boolean IN_TJ_SERVER = false;

    /**
     * 客户端map，key为客户端的class
     */
    public static final Map<Class<?>, CreateLoadBalanceResult> LOAD_BALANCE_RESUlT_MAP = new HashMap<>();

    /**
     * 存放生成的代理client对象
     */
    public static final Map<Class<?>, Object> CLIENT_MAP = new HashMap<>();

    /**
     * ThriftJsoaSession ThreadLocal变量
     */
    public static final ThreadLocal<ThriftJsoaSession> SESSION_TL = new ThreadLocal<>();
}
