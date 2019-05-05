## 一个Spring Repository的简易实现
（所以我命名为ToySpringRepository）

### Spring Repository的黑魔法简介

1. 允许只有接口不提供实现，只要扩展`Repository`及其子接口，就具有基本的CRUD功能，比如

   `public interface StudentRepository extends CRUDRepository<Student, Integer>`

   该接口即可直接用来做Student实体的增删改查

2. 允许在接口中直接声明方法名来完成查询，比如：

   `List<Student> findByName(String name);`

   `List<Student> findByNameLikeAndGender(String name, String gender);`

3. 允许通过指定返回值为接口，或者运行时提供Class参数，来只查询部分字段。

   假设我们有Student实体，字段如下：

   ```java
   public class Student {
       @Id
       Integer id;
       String name;
       String gender;
       LocalDate birthday;
   }
   ```

   我们声明一个接口：

   ```java
   public interface StudentVo {
       Integer getId();
       String getName();
   }
   ```

   则可以在上述的`StudentRepository`中声明以下方法：

   `StudentVo findByName(String name);`

   即可完成只查询部分字段的功能，或：

   创建一个类，这个类需要有一个所有字段包含在内的构造函数(我们在这里通过lombok插件的@Value注解完成这个工作)

   ```java
   @Value
   public class StudentVoClass {
       private String name;
   }
   ```

   则可以在上述的`StudentRepository`中声明以下方法：

   `<T> List<T> findByGender(String gender, Class<T> type);`

   在运行时将`StudentVoClass.class`作为参数传入，这样也可以实现只查询部分字段。

4. 允许丰富的自定义返回值，Spring都可以handle住。

   比如我们声明一个方法：`findById(ID id);`那么其返回值既可以是`List<T>`，还可以是`Set<T>`，还可以是`Optional<T>`，当然也可以是`T`，Spring会处理好返回值的细节。只是，当用`Optional<T>`或`T`来接返回值时，返回值超过一个时会抛异常。

5. 允许用户的Repository继承两个接口，另一个接口可以有一个实现，该实现的命名在默认情况下必须是业务Repository名+Impl，通过这种方式，Spring在运行时可以调用这个实现的方法，如：

   ```java
   public interface StudentRepository extends 
     Repository<Student, Integer>, StudentInterface {
   	...
   }
   ```

   StudentInterface本身如下：

   ```java
   public interface StudentInterface {
       Map<String, Object> getMapById(Integer id); //返回字段名到字段值的map
   }
   ```

   提供实现如下：

   ```java
   public class StudentRepositoryImpl implements StudentInterface {
   
       public Map<String, Object> getMapById(Integer id) {
   				// do sth, anything here
       }
   
   }
   ```

   则在代码中可以这样写：

   ```java
   studentRepository.getMapById(1).forEach((k, v) -> System.out.println(String.format("key=%s,value=%s", k, v)));
   ```

   Spring会调用Impl中的方法。

6. 上面的第4条显然已经提供了一个自定义Repository的方法，但是，假如我们需要更大范围的自定义，比如我们希望为所有的Repository提供一个公共方法。那么我们可以替换BaseRepository。

   这里需要了解一些基础知识，即所有Spring提供的Repository接口，其实现都在SimpleJpaRepository中，该class实现了所有的Repository interface的方法，因此我们使用的那些默认Spring Repository方法，其实运行时实际调用的都是SimpleJpaRepository中的对应方法。

   因此，我们只需要替换掉这个SimpleJpaRepository，换上我们自己的实现，并且提供一个新的接口给我们的实际Repository使用，即可完成为所有实际Repository添加公共方法。

   Spring通过`@EnableJpaRepository`注解中的自定义字段值，为用户提供了修改BaseRepository的入口，比如我们在Spring的主类上增加这样的注解：

   ```java
   @EnableJpaRepositories(repositoryBaseClass = BatchRepositoryImpl.class)
   public class MyApplication {
   	public static void main(String[] args) {
   		SpringApplication.run(MyApplication.class, args);
   	}
   }
   ```

   然后声明一个这样的接口，该接口需要标注`@NoRepositoryBean`，但并不一定必要实现Spring的Repository系列接口，只是实现了的话就可以在我们的业务Repository直接使用这些方法，比较方便而已。

   ```java
   @NoRepositoryBean
   public interface BatchRepository<T, ID extends Serializable>
           extends CrudRepository<T, ID> {
   
       int batchInsert(List<T> list);
   }
   ```

   在提供基础实现如下，注意这个类需要扩展SimpleJpaRepository，实现我们上面的接口

   ```java
   public class BatchRepositoryImpl<T, ID extends Serializable>
       extends SimpleJpaRepository<T, ID> implements BatchRepository<T, ID> {
   
       public BatchRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
           super(entityInformation, entityManager);
       }
   
       public int batchInsert(List<T> list) {
           // do sth, any thing here
       }
   }
   ```

   之后我们的业务Repository这样声明：

   ```java
   public interface StudentRepository extends BatchRepository<Student, Integer> {
     
   }
   ```

   即可使用`batchInsert()`这个方法。

