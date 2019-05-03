package com.toySpring.repository.baseRepository;

import com.toySpring.repository.helper.EntityInfo;

import javax.sql.DataSource;

public class BaseRepositoryFactory {

    @SuppressWarnings("unchecked")
    public static <T, Id> BaseRepository<T, Id> buildBaseRepository(Class<? extends BaseRepository> baseRepoClass,
                                                                    Class<T> entityClass, Class<Id> idClass,
                                                                    EntityInfo entityInfo, DataSource dataSource) {
        try {
            return baseRepoClass.getConstructor(Class.class,
                Class.class, EntityInfo.class, DataSource.class).newInstance(entityClass, idClass, entityInfo, dataSource);
        } catch (Exception e) {
            throw new RuntimeException(String.format("%s似乎没有找到对应的构造函数，或创建实例失败", baseRepoClass.getName()), e);
        }

    }

}
