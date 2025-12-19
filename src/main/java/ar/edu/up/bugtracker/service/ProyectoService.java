package ar.edu.up.bugtracker.service;

import ar.edu.up.bugtracker.dao.ComentarioDao;
import ar.edu.up.bugtracker.dao.IncidenciaDao;
import ar.edu.up.bugtracker.dao.IncidenciaVersionDao;
import ar.edu.up.bugtracker.dao.ProyectoDao;
import ar.edu.up.bugtracker.exceptions.*;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ProyectoService {

    private final ProyectoDao proyectoDao;
    private final IncidenciaDao incidenciaDao;
    private final ComentarioDao comentarioDao;
    private final IncidenciaVersionDao incidenciaVersionDao;
    private final EntityManager em;

    public ProyectoService(ProyectoDao proyectoDao, IncidenciaDao incidenciaDao, 
                          ComentarioDao comentarioDao, IncidenciaVersionDao incidenciaVersionDao, 
                          EntityManager em) {
        this.proyectoDao = proyectoDao;
        this.incidenciaDao = incidenciaDao;
        this.comentarioDao = comentarioDao;
        this.incidenciaVersionDao = incidenciaVersionDao;
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
        } catch (BusinessException ex) {
            rollbackSilently();
            throw ex;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error creando proyecto: ", ex);
        }
    }

    public List<Proyecto> getAll() {
        try {
            return proyectoDao.findAll();
        } catch (BusinessException ex) {
            throw ex;
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
        } catch (BusinessException ex) {
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
        } catch (BusinessException ex) {
            rollbackSilently();
            throw ex;
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
            
            // Buscar todas las incidencias por ID del proyecto
            List<Incidencia> incidencias = incidenciaDao.findByProyecto(id);
            
            // Para todas las incidencias borrar todos los registros de las tablas "comentario" e "incidencia_version" por incidencia_id
            for (Incidencia incidencia : incidencias) {
                Long incidenciaId = incidencia.getId();
                
                // Eliminar comentarios por incidencia_id
                comentarioDao.deleteByIncidenciaId(incidenciaId);
                
                // Eliminar versiones por incidencia_id
                incidenciaVersionDao.deleteByIncidenciaId(incidenciaId);
            }
            em.flush();
            em.clear();
            
            // Borrar las incidencias por proyecto
            incidenciaDao.deleteByProyectoId(id);
            em.flush();
            em.clear();
            
            // Borrar el proyecto
            proyectoDao.deleteById(id);
            commit();
        } catch (BusinessException ex) {
            rollbackSilently();
            throw ex;
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

