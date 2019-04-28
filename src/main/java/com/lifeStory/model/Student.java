package com.lifeStory.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Data
@Entity
public class Student {

    @Id
    Integer id;
    String name;
    String gender;
    LocalDate birthday;
}
