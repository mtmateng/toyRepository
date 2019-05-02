package com.toySpring.repository.helper;

import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;

@Data
public class SQLMethodInfo {

    private Method method;
    private String SQLTemplate;
    private Class<?> wrapClass;
    private Class<?> actualClass;
    private List<String> selectFieldsNames;
    private List<String> queryFieldsNames;

}
