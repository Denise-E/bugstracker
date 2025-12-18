package ar.edu.up.bugtracker.dao;

import ar.edu.up.bugtracker.exceptions.DaoException;
import ar.edu.up.bugtracker.models.Comentario;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ComentarioDao implements IDao<Comentario, Long> {

    private final EntityManager em;

    public ComentarioDao(EntityManager em) {
        this.em = em;
    }

    @Override
    public Long create(Comentario entity) {
        try {
            em.persist(entity);
            return entity.getId();
        } catch (Exception e) {
            throw new DaoException("Error creando comentario", e);
        }
    }

    @Override
    public Comentario findById(Long id) {
        try {
            return em.find(Comentario.class, id);
        } catch (Exception e) {
            throw new DaoException("Error buscando comentario por id", e);
        }
    }

    @Override
    public List<Comentario> findAll() {
        try {
            return em.createQuery("SELECT c FROM Comentario c ORDER BY c.createdAt DESC", Comentario.class)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error listando comentarios", e);
        }
    }

    public List<Comentario> findByIncidencia(Long incidenciaId) {
        try {
            return em.createQuery(
                    "SELECT c FROM Comentario c WHERE c.incidencia.id = :incidenciaId ORDER BY c.createdAt ASC",
                    Comentario.class)
                    .setParameter("incidenciaId", incidenciaId)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error buscando comentarios por incidencia", e);
        }
    }

    @Override
    public void update(Comentario entity) {
        try {
            em.merge(entity);
        } catch (Exception e) {
            throw new DaoException("Error actualizando comentario", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            Comentario managed = em.find(Comentario.class, id);
            if (managed != null) {
                em.remove(managed);
            }
        } catch (Exception e) {
            throw new DaoException("Error eliminando comentario", e);
        }
    }

    public void deleteByIncidenciaId(Long incidenciaId) {
        try {
            em.createQuery("DELETE FROM Comentario c WHERE c.incidencia.id = :incidenciaId")
                    .setParameter("incidenciaId", incidenciaId)
                    .executeUpdate();
        } catch (Exception e) {
            throw new DaoException("Error eliminando comentarios por incidencia", e);
        }
    }
}

