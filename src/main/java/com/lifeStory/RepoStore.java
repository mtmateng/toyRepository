package com.lifeStory;

import com.lifeStory.baseRepository.BaseRepository;
import com.lifeStory.baseRepository.Repository;
import com.lifeStory.repository.StudentRepository;
import com.lifeStory.utils.ClassUtils;
import com.zaxxer.hikari.HikariDataSource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepoStore {

    private final Map<Class<?>, Repository<?, ?>> store = new HashMap<>();

    public RepoStore() {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3308/publish-local?useUnicode=true&rewriteBatchedStatements=true&characterEncoding=utf8&useSSL=false");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUsername("root");
        dataSource.setPassword("dreamland");

        List<Class<Repository>> classes = ClassUtils.getAllClassByInterface(Repository.class, "com.lifeStory.repository");
        for (Class<Repository> aClass : classes) {

            Class entityClass = (Class) ((ParameterizedType) aClass.getGenericInterfaces()[0]).getActualTypeArguments()[0];
            Class idClass = (Class) ((ParameterizedType) aClass.getGenericInterfaces()[0]).getActualTypeArguments()[1];
            BaseRepository baseRepository = new BaseRepository(entityClass, idClass);
            RepositoryHandler repositoryHandler = new RepositoryHandler(baseRepository, dataSource);
            Repository repository = aClass.cast(Proxy.newProxyInstance(
                    aClass.getClassLoader(), new Class<?>[]{aClass, Repository.class}, repositoryHandler
            ));
            store.put(aClass, repository);
        }
    }

    public <T> T getRepository(Class<T> tClass) {
        try {
            return tClass.cast(store.get(tClass));
        } catch (Exception e) {
            throw new RuntimeException(String.format("没有找到相应的类%s", tClass.getName()), e);
        }
    }

}
