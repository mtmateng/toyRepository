package com.lifeStory;

import com.lifeStory.baseRepository.BaseRepository;
import com.lifeStory.utils.SQLUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RepositoryHandler<T, ID> implements InvocationHandler {

    private final BaseRepository<T, ID> baseRepository;
    private final Map<Method, String> method2SqlTemplate = new HashMap<>();

    public RepositoryHandler(BaseRepository<T, ID> baseRepository) {
        this.baseRepository = baseRepository;
        for (Method declaredMethod : baseRepository.getClass().getDeclaredMethods()) {
            method2SqlTemplate.put(declaredMethod, SQLUtil.buildSqlTemplate(declaredMethod, baseRepository.getEntityInfo()));
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) {

        Method baseMethod;
        if ((baseMethod = baseRepository.getDeclaredMethod(method)) != null) {
            try {
                return baseMethod.invoke(baseRepository, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(String.format("调用%s时出错", baseMethod.getName()));
            }
        }

        if (method.getName().startsWith("find")) {
            return baseRepository.executeSelectSqlByMethodName(method, args);
        } else if (method.getName().startsWith("save")) {
            throw new RuntimeException("save方法尚未支持");
        } else if (method.getName().startsWith("delete")) {
            throw new RuntimeException("delete方法尚未支持");
        } else {
            throw new RuntimeException(String.format("方法名有误: %s", method.getName()));
        }
    }

}
