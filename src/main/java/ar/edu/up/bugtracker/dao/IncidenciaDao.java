package ar.edu.up.bugtracker.dao;

import ar.edu.up.bugtracker.exceptions.DaoException;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.IncidenciaEstado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.List;

public class IncidenciaDao implements IDao<Incidencia, Long> {

    private final EntityManager em;

    public IncidenciaDao(EntityManager em) {
        this.em = em;
    }

    @Override
    public Long create(Incidencia entity) {
        try {
            em.persist(entity);
            return entity.getId();
        } catch (Exception e) {
            throw new DaoException("Error creando incidencia", e);
        }
    }

    @Override
    public Incidencia findById(Long id) {
        try {
            return em.find(Incidencia.class, id);
        } catch (Exception e) {
            throw new DaoException("Error buscando incidencia por id", e);
        }
    }

    @Override
    public List<Incidencia> findAll() {
        try {
            return em.createQuery("SELECT i FROM Incidencia i ORDER BY i.id", Incidencia.class)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error listando incidencias", e);
        }
    }

    public List<Incidencia> findByProyecto(Long proyectoId) {
        try {
            return em.createQuery(
                    "SELECT i FROM Incidencia i WHERE i.proyecto.id = :proyectoId ORDER BY i.id",
                    Incidencia.class)
                    .setParameter("proyectoId", proyectoId)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error buscando incidencias por proyecto", e);
        }
    }

    @Override
    public void update(Incidencia entity) {
        try {
            em.merge(entity);
        } catch (Exception e) {
            throw new DaoException("Error actualizando incidencia", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            Incidencia managed = em.find(Incidencia.class, id);
            if (managed != null) {
                em.remove(managed);
            }
        } catch (Exception e) {
            throw new DaoException("Error eliminando incidencia", e);
        }
    }

    public IncidenciaEstado findEstadoById(Long id) {
        try {
            return em.find(IncidenciaEstado.class, id);
        } catch (Exception e) {
            throw new DaoException("Error buscando estado por id", e);
        }
    }

    public IncidenciaEstado findEstadoByNombre(String nombre) {
        try {
            return em.createQuery(
                    "SELECT e FROM IncidenciaEstado e WHERE e.nombre = :n", IncidenciaEstado.class)
                    .setParameter("n", nombre)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new DaoException("Error buscando estado por nombre", e);
        }
    }

    public List<IncidenciaEstado> findAllEstados() {
        try {
            return em.createQuery("SELECT e FROM IncidenciaEstado e ORDER BY e.nombre", IncidenciaEstado.class)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error listando estados", e);
        }
    }
}

