package com.toySpring.repository.baseRepository;

import com.toySpring.repository.helper.EntityInfo;
import com.toySpring.repository.helper.SQLMethodInfo;
import com.toySpring.repository.utils.SQLUtil;
import lombok.Getter;

import javax.sql.DataSource;

import java.util.*;

import static com.toySpring.repository.utils.SQLUtil.actualBuildSQLTemplate;


public class BaseRepository<T, ID> implements Repository<T, ID> {

    @Getter
    private final Class<T> domainClass;
    @Getter
    private final Class<ID> idClass;
    @Getter
    private final EntityInfo entityInfo;
    @Getter
    private final DataSource dataSource;

    private final Map<String, SQLMethodInfo> methodInfoMap = new HashMap<>();

    public BaseRepository(Class<T> domainClass, Class<ID> idClass, EntityInfo entityInfo,
                          DataSource dataSource) {

        this.domainClass = domainClass;
        this.idClass = idClass;
        this.entityInfo = entityInfo;
        this.dataSource = dataSource;

    }

    // 示例如何做一个基础的方法，比如Repository中的findById方法;
    public T findById(ID id) {

        if (id == null) {
            throw new RuntimeException("id为null或空");
        }
        if (methodInfoMap.get("findById") == null) {
            SQLMethodInfo methodInfo = new SQLMethodInfo();
            List<String> selectFieldNames = new ArrayList<>(entityInfo.getFiledName2DBName().keySet());
            List<String> queryFieldNames = Collections.singletonList(entityInfo.getIdFieldName());
            methodInfo.setSQLTemplate(actualBuildSQLTemplate(entityInfo, selectFieldNames, queryFieldNames));
            methodInfo.setSelectFieldsNames(selectFieldNames);
            methodInfo.setQueryFieldsNames(queryFieldNames);
            methodInfo.setActualClass(domainClass);
            methodInfoMap.put("findById", methodInfo);
        }

        return domainClass.cast(SQLUtil.executeSelectSQL(dataSource, methodInfoMap.get("findById"), new Object[]{id}, domainClass, entityInfo));

    }

}
