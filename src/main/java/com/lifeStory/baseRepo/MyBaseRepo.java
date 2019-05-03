package com.lifeStory.baseRepo;

import com.toySpring.repository.baseRepository.Repository;

public interface MyBaseRepo<T, ID> extends Repository<T, ID> {

    T idPlus1(ID id);

}
