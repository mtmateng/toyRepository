package com.lifeStory.repository;

import com.lifeStory.selfImplInterface.StudentInterface;
import com.lifeStory.test.returnVo.StudentVoClass;
import com.toySpring.repository.baseRepository.Repository;
import com.lifeStory.model.Student;
import com.lifeStory.test.returnVo.StudentVo;

import java.util.List;

public interface StudentRepository extends Repository<Student, Integer>, StudentInterface {

    StudentVo findByIdAndName(Integer id, String name);
//    StudentVo findByIdAndName(Integer id, String name);

    List<StudentVo> findByName(String name);

    <T> List<T> findByGender(String gender, Class<T> type);

    List<Student> findByNameAndGender(String name, String gender);

}