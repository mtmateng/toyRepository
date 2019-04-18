package com.lifeStory;

import com.lifeStory.repository.BaseRepository;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositoryHandler<T, ID> implements InvocationHandler {

    private final BaseRepository<T, ID> baseRepository;
    private final Map<String, Class> fieldName2Type = new HashMap<>();

    public RepositoryHandler(BaseRepository<T, ID> baseRepository) {
        this.baseRepository = baseRepository;

        Field[] fields = baseRepository.getDomainClass().getFields();
        for (Field field : fields) {
            fieldName2Type.put(field.getName(), field.getType());
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) {
        Object retValue = null;
        String methodName = method.getName();
        List<String> fieldNames = getFiledNames(methodName);
        checkParams(fieldNames, args, methodName);
        String entityName = baseRepository.getDomainClass().getName().toLowerCase();
        String sql = generateSql(entityName, fieldNames);

    }

    private void checkParams(List<String> fieldNames, Object[] args, String methodName) {

        if (fieldNames.size() != args.length) {
            throw new RuntimeException("参数数量错误：" + methodName);
        }
        for (int i = 0; i != args.length; ++i) {
            if (fieldName2Type.get(fieldNames.get(i)) == null ||
                !fieldName2Type.get(fieldNames.get(i)).equals(args[0].getClass())) {
                throw new RuntimeException("第" + i + "个参数类型错误：" + methodName);
            }
        }

    }

    private String generateSql(String entityName, List<String> fieldNames) {
        StringBuilder sql = new StringBuilder()
            .append("select * from ")
            .append(entityName.toLowerCase())
            .append(" where");
        for (String fieldName : fieldNames) {
            sql.append(" ").append(fieldName.toLowerCase()).append("=? and");
        }
        return sql.toString().replaceFirst("and$", "");
    }

    /**
     * 只实现一个简版的，示例而已，即只允许find[get]XXXByName1AndName2(param1,param2)这种形式
     */
    private List<String> getFiledNames(String methodName) {
        String removedStart = methodName.replaceFirst("^(find|get)[\\w+]By", "");
        return Arrays.asList(removedStart.split("And"));
    }

}