7. 允许在一个项目里使用多数据源，虽然**官方并不建议那么做，这意味着你的微服务太过复杂，应该重新考虑微服务架构设计**，但仍然支持。多数据源的支持比较复杂，这里不做演示。

   简单来讲，还是通过`@EnableJpaRepository`中的值来自定义化，你需要提供一个数据源，需要指定哪些实体由哪个数据源来管理，哪些Repository对应哪些数据源等。这里不赘述了。

8. 可以根据不同的数据源，选择不同的数据库方言，比如，Oracle和Mysql对于如何实现分页的具体语句就不一样，因此生成的语句也会不一样。

9. 超级丰富的表关联查询。这应该属于上述功能3的一部分，但因为这个功能很好地完成了表关联，其实是对JPA规范的实现，相当复杂，我们并没有实现，因此在这里单独列出来。

### 这个项目完成的功能

实现了上述的1、2、3、4、5、6、7这几个功能。

方言方面，计划以后提供一个接口，方便替换方言。目前因为项目用的是H2内存数据库，也就只是实现了能够在H2数据库上运行起来的方言。

需要注明的是，Spring的核心功能是依赖注入和面向切面编程，因此Spring Repository项目中也大量使用了依赖注入和面向切面的思想和功能，比如DataSource的注入，事务的管理等，我在这里显然没有精力实现，因此有大量的实现都是简化版，也不优雅。但尽量将不同的功能放到不同的模块里面去，以便代码结构比较清晰，修改起来也比较友好。

### 主要代码结构

1. `com.toySpring.repository.baseRepository`

   这个包里有三个类：

   + Repository，对应Spring中`Repository`系列标记接口。它的所有接口方法最终实现于同一个包下面的`BaseRepository`类中。
   + BaseRepository。对应Spring中的`SimpleJPARepository`，是所有Repository接口的实现类，实现了`Repository`系列标记接口的所有方法。当然啦， 在我这个小项目里，这个系列接口只有`Repository`一个接口。当用户想要提供自定义的BaseRepository方法时，必须扩展这个类。
   + BaseRepositoryFactory。当我们要用自定义的BaseRepository来替代原有的BaseRepository时，可以很容易地想到使用工厂方法，而我们的这个类也就是为了生产BaseRepository的。

2. `com.toySpring.repository.custom`

   这个包是Spring中`@EnableJPARepository`的替代品，考虑到如果我要使用注解的话，配多数据源就需要多个注解，但一个类上面又不能标记重复注解，所以我就需要至少两个类，Spring是依靠它本身的依赖注入功能，结合`@Configuration`注解一起实现的多`@EnableJPARepository`注解，从而实现了多数据源。但我们显然很难参考这种方法，因为这样的话我们就必须实现Spring框架的很多功能。因此，我们这里采取了一种更简便的方式，即提供了一个`com.toySpring.repository.custom.CustomRepoSetting`类，如下：

   ```java
   public class CustomRepoSetting {
       private String entityPackage = null;
       private String repositoryPackage = null;
       private String dataSourceName = "default";
       private Class<? extends BaseRepository> baseRepositoryClass = null;
   ·}
   ```

   这个类的四个字段，分别对应了

   + 需要扫描的实体类，如`Student`在哪个包里，如未提供，则默认扫描用户的main类所在包及其子包。
   + 需要扫描的Repository，如`StudentRepository`在哪个包里，如未提供，则默认扫描用户的main类所在包及其子包。注意要和entityPackage中的entity对应起来，意思是说，假如这个包里有一个Repository，其管理的实体类是`Student`，那么`Student`必须在`entityPackage`里。
   + dataSource名，即用于提供的dataSource的名字，如未提供，默认是default，我们的项目会从用户提供的类里面查找这个dataSource名，具体见`com.toySpring.repository.utils.RepoStore`。用户必须保证这个名字的dataSource存在。
   + BaseRepository类，即用于提供`BaseRepository`，即用户的自定义的`BaseRepository`，默认值是上面的`com.toySpring.repository.baseRepository.Repository`类。

