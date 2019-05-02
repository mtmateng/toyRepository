package com.toySpring.repository.baseRepository;

import com.toySpring.repository.helper.EntityInfo;
import lombok.Getter;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.*;

public class BaseRepository<T, ID> implements Repository<T, ID> {

    @Getter
    private final Class<T> domainClass;
    @Getter
    private final Class<ID> idClass;
    @Getter
    private final EntityInfo entityInfo;
    @Getter
    private final DataSource dataSource;

    public BaseRepository(Class<T> domainClass, Class<ID> idClass, EntityInfo entityInfo,
                          DataSource dataSource, Class<Repository> repo) {

        this.domainClass = domainClass;
        this.idClass = idClass;
        this.entityInfo = entityInfo;
        this.dataSource = dataSource;

    }

    private String getEntityDBName() {
        return entityInfo.getEntityDBName();
    }

    // 示例如何做一个基础的方法，比如Repository中的findById方法;
    public T findById(ID id) {

        if (id == null) {
            throw new RuntimeException("id为null或空");
        }
        return null;
//        String sql = generateSelectSql(entityInfo.getEntityDBName(), entityInfo.getIdDBName(), id);
//        return domainClass.cast(buildReturnValue(executeSelectSql(sql), domainClass, "findById"));

    }

//    public T save(T entity) {
//
//    }

    /**
     * 演示如何按methodName动态生成sql语句
     * Spring支持select部分字段，有两种方式，一个是返回值直接指定为一个interface，一个是传入一个Type参数
     * 如果返回值是一个接口，那么就需要用cglib来搞，如果传入一个Type参数，那就直接实例化一个就好
     */
    public Object executeSelectSqlByMethodName(Method method, Object[] args) {

//        List<String> queryFieldNames = getQueryFiledNamesByMethodName(method.getName());
//        List<String> selectFieldNames = getSelectFieldNamesByMethod(method);
//        List<String> queryFieldNames = null;
//        checkParams(queryFieldNames, args, method.getName());
//        String sql = generateSelectSql(getEntityDBName(), queryFieldNames, args);
//        List<T> results = executeSelectSql(sql);
//        return buildReturnValue(results, method.getReturnType(), method.getName());
        return null;

    }

    private void checkParams(List<String> fieldNames, Object[] args, String methodName) {

        if (args == null || fieldNames.size() != args.length) {
            throw new RuntimeException("参数数量错误：" + methodName);
        }
        for (int i = 0; i != args.length; ++i) {
            if (entityInfo.getFieldName2Type().get(fieldNames.get(i)) == null ||
                !entityInfo.getFieldName2Type().get(fieldNames.get(i)).equals(args[0].getClass())) {
                throw new RuntimeException("第" + i + "个参数类型错误：" + methodName);
            }
        }
    }

}
