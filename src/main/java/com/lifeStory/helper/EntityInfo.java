package com.lifeStory.helper;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class EntityInfo {

    private Class entityClass;
    private String entityDbName;
    private Map<String, String> filedName2DbName = new HashMap<>();
    private final Map<String, Class> fieldName2Type = new HashMap<>();
    private String idDbName;

}