3. `com.toySpring.repository.helper`

   一些重要的辅助类：

   + EntityInfo

     每一个受管理的实体，比如`Student`，都会有这样的一个类来描述它。包括实体类的class，id字段在实体类中的名字，id在数据库中的字段名，实体类在数据库中的名字，实体类字段名到数据库中字段名的映射，字段名到字段类型的映射。实体类中字段名到数据库中的字段名，是按照驼峰进行解析的，比如一个实体类，名字是`CaseEntity`，那么它在数据库中的表名会被解析为case_entity。

     同时这里也是留了一个接口，把所有实体信息的解析放在一个地方集中完成，之后要支持自定义时，比如像Spring那样支持`@Column`自定义字段名，`@Table`指定表名，都可以在一处集中完成，不需要在整个代码里搜索改来改去。

   + SQLMethodInfo

     对于每一个定义在业务Repository中的方法，都会生成这样的一个类来描述它。包括Method、该Method生成的SQLTemplate、返回值的容器类(如Optional、List、Set等），返回值的真实类，如Student，StudentVo等，从返回值等解析出来的select子句中需要选择的字段，从方法名中解析的query子句中where的字段。

     把这些东西都集中在这里，也是为了便于管理，减少修改的影响面。

   + RepositoryHandler

     即用户Repoository的动态代理的Handler。这个比较复杂，但阅读源码就容易理解是干嘛的了。

   + ReturnValueHandler

     即当用户Repository的方法中使用接口作为返回值(如上面的StudentVo)，这是返回值的动态代理handler。

4. `com.toySpring.repository.utils`

   一些工具类。但显然我在起名上面过于随意了，这个包里面的东西比较混乱，什么只要想不明白该放到哪里，就先放到这里了。

   + ClassUtils

     用来搜索包里面的各种类的。因为毕竟我们要扫描包以获得Entity和Repsitory不是。

   + EntityUtil

     扫描并构建EntityInfo的地方。

   + NameUtil

     完成实体类字段名到数据库字段名转换的Util

   + RepoStore

     这个类似于Spring的ApplicationContext，超级超级简化版。实际上就是一个Map，建立了业务Repository的Class和Instance(实际是动态代理)之间的映射。当用户需要Repistory时，只需要像下面这样就可以了。

     ```java
     StudentRepository studentRepository = repoStore.getRepository(StudentRepository.class);
     ```

     它的构造函数接受两个类参数，以及一个可变长参数：

     ```java
     public RepoStore(Class mainClass, Class<?> dataSourceStore, CustomRepoSetting... customRepoSettings) {}
     ```

     + 第一个参数：用户的Main类的Class，即用户的`public static void main(String[] args) `方法坐在的类，当用户未提供`CustomRepoSetting`时，我们的项目用到了这个来解析默认的`entityPackage`和`repositoryPackage`。

     + 第二个参数：提供DataSource的类，这个类不需要继承任何类，必须有一个静态方法，其声明必须如下：

       ```java
       public static DataSource getDataSource(String name) {}
       ```

       我们的项目通过反射获得这个方法，用`CustomRepoSetting`中的`dataSourceName`作为参数调用之，获得一个Datasource。

     + 可变长参数，即用户自定义的配置，可以替换数据源、替换BaseRepository等。

5. SQLUtil

   即具体生成SQL语句、执行SQL语句的类。

   目前我们对数据库方言没有支持，只支持了H2的方言(其实是Mysql兼容模式的H2)。

   以后如果要支持方言，在这里修改增加就行了。目前这里的方法都写成了静态方法，显然是无法支持方言了……以后有机会修改吧。

### 主要实现思路

1. 运行时动态生成接口的实现

   既然是只有接口没有实现，那么Spring必然在某个地方使用了`动态代理`。

   本项目中使用动态代理主要干了这么几件事情：

   + 为业务Repository生成代理，代理的`Handler`是`com.toySpring.repository.helper.RepositoryHandler`，生成代理的代码在`com.toySpring.repository.utils.RepoStore`的`initStore()`方法中，具体如下：

     ```java
     RepositoryHandler<?, ?> repositoryHandler = buildRepositoryHandler(entityClass, idClass, aClass, baseRepoClass, entityUtil, dataSource);
     Repository repository = aClass.cast(Proxy.newProxyInstance(
         aClass.getClassLoader(), new Class<?>[]{aClass, Repository.class}, repositoryHandler
     ));
     ```

   + 为业务Repository返回的接口生成代理，代理的`Handler`是`com.toySpring.repository.helper.ReturnValueHandler`，生成代理的代码在`com.toySpring.repository.utils.SQLUtil`的`getResultInstance()`方法中，具体如下：

     ```java
     return resultClass.cast(Proxy.newProxyInstance(
         resultClass.getClassLoader(), new Class<?>[]{resultClass}, new ReturnValueHandler(sqlResultMap)
     ));
     ```

2. 根据方法名生成SQL的解析：

   在`RepositoryHandler`初始化时，扫描客户Repository中的所有声明的方法，对其中的find方法，先检查其声明的合法性，比如`findByName`，却提供了两个参数，或者提供的参数是`Integer`类型的，这都不允许，检查完之后，直接生成了SQL的template语句，即类似

   ```sql
   SELECT name from student where name = '%s';
   ```

   把SQL模板的生成放在这里进行，有两个好处，一个是在初始化时就可以检查用户声明的正确性，减少用户运行时出错的机会；二是可以减少运行时解析方法名的开销，直接用SQL模板套上参数就可以运行了。

   对于每个解析后的方法，我们生成了一个`com.toySpring.repository.helper.SQLMethodInfo`类，里面描述了这个方法select了哪些字段，query by哪些字段，返回值是什么容器类（如Optional、List），什么实际类（如上面的StudentVo，Student）等，以便在运行时调用该方法，不需要对方法再进行任何解析。

   当然，传入Class参数的方法，如上文所述的`<T> List<T> findByGender(String gender, Class<T> type);`，其解析需要推迟到运行时才能进行，因为我们尚且不知道哪些具体的类会被传进来，但只要进行一次，我们也保存相应的`SQLMethodInfo`信息，以加速之后的访问。

3. 自定义方法。

   我们基于这样的一个事实，即所有声明在业务Repository扩展的接口中的方法，我们都提供了实现。比如我们上面的这个声明：

   ```java
   public interface StudentRepository extends 
     Repository<Student, Integer>, StudentInterface {
   	...
   }
   ```

   其扩展的两个接口中的方法，其中继承自Repository中的方法，实现在`BaseRepository`中，继承自`StudentInterface`的方法，实现在`StudentRepositoryImpl`中。

   因此，对于这些预定义的方法，我们在初始化`RepositoryHandler`时，建立了一个声明的`Method`到这些`Method`实现的映射，当`invoke()`函数被调用时，如果先查到了这些函数，那么直接调用其实现，如果没有查到，再去查他们的SQL语句，还未查到，那就现场解析。

4. 多数据源的准备

   Spring本身是**约定大于配置**，约定了很多内容，因此我这里也不要脸地用了一些约定

   在多数据源的实现上，Spring是通过`@EnableJpaRepository`来定制化，我这里使用了一个

   `com.toySpring.repository.custom.CustomRepoSetting`来实现的，可以在这里面指定要扫描的`Entity`所在包名，`Repository`所在包名，以及`DataSource`的名字，之后传给`RepoStore`来生成我们需要的业务Repository代理类。

   其中，DataSource必须由用户预定义好，定义在什么类里面不重要，但这个类必须有一个静态方法，其声明为`public static DataSource getDataSource(String name) {}`，根据名字返回一个`DataSource`，在我的测试代码里，这个类叫做`com.lifeStory.DataSourceStore`，这个类也要被传入`RepoStore`的构造函数，以获得这些DataSource。

5. RepoStore

   完成所有功能后，我们只需要从RepoStore中get我们需要的Repository出来，直接使用就可以了。