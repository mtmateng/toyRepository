package com.lifeStory.utils;

import com.lifeStory.helper.EntityInfo;
import sun.jvm.hotspot.utilities.Assert;

import javax.sql.DataSource;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SQLUtil {

    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE IF NOT EXISTS `%s` (%s %s) %s %s;";

    static String generateCreateTableSql(EntityInfo entityInfo) {

        StringBuilder fieldClaus = new StringBuilder();
        for (String fieldName : entityInfo.getFieldName2Type().keySet()) {
            fieldClaus.append(entityInfo.getFiledName2DBName().get(fieldName))
                .append(" ")
                .append(generateDBType(entityInfo.getFieldName2Type().get(fieldName)))
                .append(", ");
        }
        return String.format(CREATE_TABLE_TEMPLATE, entityInfo.getEntityDBName()
            , fieldClaus.toString(), getPrimaryKeyClaus(entityInfo.getIdDBName()),
            getEngineClaus(), getCharSetClaus());

    }

    /**
     * 根据method的相关参数，来生成sql的template String
     */
    public static String buildSqlTemplate(Method method, EntityInfo entityInfo) {

        List<String> selectFieldNames = getSelectFieldNamesByMethod(method);
        List<String> queryFieldNames = getQueryFiledNamesByMethodName(method.getName());
        checkSelectParams(selectFieldNames, method, entityInfo);
        checkQueryParams(queryFieldNames, method, entityInfo);
        //todo
        return null;
    }

    /**
     * 检查select的字段名和实体类字段是否匹配
     */
    private static void checkSelectParams(List<String> selectFieldNames, Method method, EntityInfo entityInfo) {

        for (String selectFieldName : selectFieldNames) {
            if (!entityInfo.getFieldName2Type().keySet().contains(selectFieldName)) {
                throw new RuntimeException(String.format("%s方法要求的字段值%s与实体%s中的字段不匹配", method.getName(), selectFieldName, entityInfo.getEntityDBName()));
            }
        }

    }

    //检查一下方法名、实体类字段类型和参数类型是否匹配
    private static void checkQueryParams(List<String> queryFieldNames, Method method, EntityInfo entityInfo) {

        if (queryFieldNames.size() != method.getParameters().length) {
            throw new RuntimeException("参数数量错误：" + method.getName());
        }

        // 检查一下参数类型和Entity类型是否匹配
        for (int i = 0; i != method.getParameters().length; ++i) {
            if (entityInfo.getFieldName2Type().get(queryFieldNames.get(i)) == null ||
                !entityInfo.getFieldName2Type().get(queryFieldNames.get(i)).equals(method.getParameters()[i].getClass())) {
                throw new RuntimeException("第" + i + "个参数类型错误：" + method.getName());
            }
        }

    }

    /**
     * 先看参数值里有没有Class类型，有的话就以此建立sql语句的select字句
     * 如果没有，再看返回值是不是interface
     */
    private static List<String> getSelectFieldNamesByMethod(Method method) {

        // 先看看参数中是不是有class类型
        List<Parameter> classParameters = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType() == Class.class) {
                classParameters.add(parameter);
            }
        }

        if (classParameters.size() > 1) {
            throw new RuntimeException(String.format("方法%s有%d个类型化参数", method.getName(), classParameters.size()));
        } else if (classParameters.size() == 1) {
            checkReturnType(method, classParameters.get(0).getType(), method.getGenericReturnType());
            return Arrays.stream(classParameters.get(0).getType().getFields()).map(Field::getName).collect(Collectors.toList());
        }

        // 再看是不是返回一个接口，或接口的Collection（只接受collection或者List或者Set）
        if (method.getReturnType().isInterface()) {
            return Arrays.stream(method.getReturnType().getDeclaredMethods())
                .map(Method::getName).map(name -> name.replaceFirst("^get", ""))
                .map(NameUtil::firstCharToLowerCase).collect(Collectors.toList());
        }
        // 否则直接所有的字段值
        return null;
    }

    private static void checkReturnType(Method method, Class<?> type, Type genericReturnType) {

        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            if (parameterizedType.getActualTypeArguments().length != 1) {
                throw new RuntimeException(String.format("%s.%s有多个类型化参数", method.getDeclaringClass().getName(), method.getName()));
            }
            if (type != parameterizedType.getActualTypeArguments()[0]) {
                throw new RuntimeException(String.format("%s.%s返回值类型错误", method.getDeclaringClass().getName(), method.getName()));
            }
        }

    }

    /**
     * 只实现一个简版的，示例而已，即只允许find[get]XXXByName1AndName2(param1,param2)这种形式
     */
    private static List<String> getQueryFiledNamesByMethodName(String methodName) {
        String removedStart = methodName.replaceFirst("^(find|get)\\w?By", "");
        return Arrays.stream(removedStart.split("And")).map(NameUtil::firstCharToLowerCase).collect(Collectors.toList());
    }

    private static String getPrimaryKeyClaus(String idDBName) {
        return String.format("PRIMARY KEY (%s)", idDBName);
    }

    //这都是数据库相关的，这里先不做定制化处理了
    private static String getEngineClaus() {
        return "";
    }

    private static String getCharSetClaus() {
        return "DEFAULT CHARSET=utf8mb4";
    }

    /**
     * 产生需要的type字句，类似于varchar(255)这样子的东西，显然，我们这里支持的相当有限，也不支持Lob、自定义长度等
     * 意思到了就行
     */
    private static String generateDBType(Class aClass) {
        switch (aClass.getName()) {
            case "java.lang.String":
                return "VARCHAR(255)";
            case "java.lang.Integer":
                return "INT";
            case "java.time.LocalDate":
                return "DATE";
            default:
                throw new UnsupportedOperationException("尚未支持该类型");
        }
    }

    static void executeSql(String sql, DataSource dataSource) {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("执行%s失败", sql), e);
        }
    }
}
