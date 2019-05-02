package com.toySpring.repository.helper;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class EntityInfo {

    private Class entityClass;
    private String idFieldName = null;
    private String idDBName = null;
    private String entityDBName;
    private Map<String, String> filedName2DBName = new HashMap<>();
    private Map<String, Class> fieldName2Type = new HashMap<>();

}