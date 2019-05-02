package com.toySpring.repository.helper;

import com.toySpring.repository.utils.NameUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public class ReturnValueHandler implements InvocationHandler {

    private Map<String, Object> valueRepo;

    public ReturnValueHandler(Map<String, Object> valueRepo) {

        this.valueRepo = valueRepo;

    }

    public Object invoke(Object proxy, Method method, Object[] args) {

        return valueRepo.get(NameUtil.firstCharToLowerCase(method.getName().replaceFirst("^get", "")));

    }

}
