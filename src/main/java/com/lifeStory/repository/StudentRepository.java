package com.lifeStory.repository;

import com.lifeStory.baseRepository.Repository;
import com.lifeStory.model.Student;

import java.util.List;

public interface StudentRepository extends Repository<Student, Integer> {

    //Student findById(Integer id);

    List<Student> findByName(String name);

    List<Student> findByGender(String gender);

    List<Student> findByNameAndGender(String name, String gender);

}