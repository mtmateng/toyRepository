package com.lifeStory;

import com.lifeStory.repository.CaseRepository;
import com.lifeStory.utils.RepoStore;

public class TestMain {

    private final RepoStore repoStore;

    public TestMain(RepoStore repoStore) {
        this.repoStore = repoStore;
    }

    public void test() {

//        StudentRepository studentRepository = repoStore.getRepository(StudentRepository.class);
//        System.out.println(studentRepository.findById(1));
//        System.out.println(studentRepository.findByName("xiaozhang"));
//        System.out.println(studentRepository.findByGender("male"));
//        System.out.println(studentRepository.findByNameAndGender("xiaoma", "male"));

        CaseRepository caseRepository = repoStore.getRepository(CaseRepository.class);
        caseRepository.findByCaseSubjectId("12345").ifPresent(System.out::println);

    }

    public static void main(String[] args) {

        TestMain testMain = new TestMain(new RepoStore());
        testMain.test();

    }

}
