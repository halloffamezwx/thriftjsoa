package com.halloffame.thriftjsoa.util;

import com.halloffame.thriftjsoa.base.ThriftJsoaProtocol;
import org.apache.thrift.TEnum;
import org.apache.thrift.protocol.*;
import org.slf4j.MDC;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * ThriftJsoa工具类
 * @author zhuwx
 */
public class ThriftJsoaUtil {

    /**
     * 获取上下文环境变量mdc里的traceId
     */
    public static String getTraceId() {
        return MDC.get(ThriftJsoaProtocol.TRACE_KEY);
    }

    /**
     * 获取上下文环境变量mdc里的appId
     */
    public static String getAppId() {
        return MDC.get(ThriftJsoaProtocol.APP_KEY);
    }

    /**
     * 生成uuid
     */
    public static String genUuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 发送数据
     */
    public static void writeData(TProtocol out, Object[] args, String tStructName, String[] argNames, Type[] argTypes) throws Exception {
        out.writeStructBegin(new TStruct(tStructName));

        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }
            int fieldId = i + 1;
            out.writeFieldBegin(new TField(argNames[i], getByteType(args[i].getClass(), args[i]), (short)fieldId));
            writeField(out, args[i], argTypes[i]);
            out.writeFieldEnd();
        }

        out.writeFieldStop();
        out.writeStructEnd();
    }

    /**
     * 发送字段数据
     */
    private static void writeField(TProtocol out, Object arg, Type type) throws Exception {
        Class clazz = Object.class; //基本类型信息
        Type[] types = {Object.class, Object.class}; //泛型类型的泛型参数
        if (type instanceof Class) {
            clazz = (Class) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            clazz = (Class) parameterizedType.getRawType();
            types = parameterizedType.getActualTypeArguments();
        }

        Class[] genericClassArr = {Object.class, Object.class};
        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof Class) {
                genericClassArr[i] = (Class) types[i];
            } else if (types[i] instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) types[i];
                genericClassArr[i] = (Class) parameterizedType.getRawType();
            }
        }

        if (arg instanceof Void) {
        } else if (arg instanceof Boolean) {
            out.writeBool((boolean) arg);
        } else if (arg instanceof Byte) {
            out.writeByte((byte) arg);
        } else if (arg instanceof Float || arg instanceof Double) {
            out.writeDouble((double) arg);
        } else if (arg instanceof Short) {
            out.writeI16((short) arg);
        } else if (arg instanceof Integer) {
            out.writeI32((int) arg);
        } else if (arg instanceof Long) {
            out.writeI64((long) arg);
        } else if (arg instanceof String || arg instanceof BigDecimal) {
            out.writeString(arg.toString());
        } else if (arg instanceof Map) {
            Map<Object, Object> map = (Map) arg;
            TMap tMap = new TMap(getByteType(genericClassArr[0], null), getByteType(genericClassArr[1], null), map.size());
            out.writeMapBegin(tMap);
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                writeField(out, entry.getKey(), types[0]);
                writeField(out, entry.getValue(), types[1]);
            }
            out.writeMapEnd();

        } else if (arg instanceof Set) {
            Set set = (Set) arg;
            out.writeSetBegin(new TSet(getByteType(genericClassArr[0], null), set.size()));
            for (Object obj : set) {
                writeField(out, obj, types[0]);
            }
            out.writeSetEnd();

        } else if (arg instanceof List || arg instanceof Array) {
            List list;
            if (arg instanceof Array) {
                list = new ArrayList<>(Arrays.asList(arg));
            } else {
                list = (List) arg;
            }
            out.writeListBegin(new TList(getByteType(genericClassArr[0], null), list.size()));
            for (Object o : list) {
                writeField(out, o, types[0]);
            }
            out.writeListEnd();

        } else if (arg instanceof TEnum || arg instanceof Enum) {
            if (arg instanceof TEnum) {
                TEnum tEnum = (TEnum) arg;
                out.writeI32(tEnum.getValue());
            } else {
                Enum enumObj = (Enum) arg;
                out.writeI32(enumObj.ordinal() + 1);
            }
        } else { //struct
            Field[] fields = getAllFields(clazz);
            String[] fieldNames = new String[fields.length];
            Type[] fieldTypes = new Type[fields.length];
            Object[] fieldValues = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                fieldValues[i] = fields[i].get(arg);
                fieldNames[i] = fields[i].getName();
                fieldTypes[i] = fields[i].getGenericType();
            }
            writeData(out, fieldValues, clazz.getSimpleName(), fieldNames, fieldTypes);
        }
    }

    /**
     * 读取数据
     */
    public static Object[] readData(TProtocol in, Type[] types) throws Exception {
        in.readStructBegin();
        Object[] result = new Object[types.length];

        TField tField;
        while (true) {
            tField = in.readFieldBegin();

            if (tField.type == TType.STOP) {
                break;
            }
            if (types.length >= tField.id) {
                int index = tField.id - 1;
                Object obj = readField(in, tField.type, types[index]);
                result[index] = obj;
            } else {
                TProtocolUtil.skip(in, tField.type);
            }

            in.readFieldEnd();
        }

        in.readStructEnd();
        return result;
    }

    /**
     * 读取字段数据
     */
    private static Object readField(TProtocol in, byte byteType, Type type) throws Exception {
        Object result = null;

        Class clazz = Object.class; //基本类型信息
        Type[] types = {Object.class, Object.class}; //泛型类型的泛型参数
        if (type instanceof Class) {
            clazz = (Class) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            clazz = (Class) parameterizedType.getRawType();
            types = parameterizedType.getActualTypeArguments();
        }

        switch (byteType) {
            case TType.VOID:
                result = null;
                TProtocolUtil.skip(in, byteType);
                break;
            case TType.BOOL:
                result = in.readBool();
                break;
            case TType.BYTE:
                result = in.readByte();
                break;
            case TType.I16:
                result = in.readI16();
                break;
            case TType.I32:
                int i32 = in.readI32();
                result = i32;

                if (clazz.isEnum()) {
                    boolean isTEnum = false;
                    List<Class<?>> interfaceClassList = getAllInterfaces(clazz);
                    for (Class<?> interfaceClass : interfaceClassList) {
                        if (TEnum.class.equals(interfaceClass)) {
                            isTEnum = true;
                            break;
                        }
                    }

                    if (isTEnum) {
                        for (Object obj : clazz.getEnumConstants()) {
                            Method method = clazz.getDeclaredMethod("getValue");
                            method.setAccessible(true);
                            int value = (int) method.invoke(obj);
                            if (value == i32) {
                                Enum<?> e = (Enum<?>) obj;
                                result = Enum.valueOf(clazz, e.name());
                                break;
                            }
                        }
                    } else {
                        for (Object obj : clazz.getEnumConstants()) {
                            Enum<?> e = (Enum<?>) obj;
                            if ((e.ordinal() + 1) == i32) {
                                result = Enum.valueOf(clazz, e.name());
                                break;
                            }
                        }
                    }
                }
                break;
            case TType.I64:
                result = in.readI64();
                break;
            case TType.DOUBLE:
                double d = in.readDouble();
                if (float.class.equals(clazz) || Float.class.equals(clazz)) {
                    result = (float) d;
                } else {
                    result = d;
                }
                break;
            case TType.STRING:
                String s = in.readString();
                if (BigDecimal.class.equals(clazz)) {
                    result = new BigDecimal(s);
                } else {
                    result = s;
                }
                break;
            case TType.STRUCT:
                Field[] fields = getAllFields(clazz);
                Type[] fieldTypes = new Type[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    fieldTypes[i] = fields[i].getGenericType();
                }
                Object[] objs = readData(in, fieldTypes);

                result = clazz.newInstance();
                for (int i = 0; i < fields.length; i++) {
                    fields[i].setAccessible(true);
                    fields[i].set(result, objs[i]);
                }
                break;
            case TType.MAP:
                TMap tMap = in.readMapBegin();
                Map<Object, Object> map = new HashMap<>(2 * tMap.size);
                for (int i = 0; i < tMap.size; i++) {
                    Object key = readField(in, tMap.keyType, types[0]);
                    Object value = readField(in, tMap.valueType, types[1]);
                    map.put(key, value);
                }
                in.readMapEnd();
                result = map;
                break;
            case TType.SET:
                TSet tSet = in.readSetBegin();
                Set<Object> set = new HashSet<>(2 * tSet.size);
                for (int i = 0; i < tSet.size; i++) {
                    Object value = readField(in, tSet.elemType, types[0]);
                    set.add(value);
                }
                in.readSetEnd();
                result = set;
                break;
            case TType.LIST:
                TList tList = in.readListBegin();
                Object[] objArr = new Object[tList.size];
                for (int i = 0; i < tList.size; i++) {
                    objArr[i] = readField(in, tList.elemType, types[0]);
                }
                in.readListEnd();
                if (clazz.isArray()) {
                    result = objArr;
                } else {
                    result = new ArrayList<>(Arrays.asList(objArr));
                }
                break;
            case TType.ENUM:
                //Enum类型在序列化传输时是个i32
                TProtocolUtil.skip(in, byteType);
                break;
            default:
                TProtocolUtil.skip(in, byteType);
        }

        return result;
    }

    /**
     * 获取字段类型
     */
    private static byte getByteType(Class<?> type, Object obj) {
        if (obj == null) {
            obj = new Object();
        }
        if (void.class.equals(type) || Void.class.equals(type) || obj instanceof Void) {
            return TType.VOID;
        } else if (boolean.class.equals(type) || Boolean.class.equals(type) || obj instanceof Boolean) {
            return TType.BOOL;
        } else if (byte.class.equals(type) || Byte.class.equals(type) || obj instanceof Byte) {
            return TType.BYTE;
        } else if (float.class.equals(type) || Float.class.equals(type) || obj instanceof Float) {
            return TType.DOUBLE;
        } else if (double.class.equals(type) || Double.class.equals(type) || obj instanceof Double) {
            return TType.DOUBLE;
        } else if (short.class.equals(type) || Short.class.equals(type) || obj instanceof Short) {
            return TType.I16;
        } else if (int.class.equals(type) || Integer.class.equals(type) || obj instanceof Integer) {
            return TType.I32;
        } else if (long.class.equals(type) || Long.class.equals(type) || obj instanceof Long) {
            return TType.I64;
        } else if (String.class.equals(type) || BigDecimal.class.equals(type) || obj instanceof String || obj instanceof BigDecimal) {
            return TType.STRING;
        } else if (Map.class.equals(type) || obj instanceof Map) {
            return TType.MAP;
        } else if (Set.class.equals(type) || obj instanceof Set) {
            return TType.SET;
        } else if (List.class.equals(type) || Array.class.equals(type) || obj instanceof List || obj instanceof Array) {
            return TType.LIST;
        } else if (Enum.class.equals(type) || obj instanceof Enum) { //|| obj instanceof TEnum
            return TType.I32; //TType.ENUM
        } else {
            return TType.STRUCT;
        }
    }

    /**
     * 获取所有字段
     */
    private static Field[] getAllFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        while (clazz != null){
            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        fieldList.toArray(fields);
        return fields;
    }

    /**
     * 获取所有接口
     */
    private static List<Class<?>> getAllInterfaces(Class<?> clazz) {
        if (clazz == null) {
            return new ArrayList<>();
        }
        List<Class<?>> interfaceList = new ArrayList<>();
        List<Class<?>> interfaceListTemp = new ArrayList<>(Arrays.asList(clazz.getInterfaces()));
        interfaceList.addAll(interfaceListTemp);

        for (Class<?> it : interfaceListTemp) {
            interfaceList.addAll(getAllInterfaces(it));
        }
        interfaceList.addAll(getAllInterfaces(clazz.getSuperclass()));

        return interfaceList;
    }

    /**
     * 获取所有方法
     */
    public static List<Method> getAllMethod(Class<?> clazz) {
        List<Method> methodList = new ArrayList<>();
        while (clazz != null){
            methodList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredMethods())));
            clazz = clazz.getSuperclass();
        }
        return methodList;
    }
}
