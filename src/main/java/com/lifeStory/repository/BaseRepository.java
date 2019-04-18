package com.lifeStory.repository;

import lombok.Getter;

public class BaseRepository<T, ID> implements Repository<T, ID> {

    @Getter
    private final Class<T> domainClass;
    @Getter
    private final Class<ID> idClass;

    private BaseRepository(Class<T> domainClass, Class<ID> idClass) {

        this.domainClass = domainClass;
        this.idClass = idClass;

    }

}
