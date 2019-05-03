package com.lifeStory;

import com.lifeStory.baseRepo.MyBaseRepoImpl;
import com.lifeStory.repository.StudentRepository;
import com.lifeStory.repository2.CaseRepository;
import com.lifeStory.test.returnVo.StudentVoClass;
import com.toySpring.repository.baseRepository.BaseRepository;
import com.toySpring.repository.custom.CustomRepoSetting;
import com.toySpring.repository.utils.RepoStore;

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

        StudentRepository studentRepository = repoStore.getRepository(StudentRepository.class);
        System.out.println(studentRepository.findById(1));
        studentRepository.findByName("mt").forEach(o -> {
            System.out.println(o.getId());
            System.out.println(o.getName());
        });
        studentRepository.getMapById(1).forEach((k, v) -> System.out.println(String.format("key=%s,value=%s", k, v)));
        System.out.println(studentRepository.findByGender("male", StudentVoClass.class));
        System.out.println(studentRepository.findByNameAndGender("zmz", "male"));

        CaseRepository caseRepository = repoStore.getRepository(CaseRepository.class);
        caseRepository.findByCaseSubjectId("12345").ifPresent(System.out::println);
        System.out.println(caseRepository.idPlus1("123456"));

    }

    public static void main(String[] args) {

        CustomRepoSetting customRepoSetting = new CustomRepoSetting();
        customRepoSetting.setBaseRepositoryClass(BaseRepository.class);
        customRepoSetting.setDataSourceName("student");
        customRepoSetting.setEntityPackage("com.lifeStory.model");
        customRepoSetting.setRepositoryPackage("com.lifeStory.repository");

        CustomRepoSetting customRepoSetting2 = new CustomRepoSetting();
        customRepoSetting2.setBaseRepositoryClass(MyBaseRepoImpl.class);
        customRepoSetting2.setDataSourceName("case");
        customRepoSetting2.setEntityPackage("com.lifeStory.model2");
        customRepoSetting2.setRepositoryPackage("com.lifeStory.repository2");

        TestMain testMain = new TestMain(new RepoStore(TestMain.class, DataSourceStore.class, customRepoSetting, customRepoSetting2));

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

        insertSth(DataSourceStore.getDataSource("student"), sql);
        insertSth(DataSourceStore.getDataSource("case"), sql2);

        testMain.test();

    }

    private static void insertSth(DataSource dataSource, String sql) {


        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("执行%s失败", e);
        }

    }

    private static DataSource initDataSource() {

        return DataSourceStore.getDataSource("");

    }


}
