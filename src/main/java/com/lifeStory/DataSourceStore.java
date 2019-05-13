package com.lifeStory;

import com.toySpring.repository.utils.ToyDataSourceBuilder;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class DataSourceStore {

    private static final Map<String, DataSource> datasourceMap = new HashMap<>();

    // 这里又是一个WorkAround，为了支持数据库方言。不同的关系数据库需要的SQL语句不完全相同，Hibernate也有dialect的选项
    // 但Spring的DataSource是依靠其依赖注入机制管理的，具体工作又委托给Hibernate（或其他Mybatis）来做，这两个框架都
    // 可以解析配置文件来获取方言种类。但我们在这里如果由用户直接提供一个Datasource，因为Datasource接口本身的方法特别少
    // 无法获得足够的信息知道数据库的种类，显然就不知道该用哪种方言了，为避免这种问题，在这里要求用户用我们的框架中的方法来
    // 生成Datasource，这样我们就可以根据URL，确定数据库的方言并将其注册到方言的Factory中了

    // 有意思的是，我查看了Spring的源代码，发现其对多数据源的支持竟然是差不多的，也是先getClass，然后通过class的name
    // 来精确匹配，看dataSource是什么类型的，在Spring1.x版本的DataSourceBuilder类中，也是通过url猜测driverName
    // 之类的，之后有个bind操作，看起来也是执行了一些生成Datasource以外的事情，总之并没有多么高大上。
    static {
        datasourceMap.put("student", ToyDataSourceBuilder.buildDataSource("toySpring.datasource"));
    }

    static {
        datasourceMap.put("case", ToyDataSourceBuilder.buildDataSource("toySpring.datasource2"));
    }

    public static DataSource getDataSource(String name) {

        return Optional.ofNullable(datasourceMap.get(name)).orElseThrow(() -> new RuntimeException("名为：" + name + "的dataSource不存在"));

    }


}
