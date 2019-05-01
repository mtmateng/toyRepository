package com.lifeStory.repository;

import com.lifeStory.baseRepository.Repository;
import com.lifeStory.model.Student;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends Repository<Student, Integer> {

    <T> T findSth(Class<T> type);

    List<Student> findByName(String name);

    List<Student> findByGender(String gender);

    List<Student> findByNameAndGender(String name, String gender);

}