package com.lifeStory.repository;

import com.lifeStory.selfImplInterface.StudentInterface;
import com.toySpring.repository.baseRepository.Repository;
import com.lifeStory.model.Student;
import com.lifeStory.test.returnInterface.StudentVo;

import java.util.List;

public interface StudentRepository extends Repository<Student, Integer>, StudentInterface {

    StudentVo findByIdAndName(Integer id, String name);
//    StudentVo findByIdAndName(Integer id, String name);

    List<StudentVo> findByName(String name);

    List<Student> findByGender(String gender);

    List<Student> findByNameAndGender(String name, String gender);

}