package ar.edu.up.bugtracker.service;

import ar.edu.up.bugtracker.dao.ProyectoDao;
import ar.edu.up.bugtracker.exceptions.*;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ProyectoService {

    private final ProyectoDao proyectoDao;
    private final EntityManager em;

    public ProyectoService(ProyectoDao proyectoDao, EntityManager em) {
        this.proyectoDao = proyectoDao;
        this.em = em;
    }

    public Long create(Proyecto proyecto, UserLoggedInDto currentUser) {
        validateAdmin(currentUser, "Solo los administradores pueden crear proyectos");
        
        if (proyecto == null) {
            throw new ValidationException("El proyecto no puede ser nulo");
        }
        
        if (isBlank(proyecto.getNombre())) {
            throw new ValidationException("El nombre del proyecto es obligatorio");
        }

        try {
            begin();
            Long id = proyectoDao.create(proyecto);
            commit();
            return id;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error creando proyecto: ", ex);
        }
    }

    public List<Proyecto> getAll() {
        try {
            return proyectoDao.findAll();
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo lista de proyectos", ex);
        }
    }

    public Proyecto getById(Long id) {
        try {
            Proyecto proyecto = proyectoDao.findById(id);
            if (proyecto == null) {
                throw new NotFoundException("Proyecto no encontrado");
            }
            return proyecto;
        } catch (NotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo detalle del proyecto", ex);
        }
    }

    public void update(Long id, Proyecto proyecto) {
        if (proyecto == null) {
            throw new ValidationException("El proyecto no puede ser nulo");
        }

        Proyecto existente = proyectoDao.findById(id);
        if (existente == null) {
            throw new NotFoundException("Proyecto no encontrado");
        }

        if (!isBlank(proyecto.getNombre())) {
            existente.setNombre(proyecto.getNombre());
        }
        
        if (proyecto.getDescripcion() != null) {
            existente.setDescripcion(proyecto.getDescripcion());
        }

        try {
            begin();
            proyectoDao.update(existente);
            commit();
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error actualizando proyecto", ex);
        }
    }

    public void delete(Long id, UserLoggedInDto currentUser) {
        validateAdmin(currentUser, "Solo los administradores pueden eliminar proyectos");
        
        Proyecto proyecto = proyectoDao.findById(id);
        if (proyecto == null) {
            throw new NotFoundException("Proyecto no encontrado");
        }

        try {
            begin();
            proyectoDao.deleteById(id);
            commit();
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error eliminando proyecto", ex);
        }
    }

    // Helpers
    private void validateAdmin(UserLoggedInDto currentUser, String message) {
        if (currentUser == null) {
            throw new AuthException("Debes estar autenticado para realizar esta acción");
        }
        if (!"ADMIN".equalsIgnoreCase(currentUser.getPerfil())) {
            throw new ForbiddenException(message);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void begin() {
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
        }
    }

    private void commit() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().commit();
        }
    }

    private void rollbackSilently() {
        try {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } catch (Exception ignore) {
        }
    }
}

