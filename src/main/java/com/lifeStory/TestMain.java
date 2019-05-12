package com.lifeStory;

import com.lifeStory.baseRepo.MyBaseRepoImpl;
import com.lifeStory.model.Student;
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
import java.time.LocalDate;

public class TestMain {

    private final RepoStore repoStore;

    private TestMain(RepoStore repoStore) {
        this.repoStore = repoStore;
    }

    private void test() {

        StudentRepository studentRepository = repoStore.getRepository(StudentRepository.class);
        System.out.println("++++++++以下是studentRepository的测试++++++++");
        System.out.println("\n--------测试Insert");

        Student student = new Student();
        student.setId(6);
        student.setName("ma");
        student.setBirthday(LocalDate.now());
        student.setGender("Male");
        studentRepository.save(student);
        System.out.println(studentRepository.findById(6));
        System.out.println("\n--------测试update");
        student.setName("zh");
        studentRepository.save(student);
        System.out.println(studentRepository.findById(6));

        System.out.println("\n--------测试BaseRepository工作情况，findById");
        System.out.println(studentRepository.findById(1));
        System.out.println("\n--------测试查询方法返回接口时，只查询部分字段，findByname");
        studentRepository.findByName("mt").forEach(o -> {
            System.out.println("id=" + o.getId());
            System.out.println("name=" + o.getName());
        });
        System.out.println("\n--------测试业务Repository扩展多接口，另一个接口提供实现的情况，getMapById");
        studentRepository.getMapById(1).forEach((k, v) -> System.out.println(String.format("key=%s,value=%s", k, v)));
        System.out.println("\n--------测试传入class作为参数，只返回部分字段功能；测试返回List功能，findByGender");
        System.out.println(studentRepository.findByGender("male", StudentVoClass.class));
        System.out.println("\n--------测试动态解析方法名，生成SQL语句检索，findByNameAndGender");
        System.out.println(studentRepository.findByNameAndGender("zmz", "male"));

        CaseRepository caseRepository = repoStore.getRepository(CaseRepository.class);
        System.out.println("\n\n++++++++以下是caseRepository的测试++++++++");
        System.out.println("\n--------测试返回Optional容器类,findByCaseSubjectId");
        caseRepository.findByCaseSubjectId("12345").ifPresent(System.out::println);
        System.out.println("\n--------测试替换BaseRepository的效果,idPlus1");
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

}
