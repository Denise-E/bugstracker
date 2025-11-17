package ar.edu.up.bugtracker.controller;

import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.IncidenciaEstado;
import ar.edu.up.bugtracker.models.IncidenciaVersion;
import ar.edu.up.bugtracker.service.IncidenciaService;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import java.util.List;

public class IncidenciaController {

    private final IncidenciaService service;

    public IncidenciaController(IncidenciaService service) {
        this.service = service;
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
        return service.create(incidencia, currentUser);
    }

    public List<Incidencia> getAll() {
        return service.getAll();
    }

    public List<Incidencia> findByProyecto(Long proyectoId) {
        if (proyectoId == null) {
            throw new ValidationException("ID de proyecto requerido");
        }
        return service.findByProyecto(proyectoId);
    }

    public Incidencia getById(Long id) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        return service.getById(id);
    }

    public void update(Long id, Incidencia incidencia) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        if (incidencia == null) {
            throw new ValidationException("La incidencia no puede ser nula");
        }
        service.update(id, incidencia);
    }

    public void cambiarEstado(Long id, Long nuevoEstadoId, UserLoggedInDto currentUser) {
        if (id == null) {
            throw new ValidationException("ID de incidencia requerido");
        }
        if (nuevoEstadoId == null) {
            throw new ValidationException("ID de estado requerido");
        }
        service.cambiarEstado(id, nuevoEstadoId, currentUser);
    }

    public List<IncidenciaVersion> getHistorialVersiones(Long incidenciaId) {
        if (incidenciaId == null) {
            throw new ValidationException("ID de incidencia requerido");
        }
        return service.getHistorialVersiones(incidenciaId);
    }

    public void delete(Long id) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        service.delete(id);
    }

    public List<IncidenciaEstado> getAllEstados() {
        return service.getAllEstados();
    }

    public IncidenciaEstado getEstadoById(Long id) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        return service.getEstadoById(id);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

