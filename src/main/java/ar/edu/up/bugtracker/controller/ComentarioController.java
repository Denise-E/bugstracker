package ar.edu.up.bugtracker.controller;

import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.models.Comentario;
import ar.edu.up.bugtracker.service.ComentarioService;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import java.util.List;

public class ComentarioController {

    private final ComentarioService service;

    public ComentarioController(ComentarioService service) {
        this.service = service;
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
        return service.create(comentario, currentUser);
    }

    public List<Comentario> findByIncidencia(Long incidenciaId) {
        if (incidenciaId == null) {
            throw new ValidationException("ID de incidencia requerido");
        }
        return service.findByIncidencia(incidenciaId);
    }

    public Comentario getById(Long id) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        return service.getById(id);
    }

    public void update(Long id, String nuevoTexto, UserLoggedInDto currentUser) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        if (isBlank(nuevoTexto)) {
            throw new ValidationException("El texto del comentario no puede estar vacío");
        }
        service.update(id, nuevoTexto, currentUser);
    }

    public void delete(Long id, UserLoggedInDto currentUser) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        service.delete(id, currentUser);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

