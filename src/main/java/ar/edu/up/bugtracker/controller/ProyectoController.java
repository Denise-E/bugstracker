package ar.edu.up.bugtracker.controller;

import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.service.ProyectoService;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import java.util.List;

public class ProyectoController {

    private final ProyectoService service;

    public ProyectoController(ProyectoService service) {
        this.service = service;
    }

    public Long create(Proyecto proyecto, UserLoggedInDto currentUser) {
        if (proyecto == null) {
            throw new ValidationException("El proyecto no puede ser nulo");
        }
        if (isBlank(proyecto.getNombre())) {
            throw new ValidationException("El nombre del proyecto es obligatorio");
        }
        return service.create(proyecto, currentUser);
    }

    public List<Proyecto> getAll() {
        return service.getAll();
    }

    public Proyecto getById(Long id) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        return service.getById(id);
    }

    public void update(Long id, Proyecto proyecto) {
        if (id == null) {
            throw new ValidationException("ID requerido");
        }
        if (proyecto == null) {
            throw new ValidationException("El proyecto no puede ser nulo");
        }
        service.update(id, proyecto);
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

