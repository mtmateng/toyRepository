package com.lifeStory.utils;

import javax.persistence.Table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;

public class NameUtil {

    static String getEntityDbName(Class aClass) {
        return aClass.isAnnotationPresent(Table.class)
                ? ((Table) aClass.getDeclaredAnnotation(Table.class)).name()
                : getDataBaseName(aClass.getSimpleName());
    }

    static String getDataBaseName(String javaName) {
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

    static String firstCharToLowerCase(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.substring(0, 1).toLowerCase(ENGLISH) + name.substring(1);
    }

}
