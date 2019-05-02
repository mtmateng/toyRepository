package com.lifeStory.helper;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class SQLMethodInfo {

    private Method method;
    private String SQL;
    private Class<?> wrapClass;
    private Class<?> actualClass;

}
