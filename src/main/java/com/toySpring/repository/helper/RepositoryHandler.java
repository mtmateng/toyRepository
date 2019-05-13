package com.toySpring.repository.helper;

import com.toySpring.repository.baseRepository.BaseRepository;
import com.toySpring.repository.baseRepository.Repository;
import com.toySpring.repository.utils.ClassUtils;
import com.toySpring.repository.utils.NameUtil;
import com.toySpring.repository.utils.SQLUtil;
import javafx.util.Pair;

import javax.sql.DataSource;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 这个类是Repository系列接口的动态代理，每一个业务Repository都会产生一个Handler，生成一个Proxy
 * 在这个类的实例创建时，我们首先会解析它的各种方法，分为三种情况：
 * 1. 如果这些方法是其扩展的接口中的方法，则我们必然
 *    能找到这个方法的实现，或者在BaseRepository里，或者在用户自己提供的BaseRepository里。
 *    针对这些方法，我们建立一个接口方法到实现方法的Map，这样当业务Repository相应方法被调用时
 *    我们就能准确调用实际的方法。
 * 2. 如果这些方法不继承于任何接口，并且并不是参数化的方法，即不是类似<T> List<T> findByGender(String gender, Class<T> type);
 *    这样的声明，那我们直接解析方法名，生成SQL语句，并且解析方法的返回值，确定要返回什么类型的东西，将方法
 *    的解析结果组织成一个SQLMethodInfo类，并建立一个Method到SQLMethodInfo的映射。
 * 3. 如果这些方法不继承于任何借口，并且是参数化方法，那就将解析推迟到运行时。具体解析方式和第二个相同。
 *
 * 为了稍微切分业务功能界面，这个类只做Method的解析，所有和SQL生成、SQL结果解析的工作都放在SQLUtil里面做了。这样也方便切换数据库时
 * 数据库方言的对接。
 *
 * @param <T> Repository管理的实体类
 * @param <ID> Repository管理的实体类的ID
 */
public class RepositoryHandler<T, ID> implements InvocationHandler {

    private final BaseRepository<T, ID> baseRepository;
    private final DataSource dataSource;
    private final EntityInfo entityInfo;

    private final Map<Method, SelectSQLMethodInfo> method2MethodInfoMap = new HashMap<>();
    private final Map<Method, Pair<Object, Method>> method2RealMethodMap = new HashMap<>();
    private final Map<Method, Map<Class, SelectSQLMethodInfo>> method2MethodInfoWithClassMap = new HashMap<>();
    private final Map<Class, Object> interface2ImplInstanceMap = new HashMap<>();

