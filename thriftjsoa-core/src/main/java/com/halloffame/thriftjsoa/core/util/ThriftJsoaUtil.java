package com.halloffame.thriftjsoa.core.util;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.halloffame.thriftjsoa.core.annotation.TjFieldId;
import com.halloffame.thriftjsoa.core.annotation.TjValidated;
import com.halloffame.thriftjsoa.core.base.ThriftJsoaProtocol;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.thrift.TEnum;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.slf4j.MDC;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ThriftJsoa工具类
 * @author zhuwx
 */
public class ThriftJsoaUtil {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 获取上下文环境变量mdc里的traceId
     */
    public static String getTraceId() {
        return MDC.get(ThriftJsoaProtocol.TRACE_KEY);
    }

    /**
     * 删除上下文环境变量mdc里的traceId
     */
    public static void removeTraceId() {
        MDC.remove(ThriftJsoaProtocol.TRACE_KEY);
    }

    /**
     * 获取上下文环境变量mdc里的appId
     */
    public static String getAppId() {
        return MDC.get(ThriftJsoaProtocol.APP_KEY);
    }

    /**
     * 删除上下文环境变量mdc里的appId
     */
    public static void removeAppId() {
        MDC.remove(ThriftJsoaProtocol.APP_KEY);
    }

    /**
     * 删除上下文环境变量mdc里的值
     */
    public static void removeMdc() {
        removeTraceId();
        removeAppId();
    }

    /**
     * 生成uuid
     */
    private static String genUuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 生成唯一id
     */
    public static String genStrId() {
        //return genUuid();
        return NanoIdUtils.randomNanoId();
    }

    @Data
    public static class FieldIdWrap {

        private int maxFieldId;

        private int[] fieldIds;
    }

    /**
     * 从注解中获取fieldId
     */
    public static FieldIdWrap getFieldId(Annotation[][] annss, String tStructName, String[] names) throws TException {
        FieldIdWrap result = new FieldIdWrap();

        Map<Integer, String> fieldIdMap = new HashMap<>();
        int maxFieldId = 0;
        int[] fieldIds = new int[annss.length];

        for (int i = 0; i < annss.length; i++) {
            for (Annotation ann : annss[i]) {
                if (ann instanceof TjFieldId) {
                    TjFieldId tjFieldId = (TjFieldId) ann;

                    if (tjFieldId.value() <= 0) {
                        throw new TException(tStructName + "的" + names[i] + "的字段编号值必须大于0");
                    }

                    String name = fieldIdMap.get(tjFieldId.value());
                    if (name != null) {
                        throw new TException(tStructName + "的" + names[i] + "的字段编号值和字段" + name + "的重复了");
                    }
                    fieldIdMap.put(tjFieldId.value(), names[i]);

                    fieldIds[i] = tjFieldId.value();
                    if (tjFieldId.value() > maxFieldId) {
                        maxFieldId = tjFieldId.value();
                    }
                }
            }
        }
        result.setMaxFieldId(maxFieldId);
        result.setFieldIds(fieldIds);

        return result;
    }

    /**
     * 发送数据
     */
    public static void writeData(TProtocol out, Object[] args, String tStructName, String[] argNames, Type[] argTypes,
                                 Class<?>[] argClazzs, int[] fieldIds, int maxFieldId, boolean isResult) throws Exception {
        out.writeStructBegin(new TStruct(tStructName));

        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }

            int fieldId = 0;
            if ( !(isResult && i == 0) ) {
                fieldId = fieldIds[i];

                if (fieldId <= 0) {
                    fieldId = (i + 1) + maxFieldId; //(++maxFieldId);
                }
            }

