package com.lifeStory;

import com.lifeStory.utils.RepoStore;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TestMain {

    private final RepoStore repoStore;

    private TestMain(RepoStore repoStore) {
        this.repoStore = repoStore;
    }

    private void test() {

//        StudentRepository studentRepository = repoStore.getRepository(StudentRepository.class);
//        System.out.println(studentRepository.findById(1));
////        studentRepository.findSth(Student.class);
//        System.out.println(studentRepository.findByName("mt"));
//        System.out.println(studentRepository.findByName("mt"));
//        System.out.println(studentRepository.findByGender("male"));
//        System.out.println(studentRepository.findByNameAndGender("zmz", "male"));
//
//        CaseRepository caseRepository = repoStore.getRepository(CaseRepository.class);
//        caseRepository.findByCaseSubjectId("12345").ifPresent(System.out::println);

    }

    public static void main(String[] args) {

        DataSource dataSource = initDataSource();
        TestMain testMain = new TestMain(new RepoStore(dataSource,
                "com.lifeStory.model",
                "com.lifeStory.repository"));

        insertSth(dataSource);

        testMain.test();

    }

    private static void insertSth(DataSource dataSource) {

        String sql = "INSERT INTO `student` (id, name, gender, birthday) VALUES \n" +
                "(1,'mt','male','20180102'),\n" +
                "(2,'syx','male','20180302'),\n" +
                "(3,'zmz','female','20180902'),\n" +
                "(4,'ly','female','20160102'),\n" +
                "(5,'lry','male','20180402');";

        String sql2 = "INSERT INTO `case_entity` (case_subject_id, case_id, case_guid, case_name, case_type, summary, update_time, category) VALUES \n" +
                "('12345','A918271927','asdihalkjdbnakdbklac','王大全','刑事','聚众吸毒','20180202','容留他人吸毒'),\n" +
                "('123456','A918271927','asdihalkjdbnakdbklac','王大全','刑事','聚众吸毒','20180202','容留他人吸毒'),\n" +
                "('1234567','A918271927','asdihalkjdbnakdbklac','王大全','刑事','聚众吸毒','20180202','容留他人吸毒'),\n" +
                "('12345678','A918271927','asdihalkjdbnakdbklac','王大全','刑事','聚众吸毒','20180202','容留他人吸毒');";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            statement.execute(sql2);
        } catch (SQLException e) {
            throw new RuntimeException("执行%s失败", e);
        }

    }

    private static DataSource initDataSource() {

        HikariDataSource innerDataSource = new HikariDataSource();
        innerDataSource.setJdbcUrl("jdbc:h2:mem:test");
        innerDataSource.setDriverClassName("org.h2.Driver");
        innerDataSource.setUsername("testdb");
        innerDataSource.setPassword("testdb");
        return innerDataSource;

    }


}
