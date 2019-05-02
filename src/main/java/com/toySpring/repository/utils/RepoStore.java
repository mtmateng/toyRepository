package com.toySpring.repository.utils;

import com.toySpring.repository.helper.RepositoryHandler;
import com.toySpring.repository.baseRepository.BaseRepository;
import com.toySpring.repository.baseRepository.Repository;
import lombok.Getter;

import javax.sql.DataSource;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepoStore {

    private final Map<Class<?>, Repository<?, ?>> store;

    @Getter
    private final DataSource dataSource;
    private final EntityUtil entityUtil;

    public RepoStore(DataSource dataSource, String entityPackageName, String repoPackageName) {

        this.dataSource = dataSource;
        this.entityUtil = initEntityUtil(entityPackageName);
        this.store = initStore(repoPackageName);

    }

    private Map<Class<?>, Repository<?, ?>> initStore(String repoPackageName) {
        Map<Class<?>, Repository<?, ?>> store = new HashMap<>();
        List<Class<Repository>> classes = ClassUtils.getAllClassByInterface(Repository.class, repoPackageName);
        for (Class<Repository> aClass : classes) {

            Class<?> entityClass = (Class) ((ParameterizedType) aClass.getGenericInterfaces()[0]).getActualTypeArguments()[0];
            if (!entityUtil.getAllManagedClasses().contains(entityClass)) {
                throw new RuntimeException(String.format("发现Repo：%s使用了未被管理的Entity：%s，请检查Entity所在包，或检查其是否被@Entity标注", aClass.getName(), entityClass.getName()));
            }
            Class<?> idClass = (Class) ((ParameterizedType) aClass.getGenericInterfaces()[0]).getActualTypeArguments()[1];
            RepositoryHandler<?, ?> repositoryHandler = buildRepositoryHandler(entityClass, idClass, aClass);
            Repository repository = aClass.cast(Proxy.newProxyInstance(
                aClass.getClassLoader(), new Class<?>[]{aClass, Repository.class}, repositoryHandler
            ));
            store.put(aClass, repository);
        }
        return store;
    }


    private EntityUtil initEntityUtil(String entityPackageName) {
        return new EntityUtil(entityPackageName, dataSource);
    }


    private <T, Id> RepositoryHandler<T, Id> buildRepositoryHandler(Class<T> entityClass, Class<Id> idClass, Class<Repository> repo) {
        return new RepositoryHandler<>(new BaseRepository<>(entityClass, idClass, entityUtil.getEntityInfo(entityClass), dataSource),
            entityUtil.getEntityInfo(entityClass), dataSource, entityClass, repo);
    }

    public <Repo> Repo getRepository(Class<Repo> tClass) {
        try {
            return tClass.cast(store.get(tClass));
        } catch (Exception e) {
            throw new RuntimeException(String.format("没有找到相应的类%s", tClass.getName()), e);
        }
    }

}