            out.writeFieldBegin(new TField(argNames[i], getByteType(argClazzs[i], args[i]), (short) fieldId));
            writeField(out, args[i], argTypes[i], argClazzs[i]);
            out.writeFieldEnd();
        }

        out.writeFieldStop();
        out.writeStructEnd();
    }

    /**
     * 发送字段数据
     */
    private static void writeField(TProtocol out, Object arg, Type type, Class<?> clazz) throws Exception {
        /* Class<?> clazz = arg.getClass(); //Object.class; //基本类型信息
        Type[] types = {Object.class, Object.class}; //泛型类型的泛型参数

        if (type instanceof Class) {
            clazz = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            clazz = (Class<?>) parameterizedType.getRawType();
            types = parameterizedType.getActualTypeArguments();
        }

        Class<?>[] clazzs = {Object.class, Object.class};
        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof Class) {
                clazzs[i] = (Class<?>) types[i];
            } else if (types[i] instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) types[i];
                clazzs[i] = (Class<?>) parameterizedType.getRawType();
            }
        } */

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
            Map<?, ?> map = (Map<?, ?>) arg;
            if (map.size() == 0) {
                //TMap tMap = new TMap(getByteType(clazzs[0], null), getByteType(clazzs[1], null), 0);
                TMap tMap = new TMap(TType.VOID, TType.VOID, 0);
                out.writeMapBegin(tMap);
            }

            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Class<?> keyClass = key.getClass();
                Object value = entry.getValue();
                Class<?> valueClass = value.getClass();
                if (i == 0) {
                    //TMap tMap = new TMap(getByteType(clazzs[0], key), getByteType(clazzs[1], value), map.size());
                    TMap tMap = new TMap(getByteType(keyClass, key), getByteType(valueClass, value), map.size());
                    out.writeMapBegin(tMap);
                }
                //writeField(out, key, types[0], clazzs[0]);
                //writeField(out, value, types[1], clazzs[1]);
                writeField(out, key, keyClass, keyClass);
                writeField(out, value, valueClass, valueClass);
                i++;
            }
            out.writeMapEnd();

        } else if (arg instanceof Set) {
            Set<?> set = (Set<?>) arg;
            if (set.size() == 0) {
                //out.writeSetBegin(new TSet(getByteType(clazzs[0], null), 0));
                out.writeSetBegin(new TSet(TType.VOID, 0));
            }

            int i = 0;
            for (Object obj : set) {
                Class<?> objClass = obj.getClass();
                if (i == 0) {
                    //out.writeSetBegin(new TSet(getByteType(clazzs[0], obj), set.size()));
                    out.writeSetBegin(new TSet(getByteType(objClass, obj), set.size()));
                }
                //writeField(out, obj, types[0], clazzs[0]);
                writeField(out, obj, objClass, objClass);
                i++;
            }
            out.writeSetEnd();

        } else if (arg instanceof List || arg instanceof Array) {
            List<?> list;
            if (arg instanceof Array) {
                list = Arrays.asList(arg);
            } else {
                list = (List<?>) arg;
            }
            if (list.size() == 0) {
                //out.writeListBegin(new TList(getByteType(clazzs[0], null), 0));
                out.writeListBegin(new TList(TType.VOID, 0));
            }

            int i = 0;
            for (Object o : list) {
                Class<?> oClass = o.getClass();
                if (i == 0) {
                    //out.writeListBegin(new TList(getByteType(clazzs[0], o), list.size()));
                    out.writeListBegin(new TList(getByteType(oClass, o), list.size()));
                }
                //writeField(out, o, types[0], clazzs[0]);
                writeField(out, o, oClass, oClass);
                i++;
            }
            out.writeListEnd();

        } else if (arg instanceof TEnum || arg instanceof Enum) {
            if (arg instanceof TEnum) {
                TEnum tEnum = (TEnum) arg;
                out.writeI32(tEnum.getValue());
            } else {
                Field[] fields = getAllFields(clazz);
                Enum<?> enumObj = (Enum<?>) arg;

                Map<Integer, String> fieldIdMap = new HashMap<>();
                int maxFieldId = 0;
                TjFieldId findTjFieldId = null;
                for (Field field : fields) {
                    if (!field.isEnumConstant()) {
                        continue;
                    }
                    TjFieldId tjFieldId = field.getAnnotation(TjFieldId.class);

                    if (tjFieldId != null) {
                        if (tjFieldId.value() <= 0) {
                            throw new TException(clazz.getSimpleName() + "的" + field.getName() + "的字段编号值必须大于0");
                        }

                        String fieldName = fieldIdMap.get(tjFieldId.value());
                        if (fieldName != null) {
                            throw new TException(clazz.getSimpleName() + "的" + field.getName() + "的字段编号值和字段" + fieldName + "的重复了");
                        }
                        fieldIdMap.put(tjFieldId.value(), field.getName());

                        if (tjFieldId.value() > maxFieldId) {
                            maxFieldId = tjFieldId.value();
                        }
                    }

                    //field.setAccessible(true);
                    //Enum<?> fieldEnum = (Enum<?>) field.get(arg);

                    if (field.getName().equals(enumObj.name())) {
                        findTjFieldId = tjFieldId;
                    }
                }

                int i32;
                if (findTjFieldId == null) {
                    i32 = (enumObj.ordinal() + 1) + maxFieldId;
                } else {
                    i32 = findTjFieldId.value();
                }
                out.writeI32(i32);
            }
        } else { //struct
            Field[] fields = getAllFields(clazz);
            String[] fieldNames = new String[fields.length];
            Type[] fieldTypes = new Type[fields.length];
            Class<?>[] fieldClazzs = new Class[fields.length];
            Object[] fieldValues = new Object[fields.length];
            Annotation[][] fieldAnnss = new Annotation[fields.length][];

            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                fieldValues[i] = fields[i].get(arg);
                fieldNames[i] = fields[i].getName();
                fieldTypes[i] = fields[i].getGenericType();
                fieldClazzs[i] = fields[i].getType();
                fieldAnnss[i] = fields[i].getAnnotations();
            }

            String tStructName = clazz.getSimpleName();
            ThriftJsoaUtil.FieldIdWrap fieldIdWrap = ThriftJsoaUtil.getFieldId(fieldAnnss, tStructName, fieldNames);
            int[] fieldIds = fieldIdWrap.getFieldIds();
            int maxFieldId = fieldIdWrap.getMaxFieldId();

            writeData(out, fieldValues, tStructName, fieldNames, fieldTypes, fieldClazzs, fieldIds, maxFieldId, false);
        }
    }

    /**
     * 读取数据
     */
    public static Object[] readData(TProtocol in, String tStructName, Type[] types, Class<?>[] clazzs, String[] names, int[] fieldIds, int maxFieldId) throws Exception {
        in.readStructBegin();
        Object[] result = new Object[types.length];

        TField tField;
        while (true) {
            tField = in.readFieldBegin();

            if (tField.type == TType.STOP) {
                break;
            }

            int index = 0;
            if (tField.id > 0) {
                boolean isFindFieldId = false;

                for (int i = 0; i < fieldIds.length; i++) {
                    if (tField.id == fieldIds[i]) {
                        isFindFieldId = true;
                        index = i;
                        break;
                    }
                }

                if (!isFindFieldId) {
                    index = (tField.id - 1) - maxFieldId;
                }
            }

            if (tField.id < 0 || index < 0 || (index + 1) > result.length) {
                TProtocolUtil.skip(in, tField.type);
            } else {
                result[index] = readField(in, tField.type, types[index], clazzs[index]);
            }

            in.readFieldEnd();
        }

        in.readStructEnd();
        return result;
    }

    /**
     * 读取字段数据
     */
    private static Object readField(TProtocol in, byte byteType, Type type, Class clazz) throws Exception {
        Object result = null;

        //Class clazz = Object.class; //基本类型信息
        Type[] types = {Object.class, Object.class}; //泛型类型的泛型参数
        if (type instanceof Class) {
            //clazz = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            //clazz = (Class<?>) parameterizedType.getRawType();
            types = parameterizedType.getActualTypeArguments();
        }

        Class<?>[] clazzs = {Object.class, Object.class};
        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof Class) {
                clazzs[i] = (Class<?>) types[i];
            } else if (types[i] instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) types[i];
                clazzs[i] = (Class<?>) parameterizedType.getRawType();
            }
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
                result = instanceI32(in.readI32(), clazz);
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
                Class<?>[] fieldClazzs = new Class[fields.length];
                Annotation[][] fieldAnnss = new Annotation[fields.length][];
                String[] fieldNames = new String[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    fieldTypes[i] = fields[i].getGenericType();
                    fieldClazzs[i] = fields[i].getType();
                    fieldAnnss[i] = fields[i].getAnnotations();
                    fieldNames[i] = fields[i].getName();
                }

                String tStructName = clazz.getSimpleName();
                ThriftJsoaUtil.FieldIdWrap fieldIdWrap = ThriftJsoaUtil.getFieldId(fieldAnnss, tStructName, fieldNames);
                int[] fieldIds = fieldIdWrap.getFieldIds();
                int maxFieldId = fieldIdWrap.getMaxFieldId();

                Object[] objs = readData(in, tStructName, fieldTypes, fieldClazzs, fieldNames, fieldIds, maxFieldId);

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
                    Object key = readField(in, tMap.keyType, types[0], clazzs[0]);
                    Object value = readField(in, tMap.valueType, types[1], clazzs[1]);
                    map.put(key, value);
                }
                in.readMapEnd();
                result = map;
                break;
            case TType.SET:
                TSet tSet = in.readSetBegin();
                Set<Object> set = new HashSet<>(2 * tSet.size);
                for (int i = 0; i < tSet.size; i++) {
                    Object value = readField(in, tSet.elemType, types[0], clazzs[0]);
                    set.add(value);
                }
                in.readSetEnd();
                result = set;
                break;
            case TType.LIST:
                TList tList = in.readListBegin();
                Object[] objArr = new Object[tList.size];
                for (int i = 0; i < tList.size; i++) {
                    objArr[i] = readField(in, tList.elemType, types[0], clazzs[0]);
                }
                in.readListEnd();
                if (clazz.isArray()) {
                    result = objArr;
                } else {
                    result = Arrays.asList(objArr);
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
        } else if (Map.class.isAssignableFrom(type) || Map.class.equals(type) || HashMap.class.equals(type) || TreeMap.class.equals(type) || obj instanceof Map) {
            return TType.MAP;
        } else if (Set.class.isAssignableFrom(type) || Set.class.equals(type) || HashSet.class.equals(type) || TreeSet.class.equals(type)  || obj instanceof Set) {
            return TType.SET;
        } else if (List.class.isAssignableFrom(type) || List.class.equals(type) || ArrayList.class.equals(type) || LinkedList.class.equals(type) || type.isArray() ||
                Array.class.equals(type) || obj instanceof List || obj instanceof Array) {
            return TType.LIST;
        } else if (type.isEnum() || Enum.class.equals(type) || obj instanceof Enum || obj instanceof TEnum) {
            return TType.I32; //TType.ENUM
        } else {
            return TType.STRUCT;
        }
    }

    /**
     * 获取所有字段
     */
    public static Field[] getAllFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        while (clazz != null){
            fieldList.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        fieldList.toArray(fields);
        return fields;
    }

    /**
     * 获取所有接口
     */
    public static List<Class<?>> getAllInterfaces(Class<?> clazz) {
        if (clazz == null) {
            return new ArrayList<>();
        }
        List<Class<?>> interfaceListTemp = Arrays.asList(clazz.getInterfaces());
        List<Class<?>> interfaceList = new ArrayList<>(interfaceListTemp);

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
            methodList.addAll(Arrays.asList(clazz.getDeclaredMethods()));
            clazz = clazz.getSuperclass();
        }
        return methodList;
    }

    /**
     * 校验参数
     */
    public static List<String> validate(Object[] args, Annotation[][] annss) {
        List<String> result = new ArrayList<>();
        if (annss == null) {
            return result;
        }

        for (int i = 0; i < args.length; i++) {
            for (Annotation ann : annss[i]) {
                if (ann instanceof TjValidated) {
                    TjValidated tjValidated = (TjValidated) ann;
                    List<String> msgs = validator.validate(args[i], tjValidated.value()).stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
                    result.addAll(msgs);
                }
            }
        }
        return result;
    }

    /**
     * 读写数据
     */
    public static void readWriteData(TProtocol in, TProtocol out) throws TException {

        TStruct tStruct = in.readStructBegin();
        out.writeStructBegin(tStruct);

        TField schemeField;
        while (true) {
            schemeField = in.readFieldBegin();

            if (schemeField.type == TType.STOP) {
                break;
            } else {
                out.writeFieldBegin(schemeField);
            }

            readWriteField(schemeField.type, in, out);

            in.readFieldEnd();
            out.writeFieldEnd();
        }
        out.writeFieldStop();

        in.readStructEnd();
        out.writeStructEnd();
    }

    /**
     * 读写字段数据
     */
    public static void readWriteField(byte fieldtype, TProtocol in, TProtocol out) throws TException {

        switch (fieldtype) {
            case TType.VOID:
                TProtocolUtil.skip(in, fieldtype);
                break;
            case TType.BOOL:
                out.writeBool(in.readBool());
                break;
            case TType.BYTE:
                out.writeByte(in.readByte());
                break;
            case TType.DOUBLE:
                out.writeDouble(in.readDouble());
                break;
            case TType.I16:
                out.writeI16(in.readI16());
                break;
            case TType.I32:
                out.writeI32(in.readI32());
                break;
            case TType.I64:
                out.writeI64(in.readI64());
                break;
            case TType.STRING:
                out.writeString(in.readString());
                break;
            case TType.STRUCT:
                readWriteData(in, out);
                break;
            case TType.MAP:
                /**
                 * readMapBegin返回的TMap对象有3个字段keyType，valueType，size，
                 * 就是map的key的类型，value的类型，map的大小，
                 * 从0到size循环按类型读取key和value就行了。
                 */
                TMap tMap = in.readMapBegin();
                out.writeMapBegin(tMap);
                for (int i = 0; i < tMap.size; i++) {
                    readWriteField(tMap.keyType, in, out);
                    readWriteField(tMap.valueType, in, out);
                }
                in.readMapEnd();
                out.writeMapEnd();
                break;
            case TType.SET:
                TSet tSet = in.readSetBegin();
                out.writeSetBegin(tSet);
                for (int i = 0; i < tSet.size; i++) {
                    readWriteField(tSet.elemType, in, out);
                }
                in.readSetEnd();
                out.writeSetEnd();
                break;
            case TType.LIST:
                TList tList = in.readListBegin();
                out.writeListBegin(tList);
                for (int i = 0; i < tList.size; i++) {
                    readWriteField(tList.elemType, in, out);
                }
                in.readListEnd();
                out.writeListEnd();
                break;
            case TType.ENUM:
                //Enum类型在序列化传输时是个i32
                TProtocolUtil.skip(in, fieldtype);
                break;
            default:
                TProtocolUtil.skip(in, fieldtype);
        }
    }

    /**
     * 字段包装类
     */
    @Data
    @Accessors(chain = true)
    public static class FieldWrap {

        /**
         * 字段值
         */
        private Object value;

        /**
         * 字段ID
         */
        private short id;

        /**
         * 字段名称
         */
        //private String name;

        /**
         * 字段类型
         */
        //private byte type;
    }

    /**
     * 对象包装类
     */
    @Data
    @Accessors(chain = true)
    public static class StructWrap {

        /**
         * 对象名
         */
        private String name;

        /**
         * 字段列表信息
         */
        private List<FieldWrap> fieldWraps;

    }

    /**
     * 读数据
     */
    public static StructWrap readData(TProtocol in) throws TException {
        List<FieldWrap> fieldWraps = new ArrayList<>();
        StructWrap result = new StructWrap().setName(in.readStructBegin().name).setFieldWraps(fieldWraps);

        TField schemeField;
        while (true) {
            schemeField = in.readFieldBegin();

            if (schemeField.type == TType.STOP) {
                break;
            }

            FieldWrap fieldWrap = new FieldWrap().setId(schemeField.id).setValue(readField(in, schemeField.type));
                    //.setName(schemeField.name).setType(schemeField.type);
            fieldWraps.add(fieldWrap);

            in.readFieldEnd();
        }
        in.readStructEnd();

        return result;
    }

    /**
     * 读字段数据
     */
    public static Object readField(TProtocol in, byte fieldtype) throws TException {
        Object value = null;

        switch (fieldtype) {
            case TType.VOID:
                TProtocolUtil.skip(in, fieldtype);
                break;
            case TType.BOOL:
                value = in.readBool();
                break;
            case TType.BYTE:
                value = in.readByte();
                break;
            case TType.DOUBLE:
                value = in.readDouble();
                break;
            case TType.I16:
                value = in.readI16();
                break;
            case TType.I32:
                value = in.readI32();
                break;
            case TType.I64:
                value = in.readI64();
                break;
            case TType.STRING:
                value = in.readString();
                break;
            case TType.STRUCT:
                value = readData(in);
                break;
            case TType.MAP:
                TMap tMap = in.readMapBegin();
                Map<Object, Object> map = new HashMap<>(2 * tMap.size);
                value = map;
                for (int i = 0; i < tMap.size; i++) {
                    map.put(readField(in, tMap.keyType), readField(in, tMap.valueType));
                }
                in.readMapEnd();
                break;
            case TType.SET:
                TSet tSet = in.readSetBegin();
                Set<Object> set = new HashSet<>(2 * tSet.size);
                value = set;
                for (int i = 0; i < tSet.size; i++) {
                    set.add(readField(in, tSet.elemType));
                }
                in.readSetEnd();
                break;
            case TType.LIST:
                TList tList = in.readListBegin();
                List<Object> list = new ArrayList<>(tList.size);
                value = list;
                for (int i = 0; i < tList.size; i++) {
                    list.add(readField(in, tList.elemType));
                }
                in.readListEnd();
                break;
            case TType.ENUM:
                //Enum类型在序列化传输时是个i32
                TProtocolUtil.skip(in, fieldtype);
                break;
            default:
                TProtocolUtil.skip(in, fieldtype);
        }
        return value;
    }

    /**
     * 实例化i32对象（可能是枚举类型）
     */
    @SneakyThrows
    private static Object instanceI32(int i32, Class clazz) {
        Object result = i32;
        if (!clazz.isEnum()) {
            return result;
        }

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
            Field[] fields = getAllFields(clazz);

            Map<Integer, String> fieldIdMap = new HashMap<>();
            int maxFieldId = 0;
            Field findField = null;
            for (Field field : fields) {
                if (!field.isEnumConstant()) {
                    continue;
                }
                TjFieldId tjFieldId = field.getAnnotation(TjFieldId.class);

                if (tjFieldId != null) {
                    if (tjFieldId.value() <= 0) {
                        throw new TException(clazz.getSimpleName() + "的" + field.getName() + "的字段编号值必须大于0");
                    }

                    String fieldName = fieldIdMap.get(tjFieldId.value());
                    if (fieldName != null) {
                        throw new TException(clazz.getSimpleName() + "的" + field.getName() + "的字段编号值和字段" + fieldName + "的重复了");
                    }
                    fieldIdMap.put(tjFieldId.value(), field.getName());

                    if (tjFieldId.value() > maxFieldId) {
                        maxFieldId = tjFieldId.value();
                    }

                    if (i32 == tjFieldId.value()) {
                        findField = field;
                    }
                }
                //field.setAccessible(true);
                //Enum<?> fieldEnum = (Enum<?>) field.get(arg);
            }

            if (findField != null) {
                result = Enum.valueOf(clazz, findField.getName());
            } else {
                for (Object obj : clazz.getEnumConstants()) {
                    Enum<?> e = (Enum<?>) obj;
                    if (e.ordinal() == ((i32 - 1) - maxFieldId)) {
                        result = Enum.valueOf(clazz, e.name());
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 把StructWrap实例化成真正对象
     */
    @SneakyThrows
    public static Object[] instanceStructWrap(int[] fieldIds, int maxFieldId, List<ThriftJsoaUtil.FieldWrap> fieldWraps, Class<?>[] clazzs) {
        Object[] result = new Object[clazzs.length];

        for (ThriftJsoaUtil.FieldWrap fieldWrap : fieldWraps) {
            if (fieldWrap.getId() < 0) {
                continue;
            }
            int index = 0;

            if (fieldWrap.getId() > 0) {
                boolean isFindFieldId = false;

                for (int i = 0; i < fieldIds.length; i++) {
                    if (fieldWrap.getId() == fieldIds[i]) {
                        isFindFieldId = true;
                        index = i;
                        break;
                    }
                }

                if (!isFindFieldId) {
                    index = (fieldWrap.getId() - 1) - maxFieldId;
                }
            }

            if (index < 0 || ((index + 1) > result.length)) {
                continue;
            }
            Class clazz = clazzs[index];

            if (fieldWrap.getValue() instanceof ThriftJsoaUtil.StructWrap) {

                Field[] fields = ThriftJsoaUtil.getAllFields(clazz);
                Class<?>[] fieldClazzs = new Class[fields.length];
                Annotation[][] fieldAnnss = new Annotation[fields.length][];
                String[] fieldNames = new String[fields.length];

                for (int i = 0; i < fields.length; i++) {
                    fieldClazzs[i] = fields[i].getType();
                    fieldAnnss[i] = fields[i].getAnnotations();
                    fieldNames[i] = fields[i].getName();
                }
                String tStructName = clazz.getSimpleName();

                ThriftJsoaUtil.FieldIdWrap fieldIdWrap = ThriftJsoaUtil.getFieldId(fieldAnnss, tStructName, fieldNames);
                int[] itFieldIds = fieldIdWrap.getFieldIds();
                int itMaxFieldId = fieldIdWrap.getMaxFieldId();
                ThriftJsoaUtil.StructWrap structWrap = (ThriftJsoaUtil.StructWrap) fieldWrap.getValue();

                Object[] objs = instanceStructWrap(itFieldIds, itMaxFieldId, structWrap.getFieldWraps(), fieldClazzs);

                result[index] = clazz.newInstance();
                for (int i = 0; i < fields.length; i++) {
                    fields[i].setAccessible(true);
                    fields[i].set(result[index], objs[i]);
                }

            } else if (fieldWrap.getValue() instanceof Integer) {
                int i32 = Integer.parseInt(String.valueOf(fieldWrap.getValue()));
                result[index] = instanceI32(i32, clazz);
            } else {
                result[index] = fieldWrap.getValue();
            }
        }

        return result;
    }

    /**
     * 获取方法的Struct类型
     */
    public static Set<Class<?>> getMethodStructTypes(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        if (clazz == null) {
            return result;
        }

        for (Method m : clazz.getMethods()) {
            result.addAll(getStructTypes(m.getGenericReturnType()));

            for (Type it : m.getGenericExceptionTypes()) {
                result.addAll(getStructTypes(it));
            }

            for (Type it : m.getGenericParameterTypes()) {
                result.addAll(getStructTypes(it));
            }
        }

        return result;
    }

    /**
     * 获取Struct类型
     */
    private static Set<Class<?>> getStructTypes(Type type) {
        Set<Class<?>> result = new HashSet<>();

        Class<?> clazz = null;
        if (type instanceof Class) {
            clazz = (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            clazz = (Class<?>) parameterizedType.getRawType();

            for (Type it : parameterizedType.getActualTypeArguments()) {
                result.addAll(getStructTypes(it));
            }

        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            result.addAll(getStructTypes(genericArrayType.getGenericComponentType()));
        }

        if (clazz != null && !clazz.isPrimitive() && !Short.class.equals(clazz) && !Integer.class.equals(clazz) && !Long.class.equals(clazz) &&
                !Float.class.equals(clazz) && !Double.class.equals(clazz) && !String.class.equals(clazz) && !BigDecimal.class.equals(clazz) &&
                !Map.class.isAssignableFrom(clazz) && !Set.class.isAssignableFrom(clazz) && !List.class.isAssignableFrom(clazz)) {
            result.add(clazz);

            for (Field f : getAllFields(clazz)) {
                result.addAll(getStructTypes(f.getGenericType()));
            }

            if (clazz.isArray()) {
                result.addAll(getStructTypes(clazz.getComponentType()));
            }
        }

        return result;
    }

    /**
     * 读值数据
     */
    public static List<Object> readValueData(TProtocol in) throws TException {
        List<Object> result = new ArrayList<>();
        result.add(in.readStructBegin());

        TField schemeField;
        while (true) {
            schemeField = in.readFieldBegin();
            result.add(schemeField);

            if (schemeField.type == TType.STOP) {
                break;
            }
            byte fieldtype = schemeField.type;

            switch (fieldtype) {
                case TType.VOID:
                    TProtocolUtil.skip(in, fieldtype);
                    break;
                case TType.BOOL:
                    result.add(in.readBool());
                    break;
                case TType.BYTE:
                    result.add(in.readByte());
                    break;
                case TType.DOUBLE:
                    result.add(in.readDouble());
                    break;
                case TType.I16:
                    result.add(in.readI16());
                    break;
                case TType.I32:
                    result.add(in.readI32());
                    break;
                case TType.I64:
                    result.add(in.readI64());
                    break;
                case TType.STRING:
                    result.add(in.readString());
                    break;
                case TType.STRUCT:
                    result.add(readValueData(in));
                    break;
                case TType.MAP:
                    TMap tMap = in.readMapBegin();
                    result.add(tMap);

                    for (int i = 0; i < tMap.size; i++) {
                        result.add(readField(in, tMap.keyType));
                        result.add(readField(in, tMap.valueType));
                    }
                    in.readMapEnd();
                    break;
                case TType.SET:
                    TSet tSet = in.readSetBegin();
                    result.add(tSet);

                    for (int i = 0; i < tSet.size; i++) {
                        result.add(readField(in, tSet.elemType));
                    }
                    in.readSetEnd();
                    break;
                case TType.LIST:
                    TList tList = in.readListBegin();
                    result.add(tList);

                    for (int i = 0; i < tList.size; i++) {
                        result.add(readField(in, tList.elemType));
                    }
                    in.readListEnd();
                    break;
                case TType.ENUM:
                    //Enum类型在序列化传输时是个i32
                    TProtocolUtil.skip(in, fieldtype);
                    break;
                default:
                    TProtocolUtil.skip(in, fieldtype);
            }

            in.readFieldEnd();
        }
        in.readStructEnd();

        return result;
    }

    @Data
    public static class SetEnd {}
    @Data
    public static class ListEnd {}
    @Data
    public static class MapEnd {}
    @Data
    public static class FieldStop {}
    @Data
    public static class FieldEnd {}
    @Data
    public static class StructEnd {}
    @Data
    public static class MessageEnd {}

    /**
     * 写值数据
     */
    public static void writeValueData(TProtocol out, List<Object> outValueData) throws TException {

        for (Object it : outValueData) {
            if (it instanceof SetEnd) {
                out.writeSetEnd();
            } else if (it instanceof ListEnd) {
                out.writeListEnd();
            } else if (it instanceof MapEnd) {
                out.writeMapEnd();
            } else if (it instanceof FieldStop) {
                out.writeFieldStop();
            } else if (it instanceof FieldEnd) {
                out.writeFieldEnd();
            } else if (it instanceof StructEnd) {
                out.writeStructEnd();
            } else if (it instanceof MessageEnd) {
                out.writeMessageEnd();
            } else if (it instanceof Boolean) {
                out.writeBool((Boolean) it);
            } else if (it instanceof Byte) {
                out.writeByte((Byte) it);
            } else if (it instanceof Double) {
                out.writeDouble((Double) it);
            } else if (it instanceof Short) {
                out.writeI16((Short) it);
            } else if (it instanceof Integer) {
                out.writeI32((Integer) it);
            } else if (it instanceof Long) {
                out.writeI64((Long) it);
            } else if (it instanceof String) {
                out.writeString((String) it);
            } else if (it instanceof TStruct) {
                out.writeStructBegin((TStruct) it);
            } else if (it instanceof TMap) {
                out.writeMapBegin((TMap) it);
            } else if (it instanceof TSet) {
                out.writeSetBegin((TSet) it);
            } else if (it instanceof TList) {
                out.writeListBegin((TList) it);
            } else if (it instanceof TMessage) {
                out.writeMessageBegin((TMessage) it);
            } else if (it instanceof TField) {
                out.writeFieldBegin((TField) it);
            }
        }
    }

}
