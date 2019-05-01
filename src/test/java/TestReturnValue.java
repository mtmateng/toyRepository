import com.lifeStory.model.Student;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestReturnValue {

    <T> List<T> getStudents(Class<T> type) {
        return null;
    }

    List getLists() {
        return null;
    }

    @Test
    void main() {
        for (Method declaredMethod : TestReturnValue.class.getDeclaredMethods()) {
            Type type = declaredMethod.getGenericReturnType();
            if (type instanceof ParameterizedType) {
                System.out.println(type);
                for (Type actualTypeArgument : ((ParameterizedType) type).getActualTypeArguments()) {
                    System.out.println(actualTypeArgument);
                }
                System.out.println(((ParameterizedType) type).getRawType());
            }
        }
    }

    @Test
    void main23() {
        Map<String, String> map = new HashMap<>();
        map.put("abc", null);
        map.putIfAbsent("abc", "cdc");
        System.out.println(map.size());
        System.out.println(map.get("abc"));
    }

}
