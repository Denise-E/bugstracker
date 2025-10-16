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

    // POST /api/users/register
    public Long register(UserRegisterCmd cmd) {
        if (cmd == null) throw new ValidationException("Solicitud inválida");
        if (isBlank(cmd.getNombre())) throw new ValidationException("Nombre es requerido");
        if (isBlank(cmd.getEmail())) throw new ValidationException("Email es requerido");
        if (isBlank(cmd.getPassword())) throw new ValidationException("Password es requerido");
        if (isBlank(cmd.getPerfil())) throw new ValidationException("Perfil es requerido");
        validateEmailFormat(cmd.getEmail());
        return service.register(cmd);
    }

    // POST /api/users/login
    public UserLoggedInDto login(UserLoginCmd cmd) {
        if (cmd == null) throw new ValidationException("Solicitud inválida");
        if (isBlank(cmd.getEmail())) throw new ValidationException("Email es requerido");
        if (isBlank(cmd.getPassword())) throw new ValidationException("Password es requerido");
        validateEmailFormat(cmd.getEmail());
        return service.login(cmd);
    }

    // GET /api/users/detail/all
    public List<UserDetailDto> getAll() {
        return service.getAll();
    }

    // GET /api/users/detail/{user_id}
    public UserDetailDto getById(Long userId) {
        if (userId == null) throw new ValidationException("Id requerido");
        return service.getById(userId);
    }

    // PUT /api/users/update/{user_id}
    public void update(Long userId, UserUpdateCmd cmd) {
        if (userId == null) throw new ValidationException("Id requerido");
        if (cmd == null) throw new ValidationException("Solicitud inválida");

        // Validación superficial: al menos un campo para actualizar
        if (isBlank(cmd.getNombre()) && isBlank(cmd.getApellido())
                && isBlank(cmd.getPassword()) && isBlank(cmd.getPerfil())) {
            throw new ValidationException("No hay cambios para aplicar");
        }
        // No se permiten cambios en el email
        service.update(userId, cmd);
    }

    // DELETE /api/users/delete/{user_id}
    public void delete(Long userId) {
        if (userId == null) throw new ValidationException("Id requerido");
        service.delete(userId);
    }

    // Helpers
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private void validateEmailFormat(String email) {
        String e = email.trim().toLowerCase();
        String regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!e.matches(regex)) {
            throw new ValidationException("Email con formato inválido");
        }
    }
}
