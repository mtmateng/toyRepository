package com.lifeStory;

import com.lifeStory.baseRepository.BaseRepository;
import com.lifeStory.repository.StudentRepository;
import com.zaxxer.hikari.HikariDataSource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;

public class TestMain {

    private final StudentRepository studentRepository;

    public TestMain(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public void test() {
        System.out.println(studentRepository.findById(1));
        System.out.println(studentRepository.findByName("xiaozhang"));
        System.out.println(studentRepository.findByGender("male"));
        System.out.println(studentRepository.findByNameAndGender("xiaoma", "male"));
    }

    public static void main(String[] args) {

        StudentRepository studentRepository = initStudentRepository();
        TestMain testMain = new TestMain(studentRepository);
        testMain.test();
    }

    private static StudentRepository initStudentRepository() {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3308/publish-local?useUnicode=true&rewriteBatchedStatements=true&characterEncoding=utf8&useSSL=false");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUsername("root");
        dataSource.setPassword("dreamland");

        Class entityClass = (Class) ((ParameterizedType) StudentRepository.class.getGenericInterfaces()[0]).getActualTypeArguments()[0];
        Class idClass = (Class) ((ParameterizedType) StudentRepository.class.getGenericInterfaces()[0]).getActualTypeArguments()[1];
        BaseRepository baseRepository = new BaseRepository(entityClass, idClass);
        RepositoryHandler repositoryHandler = new RepositoryHandler(baseRepository, dataSource);
        StudentRepository studentRepository = (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                repositoryHandler
        );
        return studentRepository;
    }

}
