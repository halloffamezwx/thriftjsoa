package com.halloffame.thriftjsoa.core.session;

import com.halloffame.thriftjsoa.core.base.TProtocolWrap;
import com.halloffame.thriftjsoa.core.base.ThriftJsoaProtocol;
import com.halloffame.thriftjsoa.core.base.TjApplicationException;
import com.halloffame.thriftjsoa.core.common.CreateLoadBalanceResult;
import com.halloffame.thriftjsoa.core.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.core.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.core.loadbalance.LoadBalanceBean;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.MDC;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 类似SqlSession
 * @author zhuwx
 */
public class ThriftJsoaSession implements Closeable {

    /**
     * 客户端class对应的autoClose map，没有就默认autoClose变量的值
     */
    private Map<Class<?>, Boolean> autoCloseMap;

    /**
     * 是否自动关闭释放客户端连接资源等，类似autoCommit
     */
    private boolean autoClose = true;

    /**
     * 客户端class对应的负载均衡对象map
     */
    private Map<Class<?>, LoadBalanceBean> loadBalanceBeanMap = new HashMap<>();

    /**
     * seqId Map
     */
    private Map<Class<?>, Integer> seqIdMap = new HashMap<>();

    public void setClientAutoClose(Class<?> clazz, Boolean autoClose) {
        if (autoCloseMap == null) {
            autoCloseMap = new HashMap<>();
        }
        autoCloseMap.put(clazz, autoClose);
    }

    public ThriftJsoaSession(Map<Class<?>, Boolean> autoCloseMap, Boolean autoClose) {
        this.autoCloseMap = autoCloseMap;
        if (autoClose != null) {
            this.autoClose = autoClose;
        }
    }

    /**
     * 根据客户端class获取负载均衡对象
     */
    public LoadBalanceBean getLoadBalanceBean(Class<?> type) {
        LoadBalanceBean loadBalanceBean = loadBalanceBeanMap.get(type);
        if (loadBalanceBean == null) {
            CreateLoadBalanceResult loadBalanceResult = ThriftJsoaSessionData.LOAD_BALANCE_RESUlT_MAP.get(type);
            LoadBalanceClientConfig loadBalanceClientConfig = loadBalanceResult.getLoadBalanceClientConfig();
            loadBalanceBean = loadBalanceResult.getLoadBalance().getLoadBalanceBean();

            for (ClientClassConfig clazzIt : loadBalanceClientConfig.getClazzs()) {
                if (clazzIt.getName() != null) {
                    loadBalanceBeanMap.put(clazzIt.getName(), loadBalanceBean);
                }
                if (clazzIt.getSessionName() != null) {
                    loadBalanceBeanMap.put(clazzIt.getSessionName(), loadBalanceBean);
                }
            }
        }

        return loadBalanceBean;
    }

    /**
     * 创建客户端
     */
    public <T extends TServiceClient> T createClient(Class<T> type) {
        LoadBalanceBean LoadBalanceBean = getLoadBalanceBean(type);
        TProtocolWrap tProtocolWrap = LoadBalanceBean.getProtocolWrap();
        TProtocol inTProtocol = tProtocolWrap.getInTProtocolMap().get(type);
        TProtocol outTProtocol = tProtocolWrap.getOutTProtocolMap().get(type);

        try {
            Constructor<T> constructor = type.getConstructor(TProtocol.class, TProtocol.class);
            T t = constructor.newInstance(inTProtocol, outTProtocol);
            return t;
        } catch (Exception e) {
            throw new TjApplicationException(TjApplicationException.CREATE_CLIENT_ERROR, e.getMessage(), e);
        }
    }

    /**
     * 根据客户端class关闭连接资源等
     */
    public void close(Class<?> type) {
        close(type, true);
    }

    /**
     * 根据客户端class关闭连接资源等
     */
    public void close(Class<?> type, boolean isForce) {
        //if (!ThriftJsoaSessionData.IN_TJ_SERVER && ThriftJsoaSessionData.SESSION_TL.get() != this) {
        //    MDC.remove(ThriftJsoaProtocol.TRACE_KEY);
        //    MDC.remove(ThriftJsoaProtocol.APP_KEY);
        //}

        if (isForce || isAutoClose(type)) {
            LoadBalanceBean loadBalanceBean = loadBalanceBeanMap.get(type);
            TProtocolWrap tProtocolWrap = loadBalanceBean.getProtocolWrap();
            loadBalanceBean.getConnectionFactory().releaseConnection(tProtocolWrap.getTTransport());
            for (Class<?> classIt : tProtocolWrap.getInTProtocolMap().keySet()) {
                loadBalanceBeanMap.remove(classIt);
            }
        }
    }

    /**
     * 是否自动关闭连接资源等
     */
    private boolean isAutoClose(Class<?> type) {
        Boolean autoCloseObj = null;
        if (autoCloseMap != null) {
            autoCloseObj = autoCloseMap.get(type);
        }
        if (autoCloseObj != null) {
            return autoCloseObj;
        } else {
            return autoClose;
        }
    }

    @Override
    public void close() {
        if (!ThriftJsoaSessionData.IN_TJ_SERVER) {
            MDC.remove(ThriftJsoaProtocol.TRACE_KEY);
            MDC.remove(ThriftJsoaProtocol.APP_KEY);
        }
        ThriftJsoaSessionData.SESSION_TL.remove();
        ThriftJsoaSessionData.SESSION_AUTO_CLOSE_MAP_TL.remove();
        ThriftJsoaSessionData.SESSION_AUTO_CLOSE.remove();

        for (LoadBalanceBean it : new HashSet<>(loadBalanceBeanMap.values())) {
            it.getConnectionFactory().releaseConnection(it.getProtocolWrap().getTTransport());
        }
    }

    /**
     * 获取client对象
     */
    public <T> T getClient(Class<T> type) {
        return (T) ThriftJsoaSessionData.CLIENT_MAP.get(type);
    }

    /**
     * 获取seqId
     */
    public int getSeqId(Class<?> type) {
        Integer seqId = seqIdMap.get(type);
        if (seqId == null) {
            seqId = 0;
        }
        ++seqId;
        seqIdMap.put(type, seqId);
        return seqId;
    }

}
