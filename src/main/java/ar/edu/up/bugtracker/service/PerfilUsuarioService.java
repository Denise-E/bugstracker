package ar.edu.up.bugtracker.service;

import ar.edu.up.bugtracker.dao.PerfilUsuarioDao;
import ar.edu.up.bugtracker.models.PerfilUsuario;

import java.util.List;

public class PerfilUsuarioService {

    private final PerfilUsuarioDao dao;

    public PerfilUsuarioService(PerfilUsuarioDao dao) {
        this.dao = dao;
    }

    public List<PerfilUsuario> getAll() {
        return dao.findAll();
    }
}
