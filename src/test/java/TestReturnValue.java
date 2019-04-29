import com.lifeStory.model.Student;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class TestReturnValue {

    <T> List<T> getStudents(Class<T> type) {
        return null;
    }

    List getLists() {
        return null;
    }

    @Test
    public void main() {
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

}
