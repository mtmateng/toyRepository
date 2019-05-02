package com.toySpring.repository.helper;

import com.toySpring.repository.baseRepository.BaseRepository;
import com.toySpring.repository.baseRepository.Repository;
import com.toySpring.repository.utils.ClassUtils;
import com.toySpring.repository.utils.SQLUtil;
import javafx.util.Pair;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RepositoryHandler<T, ID> implements InvocationHandler {

    private final BaseRepository<T, ID> baseRepository;
    private final Class<Repository> realRepo;
    private final DataSource dataSource;
    private final EntityInfo entityInfo;

    private final Map<Method, SQLMethodInfo> method2MethodInfoMap = new HashMap<>();
    private final Map<Method, Pair<Object, Method>> method2RealMethodMap = new HashMap<>();
    private final Map<Class, Object> interface2ImplInstanceMap = new HashMap<>();

    public RepositoryHandler(BaseRepository<T, ID> baseRepository, EntityInfo entityInfo, DataSource dataSource,
                             Class<T> domainClass, Class<ID> idClass, Class<Repository> repo) {

        this.baseRepository = baseRepository;
        this.realRepo = repo;
        this.dataSource = dataSource;
        this.entityInfo = entityInfo;

        buildMethod2MethodInfoMap(entityInfo, domainClass, repo);               // 对于那种根据方法名生成SQL语句的方法，每次解析效率太低，直接生成一个map
        buildMethod2RealMethodMap(repo);

    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {

        if (method2RealMethodMap.get(method) != null) {
            return method2RealMethodMap.get(method).getValue().invoke(method2RealMethodMap.get(method).getKey(), args);
        } else if (method2MethodInfoMap.get(method) != null) {
            return SQLUtil.executeSelectSQL(dataSource, method2MethodInfoMap.get(method), args, method2MethodInfoMap.get(method).getActualClass(), entityInfo);
        } else {        //这种情况就是传入Class，返回Class了
            // todo
            throw new RuntimeException(String.format("尝试调用%s.%s()，但我们尚不支持其他方法声明方式", method.getDeclaringClass().getName(), method.getName()));
        }
    }

    /**
     * 构建一个方法到 SQL 语句的缓存
     * @param repo 需要实现的直接接口
     */
    private void buildMethod2MethodInfoMap(EntityInfo entityInfo, Class<T> domainClass, Class<Repository> repo) {

        for (Method declaredMethod : repo.getDeclaredMethods()) {
            method2MethodInfoMap.put(declaredMethod, SQLUtil.buildSQLTemplate(declaredMethod, entityInfo, domainClass, repo));
        }

    }

    /**
     * Spring的机制允许客户的Repository实现两个接口，除Repository系列的接口外，其他接口都必须有一个实现
     * 实现的类名默认是客户的Repository名+Impl。这里做了一个简单的实现
     * @param repo 需要实现的直接接口，比如StudentRepository
     */
    private void buildMethod2RealMethodMap(Class<Repository> repo) {

        for (Class<?> anInterface : repo.getInterfaces()) {
            if (Repository.class.isAssignableFrom(anInterface)) {   //是Repository的扩展接口
                actualBuildMethod2RealMethodMap(anInterface, baseRepository);
            } else {    //不是Repository的扩展接口，意味着要用户自己提供了一个实现
                Object repoImpl = getRepoImpl(anInterface, repo);
                actualBuildMethod2RealMethodMap(anInterface, repoImpl);
            }
        }

    }

    private void actualBuildMethod2RealMethodMap(Class<?> anInterface, Object repoImpl) {
        for (Method declaredMethod : anInterface.getDeclaredMethods()) {
            method2RealMethodMap.put(declaredMethod, new Pair<>(repoImpl, ClassUtils.getMethodByAnnouncement(repoImpl.getClass(), declaredMethod)));
        }
    }

    private Object getRepoImpl(Class<?> otherInterface, Class repo) {
        if (interface2ImplInstanceMap.get(otherInterface) == null) {
            try {
                interface2ImplInstanceMap.put(otherInterface, ClassUtils.findClassByNameAndInterface(repo.getSimpleName() + "Impl", otherInterface).newInstance());
            } catch (Exception e) {
                throw new RuntimeException(String.format("%sImpl没有找到，请检查", repo.getName()));
            }
        }
        return interface2ImplInstanceMap.get(otherInterface);
    }

}
