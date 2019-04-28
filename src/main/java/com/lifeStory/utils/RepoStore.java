package com.lifeStory.utils;

import com.lifeStory.RepositoryHandler;
import com.lifeStory.baseRepository.BaseRepository;
import com.lifeStory.baseRepository.Repository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import javax.sql.DataSource;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepoStore {

    private final Map<Class<?>, Repository<?, ?>> store = new HashMap<>();

    @Getter
    private final DataSource dataSource;

    public RepoStore() {

        HikariDataSource innerDataSource = new HikariDataSource();
        innerDataSource.setJdbcUrl("jdbc:h2:mem:test");
        innerDataSource.setDriverClassName("org.h2.Driver");
        innerDataSource.setUsername("testdb");
        innerDataSource.setPassword("testdb");
        dataSource = innerDataSource;

        List<Class<Repository>> classes = ClassUtils.getAllClassByInterface(Repository.class, "com.lifeStory.repository");
        for (Class<Repository> aClass : classes) {

            Class<?> entityClass = (Class) ((ParameterizedType) aClass.getGenericInterfaces()[0]).getActualTypeArguments()[0];
            Class<?> idClass = (Class) ((ParameterizedType) aClass.getGenericInterfaces()[0]).getActualTypeArguments()[1];
            RepositoryHandler<?, ?> repositoryHandler = buildRepositoryHandler(entityClass, idClass);
            Repository repository = aClass.cast(Proxy.newProxyInstance(
                    aClass.getClassLoader(), new Class<?>[]{aClass, Repository.class}, repositoryHandler
            ));
            store.put(aClass, repository);
        }
    }

    private <T, Id> RepositoryHandler<T, Id> buildRepositoryHandler(Class<T> entityClass, Class<Id> idClass) {
        return new RepositoryHandler<>(new BaseRepository<>(entityClass, idClass), dataSource);
    }

    public <Repo> Repo getRepository(Class<Repo> tClass) {
        try {
            return tClass.cast(store.get(tClass));
        } catch (Exception e) {
            throw new RuntimeException(String.format("没有找到相应的类%s", tClass.getName()), e);
        }
    }

}
