package ar.edu.up.bugtracker.service;

import ar.edu.up.bugtracker.dao.ComentarioDao;
import ar.edu.up.bugtracker.exceptions.*;
import ar.edu.up.bugtracker.models.Comentario;
import ar.edu.up.bugtracker.models.Usuario;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ComentarioService {

    private final ComentarioDao comentarioDao;
    private final EntityManager em;

    public ComentarioService(ComentarioDao comentarioDao, EntityManager em) {
        this.comentarioDao = comentarioDao;
        this.em = em;
    }

    public Long create(Comentario comentario, UserLoggedInDto currentUser) {
        if (comentario == null) {
            throw new ValidationException("El comentario no puede ser nulo");
        }
        if (isBlank(comentario.getTexto())) {
            throw new ValidationException("El texto del comentario es obligatorio");
        }
        if (comentario.getIncidencia() == null || comentario.getIncidencia().getId() == null) {
            throw new ValidationException("La incidencia es obligatoria");
        }

        // Asignar el creador desde el usuario actual
        if (currentUser == null) {
            throw new AuthException("Debes estar autenticado para crear comentarios");
        }

        Usuario creador = em.find(Usuario.class, currentUser.getId());
        if (creador == null) {
            throw new NotFoundException("Usuario no encontrado");
        }
        comentario.setCreatedBy(creador);

        try {
            begin();
            Long incidenciaId = comentario.getIncidencia().getId();
            ar.edu.up.bugtracker.models.Incidencia incidenciaRef = em.getReference(
                ar.edu.up.bugtracker.models.Incidencia.class, incidenciaId);
            comentario.setIncidencia(incidenciaRef);
            
            Long id = comentarioDao.create(comentario);
            commit();
            return id;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error creando comentario", ex);
        }
    }

    public List<Comentario> findByIncidencia(Long incidenciaId) {
        synchronized (em) {
            try {
                em.clear(); // Limpiar el caché para asegurar datos actualizados
                
                boolean transactionStarted = !em.getTransaction().isActive();
                if (transactionStarted) {
                    em.getTransaction().begin();
                }
                
                List<Comentario> comentarios = comentarioDao.findByIncidencia(incidenciaId);
                
                // Traer las relaciones 
                if (comentarios != null) {
                    for (Comentario comentario : comentarios) {
                        comentario.getId();
                        comentario.getTexto();
                        comentario.getCreatedAt();
                        if (comentario.getIncidencia() != null) {
                            comentario.getIncidencia().getId();
                        }
                        if (comentario.getCreatedBy() != null) {
                            comentario.getCreatedBy().getId();
                            comentario.getCreatedBy().getNombre();
                            comentario.getCreatedBy().getApellido();
                            comentario.getCreatedBy().getEmail();
                        }
                    }
                }
                
                em.flush(); // Asegurar que las operaciones pendientes se completen
                
                return comentarios;
            } catch (RuntimeException ex) {
                rollbackSilently();
                throw new AppException("Error obteniendo comentarios de la incidencia", ex);
            }
        }
    }

    public Comentario getById(Long id) {
        try {
            Comentario comentario = comentarioDao.findById(id);
            if (comentario == null) {
                throw new NotFoundException("Comentario no encontrado");
            }
            return comentario;
        } catch (NotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo comentario", ex);
        }
    }

    public void update(Long id, String nuevoTexto, UserLoggedInDto currentUser) {
        if (isBlank(nuevoTexto)) {
            throw new ValidationException("El texto del comentario no puede estar vacío");
        }

        Comentario comentario = comentarioDao.findById(id);
        if (comentario == null) {
            throw new NotFoundException("Comentario no encontrado");
        }

        // Validar que solo el creador puede editar
        if (currentUser == null) {
            throw new AuthException("Debes estar autenticado para editar comentarios");
        }
        if (comentario.getCreatedBy() == null || comentario.getCreatedBy().getId() == null) {
            throw new ForbiddenException("No se puede determinar el autor del comentario");
        }
        if (!comentario.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Solo el autor del comentario puede editarlo");
        }

        comentario.setTexto(nuevoTexto.trim());

        try {
            begin();
            comentarioDao.update(comentario);
            commit();
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error actualizando comentario", ex);
        }
    }

    public void delete(Long id, UserLoggedInDto currentUser) {
        Comentario comentario = comentarioDao.findById(id);
        if (comentario == null) {
            throw new NotFoundException("Comentario no encontrado");
        }

        // Solo el creador puede eliminar
        if (currentUser == null) {
            throw new AuthException("Debes estar autenticado para eliminar comentarios");
        }
        if (comentario.getCreatedBy() == null || comentario.getCreatedBy().getId() == null) {
            throw new ForbiddenException("No se puede determinar el autor del comentario");
        }
        if (!comentario.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Solo el autor del comentario puede eliminarlo");
        }

        try {
            begin();
            comentarioDao.deleteById(id);
            commit();
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error eliminando comentario", ex);
        }
    }

    // Helpers
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

