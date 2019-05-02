package com.toySpring.repository.baseRepository;

public interface Repository<T, ID> {

    T findById(ID id);

}
