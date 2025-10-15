package ar.edu.up.bugtracker.dao;

import ar.edu.up.bugtracker.models.Usuario;

public interface UserDao extends IDao<Usuario, Long> {
    Usuario findByEmail(String email);
    boolean existsByEmail(String email);
}
