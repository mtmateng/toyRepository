package com.lifeStory.repository;

import com.lifeStory.model.Student;

import java.util.List;

public interface StudentRepository extends Repository<Student, Integer> {

    Student findById();

    List<Student> findByName();

    List<Student> findByNameAndAge();

}
