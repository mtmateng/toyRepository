package com.toySpring.repository.utils;


import com.toySpring.repository.helper.EntityInfo;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.*;

class EntityUtil {

    private final Map<Class<?>, EntityInfo> classEntityInfoMap = new HashMap<>();

    /**
     * 实际上，Spring就是不管Entity有没有被repo用到，只要标注了@Entity，且在被扫描的包里，则自动为其建表
     * @param packageName 需要扫描的Entity包
     */
    EntityUtil(String packageName, DataSource dataSource) {

        initClassEntityInfoMap(packageName);
        createTableIfNotExists(dataSource);

    }

    /**
     * 实际上，Spring还支持当字段缺失时update字段，先不实现
     */
    private void createTableIfNotExists(DataSource dataSource) {

        for (Class<?> aClass : classEntityInfoMap.keySet()) {
            String sql = SQLUtil.generateCreateTableSql(dataSource, classEntityInfoMap.get(aClass));
            SQLUtil.executeSQL(sql, dataSource);
        }

    }

    private void initClassEntityInfoMap(String packageName) {
        List<Class<?>> entities = ClassUtils.getAllClassByAnnotation(Entity.class, packageName);
        for (Class entity : entities) {
            EntityInfo entityInfo = new EntityInfo();
            entityInfo.setEntityClass(entity);
            entityInfo.setEntityDBName(NameUtil.getEntityDbName(entity));
            for (Field declaredField : entity.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Transient.class)) {   //这个标注表明了我们不搞他
                    continue;
                } else if (declaredField.isAnnotationPresent(Id.class)) {   //这里我们不解析@Column注解，太复杂了
                    if (entityInfo.getIdDBName() != null) {
                        throw new RuntimeException("有两个字段被标注为@ID，我们这个玩具系统暂不支持，见谅");
                    }
                    entityInfo.setIdDBName(NameUtil.getDataBaseName(declaredField.getName()));
                    entityInfo.setIdFieldName(declaredField.getName());
                }
                entityInfo.getFiledName2DBName().put(declaredField.getName(), NameUtil.getDataBaseName(declaredField.getName()));
                entityInfo.getFiledName2Field().put(declaredField.getName(),declaredField);
            }
            if (entityInfo.getIdFieldName() == null) {
                throw new RuntimeException(String.format("%s实体没有找到Id字段", entity.getName()));
            }
            classEntityInfoMap.put(entity, entityInfo);
        }
    }

    EntityInfo getEntityInfo(Class aClass) {
        return classEntityInfoMap.get(aClass);
    }

    Collection<Class<?>> getAllManagedClasses() {
        return classEntityInfoMap.keySet();
    }

}