    public RepositoryHandler(BaseRepository<T, ID> baseRepository, EntityInfo entityInfo, DataSource dataSource,
                             Class<T> domainClass, Class<Repository> repo) {

        this.baseRepository = baseRepository;
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
        } else {        //这种情况就是传入Class，返回Class了，而且这一步已经检查完了，确信只有一个Class为参数
            Class classParameter = getReallyParameterType(method, args);
            if (method2MethodInfoWithClassMap.get(method) == null || method2MethodInfoWithClassMap.get(method).get(classParameter) == null) {
                method2MethodInfoWithClassMap.putIfAbsent(method, new HashMap<>());
                method2MethodInfoWithClassMap.get(method).put(classParameter, buildSQLTemplateWithClass(method, classParameter, entityInfo));
            }
            return SQLUtil.executeSelectSQL(dataSource, method2MethodInfoWithClassMap.get(method).get(classParameter), args, method2MethodInfoWithClassMap.get(method).get(classParameter).getActualClass(), entityInfo);
        }
    }

    private SelectSQLMethodInfo buildSQLTemplateWithClass(Method method, Class parameterClass, EntityInfo entityInfo) {

        SelectSQLMethodInfo methodInfo = new SelectSQLMethodInfo();
        List<String> selectFieldNames = checkAndReturnFieldNamesByClass(method, entityInfo, parameterClass, methodInfo);
        List<String> queryFieldNames = getQueryFiledNamesByMethodName(method, entityInfo);
        String SQL = SQLUtil.buildSelectSQLTemplate(dataSource, entityInfo, selectFieldNames, queryFieldNames);

        methodInfo.setMethod(method);
        methodInfo.setSQLTemplate(SQL);
        methodInfo.setQueryFieldsNames(queryFieldNames);

        return methodInfo;

    }

    private List<String> checkAndReturnFieldNamesByClass(Method method, EntityInfo entityInfo, Class parameterClass, SelectSQLMethodInfo methodInfo) {

        List<String> ret = new ArrayList<>();
        Pair<Class, Class> realReturnType = checkAndGetReturnType(method);

        for (Field declaredField : parameterClass.getDeclaredFields()) {
            if (entityInfo.getFiledName2Field().get(declaredField.getName()).getType() != declaredField.getType()) {
                throw new RuntimeException(String.format("%s.%s()的返回类型与%s中的字段%s不匹配，请检查", method.getDeclaringClass().getName(), method.getName(), entityInfo.getEntityClass().getName(), declaredField.getName()));
            }
            ret.add(declaredField.getName());
        }

        methodInfo.setWrapClass(realReturnType.getKey());
        methodInfo.setActualClass(parameterClass);
        methodInfo.setSelectFieldsNames(ret);

        return ret;
    }

    private Class getReallyParameterType(Method method, Object[] args) {

        List<Class> classes = new ArrayList<>();
        for (int i = 0; i != method.getParameterCount(); ++i) {
            if (method.getParameterTypes()[i] == Class.class) {
                classes.add((Class) args[i]);
            }
        }
        if (classes.size() != 1) {
            throw new RuntimeException(String.format("%s.%s()缺少Class类型的参数，或有两个以上的class参数", method.getDeclaringClass().getName(), method.getName()));
        } else {
            return classes.get(0);
        }
    }

    /**
     * 构建一个方法到 SQLTemplate 语句的缓存
     * @param repo 需要实现的直接接口
     */
    private void buildMethod2MethodInfoMap(EntityInfo entityInfo, Class<T> domainClass, Class<Repository> repo) {

        for (Method declaredMethod : repo.getDeclaredMethods()) {
            SelectSQLMethodInfo methodInfo = checkMethodDeclarationAndGetSQLTemplate(declaredMethod, entityInfo, domainClass);
            method2MethodInfoMap.put(declaredMethod, methodInfo);
        }

    }

    /**
     * 统一检查函数的声明是否正确。
     * 我们只接受这样的函数声明：返回值，可以是一个接口、实体类本身，或者一个模板参数类，以及他们的Collection、List、Set、Optional
     * 我们称接口、实体类本身，或者模板参数为rawType，称Collection、List、Set、Optional为wrapType
     * 当返回接口时，将会检查接口中的get方法的名字，将其返回值与实体类中相应字段的类型进行比对
     * 当返回模板参数时，也会对比该模板参数类中字段的类型和实体类中对应字段的类型，而且需要有且只有一个类型为Class的参数，用以指定返回参数
     * @param method repo中直接声明的函数
     */
    private SelectSQLMethodInfo checkMethodDeclarationAndGetSQLTemplate(Method method, EntityInfo entityInfo, Class domainClass) {

        // 检查函数名，目前只支持find方法
        if (!method.getName().startsWith("find")) {
            throw new RuntimeException("目前仅支持find方法");
        }

        // 检查返回值，是不是符合要求
        Pair<Class, Class> returnType = checkAndGetReturnType(method);
        if (returnType.getValue() == null) {
            return null;
        }

        // 根据返回值构建select子句中包含哪些字段
        List<String> selectFieldNames;
        if (returnType.getValue().isInterface()) {
            selectFieldNames = checkAndReturnFieldNamesByInterface(returnType.getValue(), domainClass, entityInfo);
        } else if (returnType.getValue() == domainClass) {
            selectFieldNames = new ArrayList<>(entityInfo.getFiledName2DBName().keySet());
        } else {
            throw new RuntimeException(String.format("%s.%s()返回值%s不支持", method.getDeclaringClass().getName(), method.getName(), returnType.getValue().getName()));
        }
        List<String> queryFieldNames = getQueryFiledNamesByMethodName(method, entityInfo);
        String SQL = SQLUtil.buildSelectSQLTemplate(dataSource, entityInfo, selectFieldNames, queryFieldNames);
        SelectSQLMethodInfo methodInfo = new SelectSQLMethodInfo();
        methodInfo.setMethod(method);
        methodInfo.setSelectFieldsNames(selectFieldNames);
        methodInfo.setQueryFieldsNames(queryFieldNames);
        methodInfo.setWrapClass(returnType.getKey());
        methodInfo.setActualClass(returnType.getValue());
        methodInfo.setSQLTemplate(SQL);

        return methodInfo;

    }

    private static List<String> checkAndReturnFieldNamesByInterface(Class realReturnType, Class domainClass, EntityInfo entityInfo) {

        List<String> ret = new ArrayList<>();
        for (Method declaredMethod : realReturnType.getDeclaredMethods()) {
            String fieldName = NameUtil.firstCharToLowerCase(declaredMethod.getName().replaceFirst("^get", ""));
            if (entityInfo.getFiledName2Field().get(fieldName).getType() != declaredMethod.getReturnType()) {
                throw new RuntimeException(String.format("%s.%s()的返回类型与%s中的字段%s不匹配，请检查", realReturnType.getName(), declaredMethod.getName(), domainClass.getName(), fieldName));
            }
            ret.add(fieldName);
        }
        return ret;

    }

    /**
     * 只实现一个简版的，示例而已，即只允许find[get]XXXByName1AndName2(param1,param2)这种形式
     */
    private static List<String> getQueryFiledNamesByMethodName(Method method, EntityInfo entityInfo) {

        String removedStart = method.getName().replaceFirst("^(find|get)\\w?By", "");
        List<String> queryFieldNames = Arrays.stream(removedStart.split("And")).map(NameUtil::firstCharToLowerCase).collect(Collectors.toList());
        checkQueryParams(queryFieldNames, method, entityInfo);
        return queryFieldNames;

    }

    //检查一下方法名、实体类字段类型和参数类型是否匹配
    private static void checkQueryParams(List<String> queryFieldNames, Method method, EntityInfo entityInfo) {

        List<Parameter> parameters = Arrays.stream(method.getParameters()).filter(parameter -> parameter.getType() != Class.class).collect(Collectors.toList());
        if (queryFieldNames.size() != parameters.size()) {
            throw new RuntimeException("参数数量错误：" + method.getName());
        }

        // 检查一下参数类型和Entity类型是否匹配
        for (int i = 0; i != parameters.size(); ++i) {
            if (entityInfo.getFiledName2Field().get(queryFieldNames.get(i)).getType() == null ||
                !entityInfo.getFiledName2Field().get(queryFieldNames.get(i)).getType().equals(method.getParameters()[i].getType())) {
                throw new RuntimeException("第" + i + "个参数类型错误：" + method.getName());
            }
        }

    }

    /**
     * 检查一下返回值，是不是实体类、接口、或类型模板参数，或其Option、Collection、List、Set包装类
     */
    private Pair<Class, Class> checkAndGetReturnType(Method method) {

        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {

            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;

            //只允许有且只有一个参数化模板，比如，只接受List<Student>
            if (parameterizedType.getActualTypeArguments().length != 1) {
                throw new RuntimeException(String.format("%s.%s()的返回值有多个类型化参数", method.getDeclaringClass().getName(), method.getName()));
            }
            Class rawType = (Class) parameterizedType.getRawType();
            if (rawType != Collection.class && List.class != rawType && Set.class != rawType && rawType != Optional.class) {
                throw new RuntimeException(String.format("%s.%s()的返回格式不支持，仅支持List、Collection、Set和Optional，以及原始类", method.getDeclaringClass().getName(), method.getName()));
            }

            // 如果返回了一个类型模板参数的包装类，那么还要检查其参数类型，是不是有且只有一个Class参数，并且还有
            if (parameterizedType.getActualTypeArguments()[0] instanceof TypeVariable) {
                checkTypeVariable((TypeVariable) parameterizedType.getActualTypeArguments()[0], method);
                return new Pair<>(rawType, null);
            }
            return new Pair<>(rawType, (Class) parameterizedType.getActualTypeArguments()[0]);
        } else if (genericReturnType instanceof TypeVariable) {
            checkTypeVariable((TypeVariable) genericReturnType, method);
            return new Pair<>(null, null);
        } else return new Pair<>(null, (Class) genericReturnType);

    }

    private void checkTypeVariable(TypeVariable genericReturnType, Method method) {

        List<Class> classes = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType() == Class.class) {
                classes.add(parameter.getType());
            }
        }
        if (classes.size() != 1) {
            throw new RuntimeException(String.format("%s.%s()缺少Class类型的参数，或有两个以上的class参数", method.getDeclaringClass().getName(), method.getName()));
        } else {
            if (classes.get(0).getTypeParameters().length != 1) {
                throw new RuntimeException(String.format("%s.%s()的Class类型参数%s没有模板化参数，或有两个以上模板化参数", method.getDeclaringClass().getName(), method.getName(), classes.get(0).getName()));
            } else if (classes.get(0).getTypeParameters()[0].getClass() != genericReturnType.getClass()) {
                throw new RuntimeException(String.format("%s.%s()的Class类型参数%s的模板化参数与返回值不匹配", method.getDeclaringClass().getName(), method.getName(), classes.get(0).getName()));
            }
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
                throw new RuntimeException(String.format("%sImpl没有找到，请检查", otherInterface.getName()), e);
            }
        }
        return interface2ImplInstanceMap.get(otherInterface);
    }

}
