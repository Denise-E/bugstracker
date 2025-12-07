package ar.edu.up.bugtracker.dao;

import ar.edu.up.bugtracker.exceptions.DaoException;
import ar.edu.up.bugtracker.models.Proyecto;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ProyectoDao implements IDao<Proyecto, Long> {

    private final EntityManager em;

    public ProyectoDao(EntityManager em) {
        this.em = em;
    }

    @Override
    public Long create(Proyecto entity) {
        try {
            em.persist(entity);
            return entity.getId();
        } catch (Exception e) {
            throw new DaoException("Error creando proyecto", e);
        }
    }

    @Override
    public Proyecto findById(Long id) {
        try {
            return em.find(Proyecto.class, id);
        } catch (Exception e) {
            throw new DaoException("Error buscando proyecto por id", e);
        }
    }

    @Override
    public List<Proyecto> findAll() {
        try {
            return em.createQuery("SELECT p FROM Proyecto p ORDER BY p.creadoEn DESC", Proyecto.class)
                    .getResultList();
        } catch (Exception e) {
            throw new DaoException("Error listando proyectos", e);
        }
    }

    @Override
    public void update(Proyecto entity) {
        try {
            em.merge(entity);
        } catch (Exception e) {
            throw new DaoException("Error actualizando proyecto", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            Proyecto managed = em.find(Proyecto.class, id);
            if (managed != null) {
                em.remove(managed);
            }
        } catch (Exception e) {
            throw new DaoException("Error eliminando proyecto", e);
        }
    }
}

