package com.halloffame.thriftjsoa.core.session;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.halloffame.thriftjsoa.core.common.CreateLoadBalanceResult;
import org.springframework.context.ApplicationContext;

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
    public static final ThreadLocal<ThriftJsoaSession> SESSION_TL = new ThreadLocal<>(); //new InheritableThreadLocal<>();
    public static final ThreadLocal<Map<Class<?>, Boolean>> SESSION_AUTO_CLOSE_MAP_TL = new ThreadLocal<>();
    public static final ThreadLocal<Boolean> SESSION_AUTO_CLOSE = new ThreadLocal<>();

    /**
     * mapper ThreadLocal变量
     */
    public static final ThreadLocal<Map<Class<? extends BaseMapper<?>>, BaseMapper>> MAPPER_TL = new ThreadLocal<>();

    /**
     * spring的ApplicationContext
     */
    public static ApplicationContext applicationContext;

    /**
     * 获取mapper对象
     */
    public static <E, M extends BaseMapper<E>> BaseMapper<E> getMapper(Class<M> mapperClass) {
        BaseMapper<E> baseMapper = null;
        if (applicationContext != null) {
            baseMapper = applicationContext.getBean(mapperClass);
        }

        if (baseMapper == null) {
            baseMapper = MAPPER_TL.get().get(mapperClass);
            //MAPPER_TL.remove();
        }
        return baseMapper;
    }

}
