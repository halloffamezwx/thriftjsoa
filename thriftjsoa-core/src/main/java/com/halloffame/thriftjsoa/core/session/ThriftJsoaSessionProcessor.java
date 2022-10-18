package com.halloffame.thriftjsoa.core.session;

import com.halloffame.thriftjsoa.core.annotation.TjLog;
import com.halloffame.thriftjsoa.core.annotation.TjOneway;
import com.halloffame.thriftjsoa.core.base.TjApplicationException;
import com.halloffame.thriftjsoa.core.base.TjProtocol;
import com.halloffame.thriftjsoa.core.base.TjProtocolUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TType;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;

/**
 * ThriftJsoa会话Processor
 * @author zhuwx
 */
@Slf4j
public class ThriftJsoaSessionProcessor<T extends BaseService<?, ?, ?>> implements TProcessor {

    /**
     * 业务实现类
     */
    @Getter
    private final T t;

    public ThriftJsoaSessionProcessor(T t) {
        this.t = t;
    }

    @Override
    public boolean process(TProtocol in, TProtocol out) throws TException {
        TjLog tjLog = null;
        TMessage tMessage = null;

        Object[] args = null;
        Object[] retFields = null;

        Throwable exception = null;
        int exceptionType = -1;

        TjProtocol tjProtocolIn = (TjProtocol) in;
        TjProtocol tjProtocolOut = (TjProtocol) out;

        TMessage inMsg = tjProtocolIn.readMessageBegin();
        log.debug("接收请求: reqHead={}", inMsg);
        try {
            Class<?> clazz = t.getClass();
            //List<Method> methods = ThriftJsoaUtil.getAllMethod(clazz);
            Method method = null;
            for (Method it : clazz.getMethods()) {
                if (it.getName().equals(inMsg.name)) { //需要判断方法重载的情况？原生thrift并不支持方法重载
                    method = it;
                    break;
                }
            }

            if (method == null) {
                TjProtocolUtil.skip(tjProtocolIn, TType.STRUCT);
                throw new TApplicationException(TApplicationException.UNKNOWN_METHOD, "Invalid method name: '" + inMsg.name + "'");
            }
            tjLog = method.getAnnotation(TjLog.class);

            try {
                Parameter[] ps = method.getParameters();
                String[] argNames = new String[ps.length];
                for (int i = 0; i < ps.length; i++) {
                    argNames[i] = ps[i].getName();
                }
                args = tjProtocolIn.readObjects(method.getName() + "_args", method.getGenericParameterTypes(),
                                    method.getParameterTypes(), method.getParameterAnnotations(), argNames);
            } catch (TjApplicationException x) {
                throw x;
            } catch (Exception e) {
                throw new TjApplicationException(TApplicationException.PROTOCOL_ERROR, e.getMessage(), e);
            } finally {
                if (tjLog == null || tjLog.serverIn()) {
                    log.info("接收请求: reqHead={}, args={}", inMsg, Arrays.toString(args));
                }
            }
            //tjProtocolIn.readMessageEnd();

            Object methodInvokeResult = null;
            Throwable methodInvokeRetException = null;
            int methodInvokeRetExceptionIndex = 0;
            Class<?>[] methodInvokeRetExceptionClazzs = method.getExceptionTypes();

            try {
                methodInvokeResult = method.invoke(t, args);
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getTargetException();

                if (targetException instanceof TjApplicationException) {
                    throw targetException;
                } else {
                    for (int i = 0; i < methodInvokeRetExceptionClazzs.length; i++) {
                        if (targetException.getClass().equals(methodInvokeRetExceptionClazzs[i])) {
                            methodInvokeRetExceptionIndex = i;
                            methodInvokeRetException = targetException;
                            break;
                        }
                    }

                    if (methodInvokeRetException == null) {
                        throw new TjApplicationException(TApplicationException.INTERNAL_ERROR, "Internal error processing " + inMsg.name, targetException);
                    }
                }
            } catch (Exception e) {
                throw new TjApplicationException(TApplicationException.INTERNAL_ERROR, "Internal error processing " + inMsg.name, e);
            }

            if (!method.isAnnotationPresent(TjOneway.class)) {
                Type[] methodInvokeRetExceptionTypes = method.getGenericExceptionTypes();
                AnnotatedType[] methodInvokeRetExceptionAnnotatedTypes = method.getAnnotatedExceptionTypes();
                int retFieldSize = 1 + methodInvokeRetExceptionClazzs.length;

                retFields = new Object[retFieldSize];
                String[] retFieldNames = new String[retFieldSize];
                Type[] retFieldTypes = new Type[1 + methodInvokeRetExceptionTypes.length];
                Class<?>[] retFieldClazzs = new Class[1 + methodInvokeRetExceptionClazzs.length];
                Annotation[][] retFieldAnnss = new Annotation[1 + methodInvokeRetExceptionAnnotatedTypes.length][];

                retFields[0] = methodInvokeResult;
                retFieldNames[0] = "success";
                retFieldTypes[0] = method.getGenericReturnType();
                retFieldClazzs[0] = method.getReturnType();
                retFieldAnnss[0] = method.getAnnotatedReturnType().getAnnotations();

                if (retFieldSize > 1) {
                    retFields[methodInvokeRetExceptionIndex + 1] = methodInvokeRetException;
                }
                for (int i = 0; i < methodInvokeRetExceptionClazzs.length; i++) {
                    retFieldNames[i + 1] = methodInvokeRetExceptionClazzs[i].getSimpleName();
                }
                System.arraycopy(methodInvokeRetExceptionTypes, 0, retFieldTypes, 1, methodInvokeRetExceptionTypes.length);
                System.arraycopy(methodInvokeRetExceptionClazzs, 0, retFieldClazzs, 1, methodInvokeRetExceptionClazzs.length);
                for (int i = 0; i < methodInvokeRetExceptionAnnotatedTypes.length; i++) {
                    retFieldAnnss[i + 1] = methodInvokeRetExceptionAnnotatedTypes[i].getAnnotations();
                }

                tMessage = new TMessage(inMsg.name, TMessageType.REPLY, inMsg.seqid);
                tjProtocolOut.writeMessageBegin(tMessage);
                tjProtocolOut.writeObjects(retFields, inMsg.name + "_result", retFieldNames, retFieldTypes, retFieldClazzs, retFieldAnnss, true);
                tjProtocolOut.writeMessageEnd();
                tjProtocolOut.getTransport().flush();
            }

        } catch (TjApplicationException tjae){
            log.error("", tjae);
            exception = tjae;
            exceptionType = tjae.getUnderException().getType();
            processTApplicationException(tjae.getUnderException(), inMsg, tjProtocolOut);

        } catch (TApplicationException x) {
            log.error("", x);
            exception = x;
            exceptionType = x.getType();
            processTApplicationException(x, inMsg, tjProtocolOut);

        } catch (Throwable e) {
            //log.error("", e);
            exception = e;
            throw new TException(e);
        } finally {
            tjProtocolIn.readMessageEnd();
            if (tjLog == null || tjLog.serverOut()) {
                log.info("返回请求: reqHead={}, respHead={}, args={}, result={}, exception={}, exceptionType={}",
                        inMsg, tMessage, args, retFields, exception, exceptionType);
            }
        }

        return true;
    }

    private void processTApplicationException(TApplicationException x, TMessage inMsg, TjProtocol tjProtocolOut) throws TException {
        tjProtocolOut.writeMessageBegin(new TMessage(inMsg.name, TMessageType.EXCEPTION, inMsg.seqid));
        x.write(tjProtocolOut);
        tjProtocolOut.writeMessageEnd();
        tjProtocolOut.getTransport().flush();
    }
}
