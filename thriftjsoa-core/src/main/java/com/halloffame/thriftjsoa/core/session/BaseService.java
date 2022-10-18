package com.halloffame.thriftjsoa.core.session;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.SneakyThrows;
import net.sf.cglib.beans.BeanCopier;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 基础service
 * @author zhuwx
 */
public abstract class BaseService<T, E, M extends BaseMapper<E>> implements Base<T, E> {

    private final Class<E> entityClass;

    private final Class<T> dtoClass;

    private final Class<M> mapperClass;

    private final BeanCopier copier;

    public BaseService() {
        ParameterizedType parameterizedType = (ParameterizedType) this.getClass().getSuperclass().getGenericSuperclass();
        Type[] types = parameterizedType.getActualTypeArguments();
        entityClass = (Class<E>) types[1];
        dtoClass  = (Class<T>) types[0];
        mapperClass = (Class<M>) types[2];
        copier = BeanCopier.create(entityClass, dtoClass,false);
    }

    @Override
    @SneakyThrows
    public T selectById(Long id) {
        E entity = ThriftJsoaSessionData.getMapper(mapperClass).selectById(id);
        T dto = dtoClass.newInstance();
        copier.copy(entity, dto,null);
        return dto;
    }

    @Override
    public int insert(E entity) {
        return ThriftJsoaSessionData.getMapper(mapperClass).insert(entity);
    }

    @Override
    public int deleteById(Serializable id) {
        return ThriftJsoaSessionData.getMapper(mapperClass).deleteById(id);
    }

    @Override
    public int deleteById(E entity) {
        return ThriftJsoaSessionData.getMapper(mapperClass).deleteById(entity);
    }

    @Override
    public int deleteByMap(@Param("cm") Map<String, Object> columnMap) {
        return ThriftJsoaSessionData.getMapper(mapperClass).deleteByMap(columnMap);
    }

    @Override
    public int delete(@Param("ew") Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).delete(queryWrapper);
    }

    @Override
    public int deleteBatchIds(@Param("coll") Collection<?> idList) {
        return ThriftJsoaSessionData.getMapper(mapperClass).deleteBatchIds(idList);
    }

    @Override
    public int updateById(@Param("et") E entity) {
        return ThriftJsoaSessionData.getMapper(mapperClass).updateById(entity);
    }

    @Override
    public int update(@Param("et") E entity, @Param("ew") Wrapper<E> updateWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).update(entity, updateWrapper);
    }

    @Override
    public E selectById(Serializable id) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectById(id);
    }

    @Override
    public List<E> selectBatchIds(@Param("coll") Collection<? extends Serializable> idList) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectBatchIds(idList);
    }

    @Override
    public List<E> selectByMap(@Param("cm") Map<String, Object> columnMap) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectByMap(columnMap);
    }

    @Override
    public E selectOne(@Param("ew") Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectOne(queryWrapper);
    }

    @Override
    public boolean exists(Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).exists(queryWrapper);
    }

    @Override
    public Long selectCount(@Param("ew") Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectCount(queryWrapper);
    }

    @Override
    public List<E> selectList(@Param("ew") Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectList(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> selectMaps(@Param("ew") Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectMaps(queryWrapper);
    }

    @Override
    public List<Object> selectObjs(@Param("ew") Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectObjs(queryWrapper);
    }

    @Override
    public <P extends IPage<E>> P selectPage(P page, @Param("ew") Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectPage(page, queryWrapper);
    }

    @Override
    public <P extends IPage<Map<String, Object>>> P selectMapsPage(P page, @Param("ew") Wrapper<E> queryWrapper) {
        return ThriftJsoaSessionData.getMapper(mapperClass).selectMapsPage(page, queryWrapper);
    }
}
