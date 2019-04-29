package com.lifeStory.utils;

import javax.persistence.Table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;

public class NameUtil {

    public static String getEntityDbName(Class aClass) {
        return aClass.isAnnotationPresent(Table.class)
                ? ((Table) aClass.getDeclaredAnnotation(Table.class)).name()
                : getDataBaseName(aClass.getSimpleName());
    }

    public static String getDataBaseName(String javaName) {
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

    private static String firstCharToLowerCase(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.substring(0, 1).toLowerCase(ENGLISH) + name.substring(1);
    }

    /**
     * 只实现一个简版的，示例而已，即只允许find[get]XXXByName1AndName2(param1,param2)这种形式
     */
    public static List<String> getFiledNamesByMethodName(String methodName) {
        String removedStart = methodName.replaceFirst("^(find|get)\\w?By", "");
        return Arrays.stream(removedStart.split("And")).map(NameUtil::firstCharToLowerCase).collect(Collectors.toList());
    }
}
