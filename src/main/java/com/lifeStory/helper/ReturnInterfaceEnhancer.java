package com.lifeStory.helper;

import com.lifeStory.utils.NameUtil;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Map;

public class ReturnInterfaceEnhancer implements MethodInterceptor {

    private Object target;
    private Map<String, Object> valueStore;

    public Object getInstance(Object target) {
        this.target = target;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(this.target.getClass());
        enhancer.setCallback(this);
        return enhancer.create();
    }

    // 实现回调方法，我们的逻辑是根据方法名来判断
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) {
        return valueStore.get(NameUtil.firstCharToLowerCase(method.getName().replaceFirst("^get", "")));
    }

    public ReturnInterfaceEnhancer setValueStore(Map<String, Object> valueStore) {
        this.valueStore = valueStore;
        return this;
    }

}
