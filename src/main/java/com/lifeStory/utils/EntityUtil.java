package com.lifeStory.utils;


import com.lifeStory.helper.EntityInfo;

import javax.persistence.Entity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityUtil {

    private final Map<Class, EntityInfo> classEntityInfoMap = new HashMap<>();

    public EntityUtil(String packageName) {

        List<Class> entities = ClassUtils.getAllClassByAnnotation(Entity.class, packageName);
        for (Class entity : entities) {
            EntityInfo entityInfo = new EntityInfo();
            entityInfo.setEntityClass(entity);
            entityInfo.setEntityDbName(NameUtil.getEntityName(entity));

        }

    }

}
