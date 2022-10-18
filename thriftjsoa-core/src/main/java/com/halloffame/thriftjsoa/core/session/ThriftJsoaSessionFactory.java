package com.halloffame.thriftjsoa.core.session;

import com.halloffame.thriftjsoa.core.annotation.TjLog;
import com.halloffame.thriftjsoa.core.annotation.TjOneway;
import com.halloffame.thriftjsoa.core.base.TProtocolWrap;
import com.halloffame.thriftjsoa.core.base.TjApplicationException;
import com.halloffame.thriftjsoa.core.base.TjProtocol;
import com.halloffame.thriftjsoa.core.common.CommonClient;
import com.halloffame.thriftjsoa.core.common.CreateLoadBalanceResult;
import com.halloffame.thriftjsoa.core.config.client.LoadBalanceClientConfig;
import com.halloffame.thriftjsoa.core.config.client.ThriftJsoaClientConfig;
import com.halloffame.thriftjsoa.core.config.common.ClientClassConfig;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;

/**
 * 类似SqlSessionFactory
 * @author zhuwx
 */
@Slf4j
public class ThriftJsoaSessionFactory {

    public ThriftJsoaSessionFactory() {
    }

    public ThriftJsoaSessionFactory(ThriftJsoaClientConfig client) throws Exception {
        ThriftJsoaSessionData.IN_TJ_SERVER = client.isInTjServer();

        for (LoadBalanceClientConfig it : client.getList()) {
            it.setInTjServer(client.isInTjServer());

            CreateLoadBalanceResult createLoadBalanceResult = CommonClient.createLoadBalance(it);
            for (ClientClassConfig itClazz : it.getClazzs()) {
                //todo 优雅关机 zkCf.close();
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
        Object newProxyInstance;

        if ("cglib".equals(System.getProperty("thriftjsoa.session.client.proxy"))) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(type);
            enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> invokeMethod(method, args, type));
            newProxyInstance = enhancer.create();
        } else {
            newProxyInstance = Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> invokeMethod(method, args, type));
        }

        ThriftJsoaSessionData.CLIENT_MAP.put(type, newProxyInstance);
    }

    private Object invokeMethod(Method method, Object[] args, Class<?> type) throws Throwable {
        //如果传进来是一个已实现的具体类
        //if (Object.class.equals(method.getDeclaringClass())) {
        //    return method.invoke(this, args);
        //} else if (method.getDeclaringClass().isInterface()) { //如果传进来的是一个接口
        //}

        if (method.isDefault()) {
            if ("close".equals(method.getName())) {
                ThriftJsoaSessionData.SESSION_TL.get().close(type, true);
                return null;
            }
        }

        Object[] result = null;
        Throwable exception = null;
        int exceptionType = -1;
        TjLog tjLog = null;
        TMessage tMessage = null;
        TMessage inMsg = null;
        ThriftJsoaSession session = null;
        try {
            session = ThriftJsoaSessionData.SESSION_TL.get();
            if (session == null) {
                //session = new ThriftJsoaSession();
                session = openSession(ThriftJsoaSessionData.SESSION_AUTO_CLOSE_MAP_TL.get(), ThriftJsoaSessionData.SESSION_AUTO_CLOSE.get());
            }

            int seqId = session.getSeqId(type);
            tMessage = new TMessage(method.getName(), TMessageType.CALL, seqId);
            tjLog = method.getAnnotation(TjLog.class);
            if (tjLog == null || tjLog.clientIn()) {
                log.info("请求开始: reqHead={}, args={}", tMessage, Arrays.toString(args));
            }

            TProtocolWrap tProtocolWrap = session.getLoadBalanceBean(type).getProtocolWrap();
            TjProtocol inTjProtocol = tProtocolWrap.getInTProtocolMap().get(type);
            TjProtocol outTjProtocol = tProtocolWrap.getOutTProtocolMap().get(type);
            outTjProtocol.writeMessageBegin(tMessage);

            Parameter[] ps = method.getParameters();
            String[] argNames = new String[ps.length];
            Type[] argTypes = new Type[ps.length];
            Class<?>[] argClazzs = new Class<?>[ps.length];
            for (int i = 0; i < ps.length; i++) {
                argNames[i] = ps[i].getName();
                argTypes[i] = ps[i].getParameterizedType();
                argClazzs[i] = ps[i].getType();
                //ps[i].getAnnotations();
            }

            outTjProtocol.writeObjects(args, method.getName() + "_args", argNames, argTypes, argClazzs,
                    method.getParameterAnnotations(), false);

            outTjProtocol.writeMessageEnd();
            outTjProtocol.getTransport().flush();

            if (!method.isAnnotationPresent(TjOneway.class)) {
                inMsg = inTjProtocol.readMessageBegin();
                if (inMsg.type == TMessageType.EXCEPTION) {
                    TApplicationException x = TApplicationException.read(inTjProtocol);
                    inTjProtocol.readMessageEnd();
                    throw x;
                } else if (inMsg.seqid != seqId) {
                    throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, method.getName() + " failed: out of sequence response");
                } else {
                    Type[] exceptionTypes = method.getGenericExceptionTypes();
                    Class<?>[] exceptionClazzs = method.getExceptionTypes();
                    AnnotatedType[] exceptionAnnotatedTypes = method.getAnnotatedExceptionTypes();

                    Type[] types = new Type[1 + exceptionTypes.length];
                    Class<?>[] clazzs = new Class[1 + exceptionClazzs.length];
                    String[] names = new String[clazzs.length];
                    Annotation[][] annss = new Annotation[1 + exceptionAnnotatedTypes.length][];

                    types[0] = method.getGenericReturnType();
                    clazzs[0] = method.getReturnType();
                    names[0] = "success";
                    annss[0] = method.getAnnotatedReturnType().getAnnotations();

                    System.arraycopy(exceptionTypes, 0, types, 1, exceptionTypes.length);
                    //System.arraycopy(exceptionClazzs, 0, clazzs, 1, exceptionClazzs.length);
                    for (int i = 0; i < exceptionClazzs.length; i++) {
                        clazzs[i + 1] = exceptionClazzs[i];
                        names[i + 1] = exceptionClazzs[i].getSimpleName();
                    }

                    for (int i = 0; i < exceptionAnnotatedTypes.length; i++) {
                        annss[i + 1] = exceptionAnnotatedTypes[i].getAnnotations();
                    }

                    result = inTjProtocol.readObjects(method.getName() + "_result", types, clazzs, annss, names);
                    inTjProtocol.readMessageEnd();

                    for (int i = 1; i < result.length; i++) {
                        if (result[i] != null) {
                            throw (Throwable) result[i];
                        }
                    }
                    return result[0];
                }
            }
        } catch (TjApplicationException x) {
            exception = x;
            exceptionType = x.getUnderException().getType();
            throw x;
        } catch (TApplicationException x) {
            exception = x;
            exceptionType = x.getType();
            throw x;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            if (session != null) {
                session.close(type, false);
            }
            if (tjLog == null || tjLog.clientOut()) {
                log.info("请求结束: reqHead={}, respHead={}, args={}, result={}, exception={}, exceptionType={}",
                        tMessage, inMsg, args, result, exception, exceptionType);
            }
        }

        return null;
    }

    /**
     * 打开会话
     */
    public ThriftJsoaSession openSession() {
        return this.openSession(null, null);
    }

    /**
     * 打开会话
     */
    public ThriftJsoaSession openSession(Map<Class<?>, Boolean> autoCloseMap, Boolean autoClose) {
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
