package com.toySpring.repository.baseRepository;

import java.util.Collection;

public interface Repository<T, ID> {

    T findById(ID id);

    T save(T entity);

    Collection<T> saveAll(Iterable<T> entities);

}
