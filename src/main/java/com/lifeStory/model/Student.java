package com.lifeStory.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Student {

    Integer id;
    String name;
    String gender;
    LocalDate birthday;
}
