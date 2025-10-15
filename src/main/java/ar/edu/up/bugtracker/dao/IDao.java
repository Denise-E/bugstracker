package ar.edu.up.bugtracker.dao;

import java.util.List;

public interface IDao<T, K> {
    K create(T entity);
    T findById(K id);
    List<T> findAll();
    void update(T entity);
    void deleteById(K id);
}
