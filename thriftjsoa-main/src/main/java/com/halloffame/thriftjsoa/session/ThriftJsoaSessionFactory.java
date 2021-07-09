package com.halloffame.thriftjsoa.session;

import com.halloffame.thriftjsoa.common.CommonClient;
import com.halloffame.thriftjsoa.common.CreateLoadBalanceResult;
import com.halloffame.thriftjsoa.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.config.client.ThriftJsoaClientConfig;
import com.halloffame.thriftjsoa.config.common.ClientClassConfig;
import com.halloffame.thriftjsoa.util.ThriftJsoaUtil;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;

import java.lang.reflect.*;
import java.util.Map;

/**
 * 类似SqlSessionFactory
 * @author zhuwx
 */
public class ThriftJsoaSessionFactory {

    public ThriftJsoaSessionFactory(ThriftJsoaClientConfig client) throws Exception {
        ThriftJsoaSessionData.IN_TJ_SERVER = client.isInTjServer();

        for (LoadBalanceClientConfig it : client.getList()) {
            it.setInTjServer(client.isInTjServer());

            CreateLoadBalanceResult createLoadBalanceResult = CommonClient.createLoadBalance(it);
            for (ClientClassConfig itClazz : it.getClazzs()) {
                if (itClazz.getName() != null) {
                    ThriftJsoaSessionData.LOAD_BALANCE_RESUlT_MAP.put(itClazz.getName(), createLoadBalanceResult);
                }
                if (itClazz.getSessionName() != null) {
                    ThriftJsoaSessionData.LOAD_BALANCE_RESUlT_MAP.put(itClazz.getSessionName(), createLoadBalanceResult);
                    addClient(itClazz.getSessionName());
                }
            }
        }
    }

    /**
     * 新增代理client对象
     */
    private void addClient(Class<?> type) {
        Object newProxyInstance = Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class[] { type },
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    //如果传进来是一个已实现的具体类
                    if (Object.class.equals(method.getDeclaringClass())) {
                        return method.invoke(this, args);

                    } else if (method.getDeclaringClass().isInterface()) { //如果传进来的是一个接口
                        ThriftJsoaSession session = ThriftJsoaSessionData.SESSION_TL.get();
                        if (session == null) {
                            session = new ThriftJsoaSession();
                        }

                        try {
                            TProtocol tProtocol = session.getLoadBalanceBean(type).getProtocolWrap().getTProtocolMap().get(type);
                            int seqId = session.getSeqId(type);
                            tProtocol.writeMessageBegin(new TMessage(method.getName(), TMessageType.CALL, seqId));

                            Parameter[] ps = method.getParameters();
                            String[] argNames = new String[ps.length];
                            Type[] argTypes = new Type[argNames.length];
                            for (int i = 0; i < ps.length; i++) {
                                argNames[i] = ps[i].getName();
                                argTypes[i] = ps[i].getParameterizedType();
                            }
                            ThriftJsoaUtil.writeData(tProtocol, args, method.getName() + "_args", argNames, argTypes);

                            tProtocol.writeMessageEnd();
                            tProtocol.getTransport().flush();

                            TMessage inMsg = tProtocol.readMessageBegin();
                            if (inMsg.type == TMessageType.EXCEPTION) {
                                TApplicationException x = TApplicationException.read(tProtocol);
                                tProtocol.readMessageEnd();
                                throw x;
                            } else if (inMsg.seqid != seqId) {
                                throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, method.getName() + " failed: out of sequence response");
                            } else {
                                Type[] types = {method.getGenericReturnType()};
                                Object[] result = ThriftJsoaUtil.readData(tProtocol, types);
                                tProtocol.readMessageEnd();

                                if (result != null && result.length > 0) {
                                    return result[0];
                                }
                            }
                        } finally {
                            session.close(type);
                        }
                    }
                    return null;
                }
            }
        );

        ThriftJsoaSessionData.CLIENT_MAP.put(type, newProxyInstance);
    }

    /**
     * 打开会话
     */
    public ThriftJsoaSession openSession() {
        return this.openSession(null, true);
    }

    /**
     * 打开会话
     */
    public ThriftJsoaSession openSession(Map<Class<?>, Boolean> autoCloseMap, boolean autoClose) {
        ThriftJsoaSession session = new ThriftJsoaSession(autoCloseMap, autoClose);
        ThriftJsoaSessionData.SESSION_TL.set(session);
        return session;
    }

    /**
     * 获取生成的代理client对象map
     */
    public Map<Class<?>, Object> getClientMap() {
        return ThriftJsoaSessionData.CLIENT_MAP;
    }
}
