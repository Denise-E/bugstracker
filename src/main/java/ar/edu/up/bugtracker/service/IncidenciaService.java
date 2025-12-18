package ar.edu.up.bugtracker.service;

import ar.edu.up.bugtracker.dao.ComentarioDao;
import ar.edu.up.bugtracker.dao.IncidenciaDao;
import ar.edu.up.bugtracker.dao.IncidenciaVersionDao;
import ar.edu.up.bugtracker.dao.ProyectoDao;
import ar.edu.up.bugtracker.dao.UserDao;
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
    private final UserDao userDao;
    private final ProyectoDao proyectoDao;
    private final ComentarioDao comentarioDao;
    private final EntityManager em;

    public IncidenciaService(IncidenciaDao incidenciaDao, IncidenciaVersionDao versionDao, 
                             UserDao userDao, ProyectoDao proyectoDao, ComentarioDao comentarioDao,
                             EntityManager em) {
        this.incidenciaDao = incidenciaDao;
        this.versionDao = versionDao;
        this.userDao = userDao;
        this.proyectoDao = proyectoDao;
        this.comentarioDao = comentarioDao;
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

        IncidenciaEstado estadoInicial = incidenciaDao.findEstadoById(1L);
        if (estadoInicial == null) {
            throw new BusinessException("No se encontró el estado inicial con ID 1");
        }

        Usuario creador = userDao.findById(currentUser.getId());
        if (creador == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        try {
            begin();

            if (incidencia.getProyecto() != null && incidencia.getProyecto().getId() != null) {
                Proyecto proyectoGestionado = proyectoDao.getReference(incidencia.getProyecto().getId());
                incidencia.setProyecto(proyectoGestionado);
            }

            if (incidencia.getResponsable() != null && incidencia.getResponsable().getId() != null) {
                Usuario responsableGestionado = userDao.getReference(incidencia.getResponsable().getId());
                incidencia.setResponsable(responsableGestionado);
            }

            Long incidenciaId = incidenciaDao.create(incidencia);

            Incidencia incidenciaGestionada = incidenciaDao.findById(incidenciaId);

            IncidenciaVersion versionInicial = new IncidenciaVersion();
            versionInicial.setIncidencia(incidenciaGestionada);
            versionInicial.setEstado(estadoInicial);
            versionInicial.setCreatedBy(creador);
            String estadoNombre = estadoInicial.getNombre() != null ? estadoInicial.getNombre() : "TODO";
            versionInicial.setDetalles("{\"tipo\":\"creacion\",\"estado\":\"" + estadoNombre + "\"}");

            Long versionId = versionDao.create(versionInicial);

            IncidenciaVersion versionGestionada = versionDao.findById(versionId);
            incidenciaGestionada.setCurrentVersion(versionGestionada);
            incidenciaDao.update(incidenciaGestionada);

            commit();
            return incidenciaId;
        } catch (BusinessException ex) {
            rollbackSilently();
            throw ex;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error creando incidencia", ex);
        }
    }

    public List<Incidencia> getAll() {
        try {
            return incidenciaDao.findAll();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo lista de incidencias", ex);
        }
    }

    public List<Incidencia> findByProyecto(Long proyectoId) {
        try {
            return incidenciaDao.findByProyecto(proyectoId);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo incidencias del proyecto", ex);
        }
    }

    public Incidencia getById(Long id) {
        // Sincronizar el acceso al EntityManager para evitar problemas con múltiples threads
        synchronized (em) {
            try {
                em.clear();
                
                boolean transactionStarted = !em.getTransaction().isActive();
                if (transactionStarted) {
                    em.getTransaction().begin();
                }
                
                Incidencia incidencia = incidenciaDao.findById(id);
                if (incidencia == null) {
                    throw new NotFoundException("Incidencia no encontrada");
                }
                
                if (incidencia.getProyecto() != null) {
                    incidencia.getProyecto().getId();
                    incidencia.getProyecto().getNombre();
                }
                if (incidencia.getResponsable() != null) {
                    incidencia.getResponsable().getId();
                }
                if (incidencia.getCurrentVersion() != null) {
                    incidencia.getCurrentVersion().getId();
                    if (incidencia.getCurrentVersion().getEstado() != null) {
                        incidencia.getCurrentVersion().getEstado().getId();
                        incidencia.getCurrentVersion().getEstado().getNombre();
                    }
                    if (incidencia.getCurrentVersion().getCreatedBy() != null) {
                        incidencia.getCurrentVersion().getCreatedBy().getId();
                    }
                }
                
                em.flush(); 
                
                return incidencia;
            } catch (BusinessException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                rollbackSilently();
                throw new AppException("Error obteniendo incidencia", ex);
            }
        }
    }

    public void update(Long id, Incidencia incidencia) {
        if (incidencia == null) {
            throw new ValidationException("La incidencia no puede ser nula");
        }

        try {
            begin();
            
            // Obtener la incidencia para asegurar que todas las relaciones estén cargadas
            Incidencia existente = incidenciaDao.findById(id);
            if (existente == null) {
                throw new NotFoundException("Incidencia no encontrada");
            }

            // Guardar el currentVersion antes de hacer cambios
            IncidenciaVersion currentVersionPreservado = existente.getCurrentVersion();
            
            // Materializar el estado del currentVersion
            if (currentVersionPreservado != null && currentVersionPreservado.getEstado() != null) {
                currentVersionPreservado.getEstado().getId();
                currentVersionPreservado.getEstado().getNombre();
            }

            // Actualizar solo los campos que se proporcionan
            boolean actualizarDescripcion = !isBlank(incidencia.getDescripcion());
            boolean actualizarEstimacion = incidencia.getEstimacionHoras() != null;
            boolean actualizarResponsable = incidencia.getResponsable() != null || 
                    (incidencia.getResponsable() == null && !actualizarDescripcion && !actualizarEstimacion);
            
            if (actualizarDescripcion) {
                existente.setDescripcion(incidencia.getDescripcion());
            }
            if (actualizarEstimacion) {
                existente.setEstimacionHoras(incidencia.getEstimacionHoras());
            }
            
            if (actualizarResponsable) {
                if (incidencia.getResponsable() != null && incidencia.getResponsable().getId() != null) {
                    Usuario responsable = userDao.findById(incidencia.getResponsable().getId());
                    existente.setResponsable(responsable);
                } else {
                    existente.setResponsable(null);
                }
            }
            
            // Asegurar que el currentVersion se mantenga siempre
            if (currentVersionPreservado != null) {
                existente.setCurrentVersion(currentVersionPreservado);
            }
            
            incidenciaDao.update(existente);
            commit();
        } catch (BusinessException ex) {
            rollbackSilently();
            throw ex;
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

        Usuario usuario = userDao.findById(currentUser.getId());
        if (usuario == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        try {
            begin();

            Incidencia incidenciaGestionada = incidenciaDao.findById(id);
            if (incidenciaGestionada == null) {
                throw new NotFoundException("Incidencia no encontrada");
            }

            IncidenciaVersion nuevaVersion = new IncidenciaVersion();
            nuevaVersion.setIncidencia(incidenciaGestionada);
            nuevaVersion.setEstado(nuevoEstado);
            nuevaVersion.setCreatedBy(usuario);

            String estadoAnteriorNombre = estadoActual != null ? estadoActual.getNombre() : "NUEVA";
            String detallesJson = String.format(
                    "{\"tipo\":\"cambio_estado\",\"estado_anterior\":\"%s\",\"estado_nuevo\":\"%s\"}",
                    estadoAnteriorNombre, nuevoEstado.getNombre());
            nuevaVersion.setDetalles(detallesJson);

            Long versionId = versionDao.create(nuevaVersion);

            // Asegurar que la nueva versión esté persistida antes de usarla
            em.flush();
            
            IncidenciaVersion versionGestionada = versionDao.findById(versionId);
            incidenciaGestionada.setCurrentVersion(versionGestionada);
            
            incidenciaDao.update(incidenciaGestionada);
            
            em.flush();
            commit();
        } catch (NotFoundException | ValidationException | AuthException | ForbiddenException ex) {
            rollbackSilently();
            throw ex;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error cambiando estado de la incidencia", ex);
        }
    }

    public List<IncidenciaVersion> getHistorialVersiones(Long incidenciaId) {
        // Sincronizar el acceso al EntityManager para evitar problemas con múltiples threads
        synchronized (em) {
            try {
                em.clear();
                
                boolean transactionStarted = !em.getTransaction().isActive();
                if (transactionStarted) {
                    em.getTransaction().begin();
                }
                
                List<IncidenciaVersion> versiones = versionDao.findByIncidencia(incidenciaId);
                
                if (versiones != null) {
                    versiones.size(); 
                    for (IncidenciaVersion version : versiones) {
                        version.getId();
                        version.getCreatedAt();
                        if (version.getEstado() != null) {
                            version.getEstado().getId();
                            version.getEstado().getNombre();
                        }
                        if (version.getCreatedBy() != null) {
                            version.getCreatedBy().getId();
                        }
                    }
                }
                
                em.flush(); 
                
                return versiones;
            } catch (BusinessException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                rollbackSilently();
                throw new AppException("Error obteniendo historial de versiones", ex);
            }
        }
    }

    public List<IncidenciaEstado> getAllEstados() {
        // Sincronizar el acceso al EntityManager para evitar problemas con múltiples threads
        // Esto es necesario porque EntityManager no es thread-safe y múltiples SwingWorker
        // pueden ejecutarse simultáneamente
        synchronized (em) {
            try {
                boolean transactionStarted = !em.getTransaction().isActive();
                if (transactionStarted) {
                    em.getTransaction().begin();
                }
                
                List<IncidenciaEstado> estados = incidenciaDao.findAllEstados();
                
                if (estados != null) {
                    estados.size(); 
                    for (IncidenciaEstado estado : estados) {
                        estado.getId(); 
                        estado.getNombre();
                    }
                }
                
                em.flush(); 
                
                return estados;
            } catch (BusinessException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                rollbackSilently();
                throw new AppException("Error obteniendo lista de estados", ex);
            }
        }
    }

    public IncidenciaEstado getEstadoById(Long id) {
        try {
            IncidenciaEstado estado = incidenciaDao.findEstadoById(id);
            if (estado == null) {
                throw new NotFoundException("Estado no encontrado");
            }
            return estado;
        } catch (BusinessException ex) {
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
            comentarioDao.deleteByIncidenciaId(id);
            em.flush();
            em.clear();
            
            // Eliminar todas las versiones
            versionDao.deleteByIncidenciaId(id);
            em.flush();
            em.clear();
            
            // Eliminar la incidencia
            incidenciaDao.deleteById(id);
            commit();
        } catch (BusinessException ex) {
            rollbackSilently();
            throw ex;
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

