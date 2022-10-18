package com.halloffame.thriftjsoa.core.base;

import com.halloffame.thriftjsoa.core.util.ThriftJsoaUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tj协议
 * @author zhuwx
 */
public abstract class TjProtocol extends TProtocol {

    //@Getter
    //private boolean directWriteReadObj = false;

    @Getter
    private boolean directWriteObj = false;

    @Getter
    private boolean directReadObj = false;

    protected TjProtocol(TTransport trans) {
        super(trans);
    }

    protected TjProtocol(TTransport trans, boolean directWriteReadObj) {
        this(trans);
        //this.directWriteReadObj = directWriteReadObj;
    }

    /**
     * 写对象数组
     */
    public abstract void writeDirectObjects(Object[] objs);

    /**
     * 读对象数组
     */
    public abstract Object[] readDirectObjects(); //Class<?>[] objTypes

    /**
     * 写对象
     */
    public abstract void writeDirectObject(Object obj);

    /**
     * 读对象
     */
    public abstract Object readDirectObject();

    /**
     * 写对象数组
     */
    @SneakyThrows
    public void writeObjects(Object[] objs, String tStructName, String[] objNames, Type[] objTypes, Class<?>[] objClazzs,
                             Annotation[][] objAnnss, boolean isResult) {
        ThriftJsoaUtil.FieldIdWrap fieldIdWrap = ThriftJsoaUtil.getFieldId(objAnnss, tStructName, objNames);
        int[] fieldIds = fieldIdWrap.getFieldIds();
        int maxFieldId = fieldIdWrap.getMaxFieldId();

        if (isDirectWriteObj()) {
            Map<String, Object> map = new HashMap<>();
            map.put("tStructName", tStructName);
            map.put("isResult", isResult);
            map.put("maxFieldId", maxFieldId);

            List<Map<String, Object>> objList = new ArrayList<>();
            map.put("objList", objList);
            for (int i = 0; i < objs.length; i++) {
                Map<String, Object> objMap = new HashMap<>();
                objMap.put("value", objs[i]);
                objMap.put("name", objNames[i]);
                objMap.put("id", fieldIds[i]);
                objList.add(objMap);
            }

            writeDirectObject(map);
        } else {
            ThriftJsoaUtil.writeData(this, objs, tStructName, objNames, objTypes, objClazzs, fieldIds, maxFieldId, isResult);
        }
    }

    /**
     * 读对象数组
     */
    @SneakyThrows
    public Object[] readObjects(String tStructName, Type[] types, Class<?>[] clazzs, Annotation[][] annss, String[] names) {
        Object[] objs;
        ThriftJsoaUtil.FieldIdWrap fieldIdWrap = ThriftJsoaUtil.getFieldId(annss, tStructName, names);
        int[] fieldIds = fieldIdWrap.getFieldIds();
        int maxFieldId = fieldIdWrap.getMaxFieldId();

        if (isDirectReadObj()) {
            Object directObj = readDirectObject();

            if (directObj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) directObj;
                List<Map<String, Object>> objList = (List<Map<String, Object>>) map.get("objList");
                objs = new Object[names.length];

                for (Map<String, Object> objMap : objList) {
                    for (int i = 0; i < names.length; i++) {
                        if (names[i].equals(objMap.get("name"))) {
                            objs[i] = objMap.get("value");
                            break;
                        }
                    }
                }
            } else {
                ThriftJsoaUtil.StructWrap structWrap = (ThriftJsoaUtil.StructWrap) directObj;
                objs = ThriftJsoaUtil.instanceStructWrap(fieldIds, maxFieldId, structWrap.getFieldWraps(), clazzs);
            }
        } else {
            objs = ThriftJsoaUtil.readData(this, tStructName, types, clazzs, names, fieldIds, maxFieldId);
        }

        List<String> msgs = ThriftJsoaUtil.validate(objs, annss);
        if (msgs.size() > 0) {
            throw new TjApplicationException(TjApplicationException.VALIDATE_ERROR, msgs.get(0));
        }

        return objs;
    }

    /**
     * 读并写对象（用于代理转发）
     */
    @SneakyThrows
    public void readWriteObj(TjProtocol outTjProtocol) {

        if (this.isDirectReadObj() && outTjProtocol.isDirectWriteObj()) {
            outTjProtocol.writeDirectObject(this.readDirectObject());

        } else if (this.isDirectReadObj() && !outTjProtocol.isDirectWriteObj()) {
            Map<String, Object> map = (Map<String, Object>) this.readDirectObject();
            List<Map<String, Object>> objList = (List<Map<String, Object>>) map.get("objList");
            int size = objList.size();
            int maxFieldId = Integer.parseInt(String.valueOf(map.get("maxFieldId")));
            String tStructName = String.valueOf(map.get("tStructName"));
            boolean isResult = Boolean.parseBoolean(String.valueOf(map.get("isResult")));

            Object[] objs = new Object[size];
            String[] objNames = new String[size];
            Class<?>[] objClazzs = new Class[size];
            Type[] objTypes = new Type[size];
            int[] fieldIds = new int[size];

            for (int i = 0; i < objList.size(); i++) {
                Map<String, Object> objMap = objList.get(i);
                objs[i] = objMap.get("value");
                objNames[i] = String.valueOf(objMap.get("name"));
                fieldIds[i] = Integer.parseInt(String.valueOf(objMap.get("id")));

                if (objs[i] != null) {
                    objClazzs[i] = objs[i].getClass();
                    objTypes[i] = objClazzs[i];
                }
            }

            ThriftJsoaUtil.writeData(outTjProtocol, objs, tStructName, objNames, objTypes, objClazzs, fieldIds, maxFieldId, isResult);

        } else if (!this.isDirectReadObj() && outTjProtocol.isDirectWriteObj()) {
            outTjProtocol.writeDirectObject(ThriftJsoaUtil.readData(this));
        } else {
            ThriftJsoaUtil.readWriteData(this, outTjProtocol);
        }
    }
}
