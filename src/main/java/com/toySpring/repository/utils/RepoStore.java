package com.toySpring.repository.utils;

import com.toySpring.repository.baseRepository.BaseRepository;
import com.toySpring.repository.baseRepository.BaseRepositoryFactory;
import com.toySpring.repository.baseRepository.Repository;
import com.toySpring.repository.custom.CustomRepoSetting;
import com.toySpring.repository.helper.RepositoryHandler;

import javax.sql.DataSource;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.*;

public class RepoStore {

    private final Map<Class<?>, Repository<?, ?>> store = new HashMap<>();

    public RepoStore(Class mainClass, Class<?> dataSourceStore, CustomRepoSetting... customRepoSettings) {

        Set<Class> processedEntities = new HashSet<>();
        if (customRepoSettings.length == 0) {
            initStore(mainClass, dataSourceStore, new CustomRepoSetting(), processedEntities);
        }
        for (CustomRepoSetting customRepoSetting : customRepoSettings) {
            initStore(mainClass, dataSourceStore, customRepoSetting, processedEntities);
        }

    }

    private void initStore(Class mainClass, Class<?> dataSourceStore, CustomRepoSetting customRepoSetting, Set<Class> processedEntities) {

        fillIfDefaultRepoSetting(mainClass, customRepoSetting);
        DataSource dataSource;
        try {
            dataSource = (DataSource) dataSourceStore.getMethod("getDataSource", String.class).invoke(null, customRepoSetting.getDataSourceName());
        } catch (Exception e) {
            throw new RuntimeException(String.format("%s没有发现getDataSource(String)方法，请提供之", dataSourceStore.getName()));
        }
        EntityUtil entityUtil = initEntityUtil(customRepoSetting.getEntityPackage(), dataSource, processedEntities);
        initStore(customRepoSetting.getRepositoryPackage(), customRepoSetting.getBaseRepositoryClass(), entityUtil, dataSource);

    }

    /**
     * 如果CustomRepoSetting都是默认设置，则为其设置一个合理的默认值
     */
    private void fillIfDefaultRepoSetting(Class mainClass, CustomRepoSetting customRepoSetting) {

        if (customRepoSetting.getBaseRepositoryClass() == null) {
            customRepoSetting.setBaseRepositoryClass(BaseRepository.class);
        }
        if (customRepoSetting.getDataSourceName() == null) {
            customRepoSetting.setDataSourceName("default");
        }
        if (customRepoSetting.getEntityPackage() == null) {
            customRepoSetting.setEntityPackage(mainClass.getPackage().getName());
        }
        if (customRepoSetting.getRepositoryPackage() == null) {
            customRepoSetting.setRepositoryPackage(mainClass.getPackage().getName());
        }

    }

    private void initStore(String repoPackageName, Class<? extends BaseRepository> baseRepoClass,
                           EntityUtil entityUtil, DataSource dataSource) {

        List<Class<Repository>> classes = ClassUtils.getAllClassByInterface(Repository.class, repoPackageName);
        for (Class<Repository> aClass : classes) {

            if (!aClass.isInterface()) {
                continue;   //只处理接口，不处理BaseRepository这样的实现
            }

            if (store.containsKey(aClass)) {
                throw new RuntimeException(String.format("%s包已经扫描过，请检查", repoPackageName));
            }

            Class<?> entityClass = (Class) ((ParameterizedType) aClass.getGenericInterfaces()[0]).getActualTypeArguments()[0];
            if (!entityUtil.getAllManagedClasses().contains(entityClass)) {
                throw new RuntimeException(String.format("发现Repo：%s使用了未被管理的Entity：%s，请检查Entity所在包，或检查其是否被@Entity标注", aClass.getName(), entityClass.getName()));
            }

            Class<?> idClass = (Class) ((ParameterizedType) aClass.getGenericInterfaces()[0]).getActualTypeArguments()[1];
            RepositoryHandler<?, ?> repositoryHandler = buildRepositoryHandler(entityClass, idClass, aClass, baseRepoClass, entityUtil, dataSource);
            Repository repository = aClass.cast(Proxy.newProxyInstance(
                aClass.getClassLoader(), new Class<?>[]{aClass, Repository.class}, repositoryHandler
            ));
            store.put(aClass, repository);
        }

    }


    private EntityUtil initEntityUtil(String entityPackageName, DataSource dataSource, Set<Class> processedEntities) {
        EntityUtil entityUtil = new EntityUtil(entityPackageName, dataSource);
        if (processedEntities.stream().anyMatch(entityUtil.getAllManagedClasses()::contains)) {
            throw new RuntimeException(String.format("%s包已经被扫描过", entityPackageName));
        }
        return entityUtil;
    }


    private <T, Id> RepositoryHandler<T, Id> buildRepositoryHandler(Class<T> entityClass, Class<Id> idClass, Class<Repository> repo,
                                                                    Class<? extends BaseRepository> baseRepoClass, EntityUtil entityUtil, DataSource dataSource) {
        return new RepositoryHandler<>(BaseRepositoryFactory.buildBaseRepository(baseRepoClass, entityClass, idClass, entityUtil.getEntityInfo(entityClass), dataSource),
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
