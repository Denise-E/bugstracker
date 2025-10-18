package ar.edu.up.bugtracker.controller;

import ar.edu.up.bugtracker.models.PerfilUsuario;
import ar.edu.up.bugtracker.service.PerfilUsuarioService;

import java.util.List;

public class UserRoleController {

    private final PerfilUsuarioService service;

    public UserRoleController(PerfilUsuarioService service) {
        this.service = service;
    }

    public List<PerfilUsuario> getAll() {
        return service.getAll();
    }
}
