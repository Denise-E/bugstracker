package ar.edu.up.bugtracker.controller;

import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.service.UserService;
import ar.edu.up.bugtracker.service.cmd.UserLoginCmd;
import ar.edu.up.bugtracker.service.cmd.UserRegisterCmd;
import ar.edu.up.bugtracker.service.cmd.UserUpdateCmd;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import java.util.List;

public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    public Long register(UserRegisterCmd cmd) {
        // Validación mínima a nivel controller (sin lógica de negocio)
        if (cmd == null) throw new ValidationException("Body requerido.");
        if (isBlank(cmd.getNombre()) || isBlank(cmd.getApellido())
                || isBlank(cmd.getEmail()) || isBlank(cmd.getPassword())) {
            throw new ValidationException("Completá nombre, apellido, email y password.");
        }
        if (cmd.getPerfilId() == null) {                     // <<< usar ID, no String
            throw new ValidationException("Debés seleccionar un rol válido.");
        }
        return service.register(cmd);
    }

    public UserLoggedInDto login(UserLoginCmd cmd) {
        if (cmd == null) throw new ValidationException("Body requerido.");
        if (isBlank(cmd.getEmail()) || isBlank(cmd.getPassword())) {
            throw new ValidationException("Email y password son obligatorios.");
        }
        return service.login(cmd);
    }

    public List<UserDetailDto> getAll() {
        return service.getAll();
    }

    public UserDetailDto getById(Long id) {
        if (id == null) throw new ValidationException("ID requerido.");
        return service.getById(id);
    }

    public void update(Long id, UserUpdateCmd cmd) {
        if (id == null) throw new ValidationException("ID requerido.");
        if (cmd == null) throw new ValidationException("Body requerido.");

        // al menos un campo informado
        if (isBlank(cmd.getNombre())
                && isBlank(cmd.getApellido())
                && isBlank(cmd.getPassword())
                && cmd.getPerfilId() == null) {              // <<< usar ID, no String
            throw new ValidationException("No hay cambios para aplicar.");
        }
        service.update(id, cmd);
    }

    public void delete(Long id) {
        if (id == null) throw new ValidationException("ID requerido.");
        service.delete(id);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
