package com.lifeStory.utils;

import javax.persistence.Table;

import static java.util.Locale.ENGLISH;

public class NameUtil {


    public static String getEntityName(Class aClass) {
        return aClass.isAnnotationPresent(Table.class)
                ? ((Table) aClass.getDeclaredAnnotation(Table.class)).name()
                : getDataBaseName(aClass.getSimpleName());
    }

    private static String getDataBaseName(String javaName) {
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
}
