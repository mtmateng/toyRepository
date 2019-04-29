package com.lifeStory.baseRepository;

public interface Repository<T, ID> {

    T findById(ID id);

}
