package com.lifeStory.baseRepository;

import lombok.Getter;

public class BaseRepository<T, ID> implements Repository<T, ID> {

    @Getter
    private final Class<T> domainClass;
    @Getter
    private final Class<ID> idClass;

    public BaseRepository(Class<T> domainClass, Class<ID> idClass) {

        this.domainClass = domainClass;
        this.idClass = idClass;

    }

}
