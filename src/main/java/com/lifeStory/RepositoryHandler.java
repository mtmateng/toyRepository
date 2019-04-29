package com.lifeStory;

import com.lifeStory.baseRepository.BaseRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RepositoryHandler<T, ID> implements InvocationHandler {

    private final BaseRepository<T, ID> baseRepository;

    public RepositoryHandler(BaseRepository<T, ID> baseRepository) {
        this.baseRepository = baseRepository;
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
