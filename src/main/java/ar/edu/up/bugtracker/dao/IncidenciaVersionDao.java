package ar.edu.up.bugtracker.dao;

import ar.edu.up.bugtracker.exceptions.DaoException;
import ar.edu.up.bugtracker.models.IncidenciaVersion;
import jakarta.persistence.EntityManager;

import java.util.List;

public class IncidenciaVersionDao implements IDao<IncidenciaVersion, Long> {

    private final EntityManager em;

    public IncidenciaVersionDao(EntityManager em) {
        this.em = em;
    }

    @Override
    public Long create(IncidenciaVersion entity) {
        try {
            em.persist(entity);
            return entity.getId();
        } catch (Exception e) {
            throw new DaoException("Error creando versión de incidencia", e);
        }
    }

    @Override
    public IncidenciaVersion findById(Long id) {
        try {
            return em.find(IncidenciaVersion.class, id);
        } catch (Exception e) {
            throw new DaoException("Error buscando versión de incidencia por id", e);
        }
    }

    @Override
    public List<IncidenciaVersion> findAll() {
        try {
            return em.createQuery("SELECT iv FROM IncidenciaVersion iv ORDER BY iv.createdAt DESC", IncidenciaVersion.class)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error listando versiones de incidencia", e);
        }
    }

    public List<IncidenciaVersion> findByIncidencia(Long incidenciaId) {
        try {
            return em.createQuery(
                    "SELECT DISTINCT iv FROM IncidenciaVersion iv " +
                    "LEFT JOIN FETCH iv.estado " +
                    "LEFT JOIN FETCH iv.createdBy " +
                    "WHERE iv.incidencia.id = :incidenciaId ORDER BY iv.createdAt DESC",
                    IncidenciaVersion.class)
                    .setParameter("incidenciaId", incidenciaId)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error buscando versiones por incidencia", e);
        }
    }

    @Override
    public void update(IncidenciaVersion entity) {
        try {
            em.merge(entity);
        } catch (Exception e) {
            throw new DaoException("Error actualizando versión de incidencia", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            IncidenciaVersion managed = em.find(IncidenciaVersion.class, id);
            if (managed != null) {
                em.remove(managed);
            }
        } catch (Exception e) {
            throw new DaoException("Error eliminando versión de incidencia", e);
        }
    }

    public void deleteByIncidenciaId(Long incidenciaId) {
        try {
            em.createQuery("DELETE FROM IncidenciaVersion iv WHERE iv.incidencia.id = :incidenciaId")
                    .setParameter("incidenciaId", incidenciaId)
                    .executeUpdate();
        } catch (Exception e) {
            throw new DaoException("Error eliminando versiones por incidencia", e);
        }
    }
}

