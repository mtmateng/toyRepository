package com.toySpring.repository.helper;

import lombok.Data;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Data
public class EntityInfo {

    private Class entityClass;
    private String idFieldName = null;
    private String idDBName = null;
    private String entityDBName;
    private Map<String, Field> filedName2Field = new HashMap<>();
    private Map<String, String> filedName2DBName = new HashMap<>();

}
