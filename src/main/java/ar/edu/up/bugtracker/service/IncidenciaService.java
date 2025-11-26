package ar.edu.up.bugtracker.service;

import ar.edu.up.bugtracker.dao.IncidenciaDao;
import ar.edu.up.bugtracker.dao.IncidenciaVersionDao;
import ar.edu.up.bugtracker.exceptions.*;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.IncidenciaEstado;
import ar.edu.up.bugtracker.models.IncidenciaEstadoEnum;
import ar.edu.up.bugtracker.models.IncidenciaVersion;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.models.Usuario;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import jakarta.persistence.EntityManager;

import java.util.List;

public class IncidenciaService {

    private final IncidenciaDao incidenciaDao;
    private final IncidenciaVersionDao versionDao;
    private final EntityManager em;

    public IncidenciaService(IncidenciaDao incidenciaDao, IncidenciaVersionDao versionDao, EntityManager em) {
        this.incidenciaDao = incidenciaDao;
        this.versionDao = versionDao;
        this.em = em;
    }

    public Long create(Incidencia incidencia, UserLoggedInDto currentUser) {
        if (incidencia == null) {
            throw new ValidationException("La incidencia no puede ser nula");
        }
        if (isBlank(incidencia.getDescripcion())) {
            throw new ValidationException("La descripción de la incidencia es obligatoria");
        }
        if (incidencia.getProyecto() == null || incidencia.getProyecto().getId() == null) {
            throw new ValidationException("El proyecto es obligatorio");
        }
        if (currentUser == null) {
            throw new AuthException("Debes estar autenticado para crear incidencias");
        }

        IncidenciaEstado estadoNueva = incidenciaDao.findEstadoByNombre(IncidenciaEstadoEnum.NUEVA.getNombre());
        if (estadoNueva == null) {
            throw new BusinessException("No se encontró el estado inicial NUEVA");
        }

        Usuario creador = em.find(Usuario.class, currentUser.getId());
        if (creador == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        try {
            begin();

            if (incidencia.getProyecto() != null && incidencia.getProyecto().getId() != null) {
                Proyecto proyectoGestionado = em.getReference(Proyecto.class, incidencia.getProyecto().getId());
                incidencia.setProyecto(proyectoGestionado);
            }

            Long incidenciaId = incidenciaDao.create(incidencia);

            Incidencia incidenciaGestionada = incidenciaDao.findById(incidenciaId);

            IncidenciaVersion versionInicial = new IncidenciaVersion();
            versionInicial.setIncidencia(incidenciaGestionada);
            versionInicial.setEstado(estadoNueva);
            versionInicial.setCreatedBy(creador);
            versionInicial.setDetalles("{\"tipo\":\"creacion\",\"estado\":\"NUEVA\"}");

            Long versionId = versionDao.create(versionInicial);

            IncidenciaVersion versionGestionada = versionDao.findById(versionId);
            incidenciaGestionada.setCurrentVersion(versionGestionada);
            incidenciaDao.update(incidenciaGestionada);

            commit();
            return incidenciaId;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error creando incidencia", ex);
        }
    }

    public List<Incidencia> getAll() {
        try {
            return incidenciaDao.findAll();
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo lista de incidencias", ex);
        }
    }

    public List<Incidencia> findByProyecto(Long proyectoId) {
        try {
            return incidenciaDao.findByProyecto(proyectoId);
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo incidencias del proyecto", ex);
        }
    }

    public Incidencia getById(Long id) {
        try {
            Incidencia incidencia = incidenciaDao.findById(id);
            if (incidencia == null) {
                throw new NotFoundException("Incidencia no encontrada");
            }
            return incidencia;
        } catch (NotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo incidencia", ex);
        }
    }

    public void update(Long id, Incidencia incidencia) {
        if (incidencia == null) {
            throw new ValidationException("La incidencia no puede ser nula");
        }

        Incidencia existente = incidenciaDao.findById(id);
        if (existente == null) {
            throw new NotFoundException("Incidencia no encontrada");
        }

        if (!isBlank(incidencia.getDescripcion())) {
            existente.setDescripcion(incidencia.getDescripcion());
        }
        if (incidencia.getEstimacionHoras() != null) {
            existente.setEstimacionHoras(incidencia.getEstimacionHoras());
        }
        if (incidencia.getResponsable() != null && incidencia.getResponsable().getId() != null) {
            Usuario responsable = em.find(Usuario.class, incidencia.getResponsable().getId());
            existente.setResponsable(responsable);
        }

        try {
            begin();
            incidenciaDao.update(existente);
            commit();
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error actualizando incidencia", ex);
        }
    }

    public void cambiarEstado(Long id, Long nuevoEstadoId, UserLoggedInDto currentUser) {
        if (nuevoEstadoId == null) {
            throw new ValidationException("El estado es obligatorio");
        }
        if (currentUser == null) {
            throw new AuthException("Debes estar autenticado para cambiar el estado");
        }

        Incidencia incidencia = incidenciaDao.findById(id);
        if (incidencia == null) {
            throw new NotFoundException("Incidencia no encontrada");
        }

        IncidenciaEstado nuevoEstado = incidenciaDao.findEstadoById(nuevoEstadoId);
        if (nuevoEstado == null) {
            throw new NotFoundException("Estado no encontrado");
        }

        IncidenciaEstado estadoActual = incidencia.getCurrentVersion() != null 
                ? incidencia.getCurrentVersion().getEstado() 
                : null;
        if (estadoActual != null && estadoActual.getId().equals(nuevoEstadoId)) {
            throw new ValidationException("La incidencia ya está en ese estado");
        }

        Usuario usuario = em.find(Usuario.class, currentUser.getId());
        if (usuario == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        try {
            begin();

            IncidenciaVersion nuevaVersion = new IncidenciaVersion();
            nuevaVersion.setIncidencia(incidencia);
            nuevaVersion.setEstado(nuevoEstado);
            nuevaVersion.setCreatedBy(usuario);

            String estadoAnteriorNombre = estadoActual != null ? estadoActual.getNombre() : "NUEVA";
            String detallesJson = String.format(
                    "{\"tipo\":\"cambio_estado\",\"estado_anterior\":\"%s\",\"estado_nuevo\":\"%s\"}",
                    estadoAnteriorNombre, nuevoEstado.getNombre());
            nuevaVersion.setDetalles(detallesJson);

            Long versionId = versionDao.create(nuevaVersion);

            IncidenciaVersion versionGestionada = versionDao.findById(versionId);
            incidencia.setCurrentVersion(versionGestionada);
            incidenciaDao.update(incidencia);

            commit();
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error cambiando estado de la incidencia", ex);
        }
    }

    public List<IncidenciaVersion> getHistorialVersiones(Long incidenciaId) {
        try {
            return versionDao.findByIncidencia(incidenciaId);
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo historial de versiones", ex);
        }
    }

    public List<IncidenciaEstado> getAllEstados() {
        try {
            return incidenciaDao.findAllEstados();
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo lista de estados", ex);
        }
    }

    public IncidenciaEstado getEstadoById(Long id) {
        try {
            IncidenciaEstado estado = incidenciaDao.findEstadoById(id);
            if (estado == null) {
                throw new NotFoundException("Estado no encontrado");
            }
            return estado;
        } catch (NotFoundException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo estado", ex);
        }
    }

    public void delete(Long id) {
        Incidencia incidencia = incidenciaDao.findById(id);
        if (incidencia == null) {
            throw new NotFoundException("Incidencia no encontrada");
        }

        try {
            begin();
            
            // Eliminar todos los comentarios
            em.createQuery("DELETE FROM Comentario c WHERE c.incidencia.id = :incidenciaId")
                    .setParameter("incidenciaId", id)
                    .executeUpdate();
            em.flush();
            em.clear();
            
            // Eliminar todas las versiones
            em.createQuery("DELETE FROM IncidenciaVersion iv WHERE iv.incidencia.id = :incidenciaId")
                    .setParameter("incidenciaId", id)
                    .executeUpdate();
            em.flush();
            em.clear();
            
            // Eliminar la incidencia
            incidenciaDao.deleteById(id);
            commit();
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error eliminando incidencia", ex);
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

