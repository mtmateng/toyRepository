package com.lifeStory.baseRepo;

import com.toySpring.repository.baseRepository.BaseRepository;
import com.toySpring.repository.helper.EntityInfo;
import com.toySpring.repository.helper.SQLMethodInfo;
import lombok.Getter;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.util.*;

import static com.toySpring.repository.utils.SQLUtil.actualBuildSQLTemplate;

public class MyBaseRepoImpl<T, ID> extends BaseRepository<T, ID> implements MyBaseRepo<T, ID> {

    @Getter
    private final Class<T> domainClass;
    @Getter
    private final Class<ID> idClass;
    @Getter
    private final EntityInfo entityInfo;
    @Getter
    private final DataSource dataSource;

    private final Map<String, SQLMethodInfo> methodInfoMap = new HashMap<>();

    public MyBaseRepoImpl(Class<T> domainClass, Class<ID> idClass, EntityInfo entityInfo, DataSource dataSource) {

        super(domainClass, idClass, entityInfo, dataSource);
        this.domainClass = domainClass;
        this.idClass = idClass;
        this.entityInfo = entityInfo;
        this.dataSource = dataSource;

    }

    @Override
    public T idPlus1(ID id) {

        T entity = findById(id);
        try {
            PropertyDescriptor prop = new PropertyDescriptor(entityInfo.getIdFieldName(), domainClass);
            if (id.getClass() == String.class) {
                prop.getWriteMethod().invoke(entity, String.format("%s+1", prop.getReadMethod().invoke(entity)));
            } else if (id.getClass() == Integer.class) {
                prop.getWriteMethod().invoke(entity, (Integer) prop.getReadMethod().invoke(entity) + 1);
            }
        } catch (Exception e) {
            // do nothing
        }
        return entity;

    }
}
