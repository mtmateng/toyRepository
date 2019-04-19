package com.lifeStory;

import com.lifeStory.baseRepository.BaseRepository;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;

public class RepositoryHandler<T, ID> implements InvocationHandler {

    private final BaseRepository<T, ID> baseRepository;
    private final Map<String, Class> fieldName2Type = new HashMap<>();
    private final DataSource dataSource;

    public RepositoryHandler(BaseRepository<T, ID> baseRepository,
                             DataSource dataSource) {
        this.baseRepository = baseRepository;
        this.dataSource = dataSource;

        Field[] fields = baseRepository.getDomainClass().getDeclaredFields();
        for (Field field : fields) {
            fieldName2Type.put(field.getName(), field.getType());
        }
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        String methodName = method.getName();
        List<String> fieldNames = getFiledNames(methodName);
        checkParams(fieldNames, args, methodName);
        String entityName = baseRepository.getDomainClass().getSimpleName();
        String sql = generateSql(entityName, fieldNames, args);

        // 这样实现首先把整个resultSet都解析了，再去判断方法的返回值是不是collection，效率不高，不过反正是个玩具，说明意思就可以了。
        List<T> results = executeSql(sql);
        if (!Collection.class.isAssignableFrom(method.getReturnType()) && results.size() > 1) {
            throw new RuntimeException(String.format("%s超过一个返回结果，但该方法的返回值：%s只能容纳一个结果",
                    method.getName(), method.getReturnType().getName()));
        }
        if (method.getReturnType() == Optional.class) {
            if (results.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(results.get(0));
            }
        }
        if (method.getReturnType() == baseRepository.getDomainClass()) {
            return results.get(0);
        }
        if (Collection.class.isAssignableFrom(method.getReturnType())) {
            Collection ret;
            if (List.class.isAssignableFrom(method.getReturnType())) {
                ret = new ArrayList();
            } else if (Set.class.isAssignableFrom(method.getReturnType())) {
                ret = new HashSet();
            } else throw new RuntimeException("集合类型当前只支持List和Set");
            ret.addAll(results);
            return ret;
        }
        throw new RuntimeException("返回值类型不支持，无法匹配");
    }

    @SuppressWarnings("unchecked")
    private List<T> executeSql(String sql) throws Exception {

        List<T> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            T result;
            try {
                result = baseRepository.getDomainClass().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("需要默认构造函数");
            }
            while (resultSet.next()) {
                for (String fieldName : fieldName2Type.keySet()) {
                    Class<T> type = fieldName2Type.get(fieldName);
                    PropertyDescriptor prop = new PropertyDescriptor(fieldName, baseRepository.getDomainClass());
                    switch (type.getName()) {
                        case "java.lang.String":
                            prop.getWriteMethod().invoke(result, resultSet.getString(getDataBaseName(fieldName)));
                            break;
                        case "java.lang.Integer":
                            prop.getWriteMethod().invoke(result, resultSet.getInt(getDataBaseName(fieldName)));
                            break;
                        case "java.time.LocalDate":
                            Date date = resultSet.getDate(getDataBaseName(fieldName));
                            //真不敢相信Date以前竟然是java标准库的一部分，设计这个的程序员可能是临时工？
                            LocalDate localDate = LocalDate.of(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
                            prop.getWriteMethod().invoke(result, localDate);
                            break;
                        default:
                            throw new UnsupportedOperationException("尚未支持该类型");
                    }
                }
                results.add(result);
            }

        } catch (SQLException e) {
            throw new RuntimeException("发生错误", e);
        }
        return results;
    }

    private void checkParams(List<String> fieldNames, Object[] args, String methodName) {

        if (args == null || fieldNames.size() != args.length) {
            throw new RuntimeException("参数数量错误：" + methodName);
        }
        for (int i = 0; i != args.length; ++i) {
            if (fieldName2Type.get(fieldNames.get(i)) == null ||
                    !fieldName2Type.get(fieldNames.get(i)).equals(args[0].getClass())) {
                throw new RuntimeException("第" + i + "个参数类型错误：" + methodName);
            }
        }

    }

    private String generateSql(String entityName, List<String> fieldNames, Object[] args) {
        StringBuilder sql = new StringBuilder()
                .append("select * from ")
                .append(getDataBaseName(entityName))
                .append(" where");
        for (int i = 0; i != fieldNames.size(); ++i) {
            sql.append(" ").append(getDataBaseName(fieldNames.get(i))).append("=");
            if (fieldName2Type.get(fieldNames.get(i).toLowerCase()) == String.class
                    || fieldName2Type.get(fieldNames.get(i).toLowerCase()) == LocalDate.class) {
                sql.append("'").append(args[i].toString()).append("'").append(" and");
            } else {
                sql.append(args[i].toString()).append(" and");
            }

        }

        return sql.toString().replaceFirst(" and$", "");
    }

    /**
     * 将驼峰命名换成下划线命名，如果字符串第一个字符刚好大写，则只小写，不加下划线
     */
    private String getDataBaseName(String javaName) {
        if (javaName == null || javaName.isEmpty()) {
            return javaName;
        }
        javaName = javaName.substring(0, 1).toLowerCase(ENGLISH) + javaName.substring(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i != javaName.length(); ++i) {
            if (Character.isUpperCase(javaName.charAt(i))) {
                sb.append('_').append(Character.toLowerCase(javaName.charAt(i)));
            } else sb.append(javaName.charAt(i));
        }
        return sb.toString();
    }

    private String firstCharToLowerCase(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.substring(0, 1).toLowerCase(ENGLISH) + name.substring(1);
    }

    /**
     * 只实现一个简版的，示例而已，即只允许find[get]XXXByName1AndName2(param1,param2)这种形式
     */
    private List<String> getFiledNames(String methodName) {
        String removedStart = methodName.replaceFirst("^(find|get)\\w?By", "");
        return Arrays.stream(removedStart.split("And")).map(this::firstCharToLowerCase).collect(Collectors.toList());
    }

}
