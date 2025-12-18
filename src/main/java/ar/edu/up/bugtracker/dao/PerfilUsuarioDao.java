package ar.edu.up.bugtracker.dao;

import ar.edu.up.bugtracker.exceptions.DaoException;
import ar.edu.up.bugtracker.models.PerfilUsuario;
import jakarta.persistence.EntityManager;

import java.util.List;

public class PerfilUsuarioDao implements IDao<PerfilUsuario, Long> {

    private final EntityManager em;

    public PerfilUsuarioDao(EntityManager em) {
        this.em = em;
    }

    @Override
    public Long create(PerfilUsuario entity) {
        return null;
    }

    @Override
    public PerfilUsuario findById(Long id) {
        try {
            if (id == null) return null;
            return em.find(PerfilUsuario.class, id);
        } catch (Exception e) {
            throw new DaoException("Error buscando perfil por id", e);
        }
    }

    @Override
    public List<PerfilUsuario> findAll() {
        try {
            return em.createQuery(
                    "SELECT p FROM PerfilUsuario p ORDER BY p.nombre",
                    PerfilUsuario.class
            ).getResultList();
        } catch (Exception e) {
            throw new DaoException("Erro aL buscar perfiles de usuarios "+e.getMessage());
        }
    }

    @Override
    public void update(PerfilUsuario entity) {
    }

    @Override
    public void deleteById(Long id) {
    }
}
