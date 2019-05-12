package com.toySpring.repository.baseRepository;

import com.toySpring.repository.helper.EntityInfo;
import com.toySpring.repository.helper.SelectSQLMethodInfo;
import com.toySpring.repository.utils.SQLUtil;
import lombok.Getter;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.util.*;

/**
 * 这个类实现了Repository系列接口的所有方法，当业务Repository中继承自Repository系列接口
 * 的方法被调用时，这个类中的方法会被实际调用。
 *
 * SQL语句生成的工作仍然被集中到了SQLUtil里面
 *
 * @param <T> Repository管理的实体类
 * @param <ID> Repository管理的实体类的ID类
 */
public class BaseRepository<T, ID> implements Repository<T, ID> {

    @Getter
    private final Class<T> domainClass;
    @Getter
    private final Class<ID> idClass;
    @Getter
    private final EntityInfo entityInfo;
    @Getter
    private final DataSource dataSource;

    private final Map<String, SelectSQLMethodInfo> selectMethodInfoMap = new HashMap<>();

    public BaseRepository(Class<T> domainClass, Class<ID> idClass, EntityInfo entityInfo,
                          DataSource dataSource) {

        this.domainClass = domainClass;
        this.idClass = idClass;
        this.entityInfo = entityInfo;
        this.dataSource = dataSource;

    }

    // 示例如何做一个基础的方法，比如Repository中的findById方法;
    @Override
    public T findById(ID id) {

        if (id == null) {
            throw new RuntimeException("id为null或空");
        }
        //这一段的复杂度，在SpringRepository里是用EntityManager来管理的，我不想再实现一个
        //类似的东西了，所以直接用Repository来管理，其实职责划分有点混在一起了。
        if (selectMethodInfoMap.get("findById") == null) {
            SelectSQLMethodInfo methodInfo = new SelectSQLMethodInfo();
            List<String> selectFieldNames = new ArrayList<>(entityInfo.getFiledName2DBName().keySet());
            List<String> queryFieldNames = Collections.singletonList(entityInfo.getIdFieldName());
            methodInfo.setSQLTemplate(SQLUtil.actualBuildSelectSQLTemplate(entityInfo, selectFieldNames, queryFieldNames));
            methodInfo.setSelectFieldsNames(selectFieldNames);
            methodInfo.setQueryFieldsNames(queryFieldNames);
            methodInfo.setActualClass(domainClass);
            selectMethodInfoMap.put("findById", methodInfo);
        }

        return domainClass.cast(SQLUtil.executeSelectSQL(dataSource, selectMethodInfoMap.get("findById"), new Object[]{id}, domainClass, entityInfo));

    }

    @Override
    public T save(T entity) {

        if (entity == null) {
            throw new RuntimeException("你咋传了个null进来，让我怎么保存");
        }
        try {
            PropertyDescriptor prop = new PropertyDescriptor(entityInfo.getIdFieldName(), domainClass);
            if (findById(idClass.cast(prop.getReadMethod().invoke(entity))) != null) {
                SQLUtil.executeUpdateSQL(dataSource, entityInfo, entity);
            } else {
                SQLUtil.executeInsertSQL(dataSource, entityInfo, entity);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("保存%s失败", entity), e);
        }
        return entity;

    }

    @Override
    public Collection<T> saveAll(Iterable<T> entities) {

        Collection<T> ret = new ArrayList<>();
        for (T entity : entities) {
            ret.add(save(entity));
        }
        return ret;

    }

}
