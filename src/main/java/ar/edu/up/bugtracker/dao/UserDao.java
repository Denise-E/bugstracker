package ar.edu.up.bugtracker.dao;

import ar.edu.up.bugtracker.exceptions.DaoException;
import ar.edu.up.bugtracker.models.PerfilUsuario;
import ar.edu.up.bugtracker.models.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.List;


public class UserDao implements IDao<Usuario, Long> {

    private final EntityManager em;

    public UserDao(EntityManager em) {
        this.em = em;
    }

    @Override
    public Long create(Usuario entity) {
        try {
            em.persist(entity);
            return entity.getId();
        } catch (Exception e) {
            throw new DaoException("Error creando usuario", e);
        }
    }

    @Override
    public Usuario findById(Long id) {
        try {
            return em.find(Usuario.class, id);
        } catch (Exception e) {
            throw new DaoException("Error buscando usuario por id", e);
        }
    }

    @Override
    public List<Usuario> findAll() {
        try {
            return em.createQuery(
                    "SELECT DISTINCT u FROM Usuario u " +
                    "LEFT JOIN FETCH u.perfil " +
                    "ORDER BY u.id", Usuario.class)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error listando usuarios", e);
        }
    }

    @Override
    public void update(Usuario entity) {
        try {
            em.merge(entity);
        } catch (Exception e) {
            throw new DaoException("Error actualizando usuario", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            Usuario managed = em.find(Usuario.class, id);
            if (managed != null) {
                em.remove(managed);
            }
        } catch (Exception e) {
            throw new DaoException("Error eliminando usuario", e);
        }
    }

    public Usuario findByEmail(String email) {
        try {
            return em.createQuery(
                    "SELECT u FROM Usuario u WHERE LOWER(u.email) = :e", Usuario.class)
                    .setParameter("e", email == null ? null : email.trim().toLowerCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new DaoException("Error buscando usuario por email", e);
        }
    }

    public boolean existsByEmail(String email) {
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(u) FROM Usuario u WHERE LOWER(u.email) = :e", Long.class)
                    .setParameter("e", email == null ? null : email.trim().toLowerCase())
                    .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            throw new DaoException("Error verificando existencia de email", e);
        }
    }

    /** Helper de lectura para resolver el perfil por nombre (ADMIN/USUARIO). */
    public PerfilUsuario findPerfilByNombre(String nombre) {
        try {
            return em.createQuery(
                    "SELECT p FROM PerfilUsuario p WHERE p.nombre = :n", PerfilUsuario.class)
                    .setParameter("n", nombre)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new DaoException("Error buscando perfil por nombre", e);
        }
    }
}
