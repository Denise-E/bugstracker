package ar.edu.up.bugtracker.service;

import ar.edu.up.bugtracker.dao.PerfilUsuarioDao;
import ar.edu.up.bugtracker.dao.UserDao;
import ar.edu.up.bugtracker.exceptions.*;
import ar.edu.up.bugtracker.models.PerfilUsuario;
import ar.edu.up.bugtracker.models.Usuario;
import ar.edu.up.bugtracker.service.cmd.UserLoginCmd;
import ar.edu.up.bugtracker.service.cmd.UserRegisterCmd;
import ar.edu.up.bugtracker.service.cmd.UserUpdateCmd;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import jakarta.persistence.EntityManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class UserService {

    private final UserDao usuarioDao;
    private final PerfilUsuarioDao perfilUsuarioDao;
    private final EntityManager em; 
    private static final int SALT_BYTES = 24;

    public UserService(UserDao usuarioDao, PerfilUsuarioDao perfilUsuarioDao, EntityManager em) {
        this.usuarioDao = usuarioDao;
        this.perfilUsuarioDao = perfilUsuarioDao;
        this.em = em;
    }

    public Long register(UserRegisterCmd cmd) {
        String email = normEmail(cmd.getEmail());
        if (usuarioDao.existsByEmail(email)) {
            throw new ValidationException("Email ya registrado");
        }

        PerfilUsuario perfil = resolvePerfil(cmd.getPerfilId());
        if (perfil == null) {
            throw new ValidationException("Perfil inválido: " + cmd.getPerfilId());
        }

        String salt = generateSalt();
        String hash = hashPassword(salt, cmd.getPassword());

        Usuario u = new Usuario();
        u.setNombre(cmd.getNombre());
        u.setApellido(cmd.getApellido());
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setPasswordSalt(salt);
        u.setPerfil(perfil);

        try {
            begin();
            Long id = usuarioDao.create(u);
            commit();
            return id;
        } catch (NotFoundException | ValidationException | AuthException | ForbiddenException ex) {
            rollbackSilently();
            throw ex;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error registrando al usuario: ", ex);
        }
    }

    public UserLoggedInDto login(UserLoginCmd cmd) {
        try {
            String email = normEmail(cmd.getEmail());
            Usuario u = usuarioDao.findByEmail(email);
            if (u == null) throw new AuthException("Credenciales inválidas");

            boolean ok = verifyPassword(
                    Objects.toString(u.getPasswordSalt(), ""),
                    cmd.getPassword(),
                    Objects.toString(u.getPasswordHash(), "")
            );
            if (!ok) throw new AuthException("Credenciales inválidas");

            return toLoggedInDto(u);
        } catch (NotFoundException | ValidationException | AuthException | ForbiddenException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AppException("Error logueando al usuario: ", ex);
        }
    }

    public List<UserDetailDto> getAll() {
        synchronized (em) {
            try {
                boolean transactionStarted = !em.getTransaction().isActive();
                if (transactionStarted) {
                    em.getTransaction().begin();
                }
                
                List<Usuario> usuarios = usuarioDao.findAll();
                
                if (usuarios != null) {
                    for (Usuario u : usuarios) {
                        if (u.getPerfil() != null) {
                            u.getPerfil().getId();
                            u.getPerfil().getNombre();
                        }
                    }
                }
                
                em.flush();
                
                List<UserDetailDto> result = usuarios.stream()
                        .map(this::toDetailDto)
                        .collect(Collectors.toList());
                
                return result;
            } catch (BusinessException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                rollbackSilently();
                throw new AppException("Error obteniendo lista de usuarios ", ex);
            }
        }
    }

    public UserDetailDto getById(Long id) {
        try {
            Usuario u = usuarioDao.findById(id);
            if (u == null) throw new NotFoundException("Usuario no encontrado");
            return toDetailDto(u);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AppException("Error obteniendo detalle del usuario: ", ex);
        }
    }

    public void update(Long id, UserUpdateCmd cmd) {
        Usuario u = usuarioDao.findById(id);
        if (u == null) throw new NotFoundException("Usuario no encontrado");

        if (!isBlank(cmd.getNombre())) u.setNombre(cmd.getNombre());
        if (!isBlank(cmd.getApellido())) u.setApellido(cmd.getApellido());

        if (!isBlank(cmd.getPassword())) {
            String salt = generateSalt();
            String hash = hashPassword(salt, cmd.getPassword());
            u.setPasswordSalt(salt);
            u.setPasswordHash(hash);
        }

        if (cmd.getPerfilId() != null) {
            PerfilUsuario perfil = resolvePerfil(cmd.getPerfilId());
            if (perfil == null) throw new ValidationException("Perfil inválido: " + cmd.getPerfilId());
            u.setPerfil(perfil);
        }

        try {
            begin();
            usuarioDao.update(u);
            commit();
        } catch (NotFoundException | ValidationException | AuthException | ForbiddenException ex) {
            rollbackSilently();
            throw ex;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error actualizando al usuario: ", ex);
        }
    }

    public void delete(Long id) {
        Usuario u = usuarioDao.findById(id);
        if (u == null) throw new NotFoundException("Usuario no encontrado");

        try {
            begin();
            usuarioDao.deleteById(id);
            commit();
        } catch (NotFoundException | ValidationException | AuthException | ForbiddenException ex) {
            rollbackSilently();
            throw ex;
        } catch (RuntimeException ex) {
            rollbackSilently();
            throw new AppException("Error eliminando al usuario: ", ex);
        }
    }

    // Helpers
    private PerfilUsuario resolvePerfil(Long perfilId) {
        if (perfilId == null) return null;
        return perfilUsuarioDao.findById(perfilId);
    }

    private String normEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String saltBase64, String rawPassword) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new AppException("Error generando hash de password", e);
        }
    }

    private boolean verifyPassword(String saltBase64, String rawPassword, String expectedHashBase64) {
        String actual = hashPassword(saltBase64, rawPassword);
        return actual.equals(expectedHashBase64);
    }

    private UserDetailDto toDetailDto(Usuario u) {
        UserDetailDto dto = new UserDetailDto();
        dto.setId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setEmail(u.getEmail());
        dto.setPerfil(u.getPerfil() != null ? u.getPerfil().getNombre() : null);
        dto.setCreadoEn(u.getCreadoEn());
        return dto;
    }

    private UserLoggedInDto toLoggedInDto(Usuario u) {
        UserLoggedInDto dto = new UserLoggedInDto();
        dto.setId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setEmail(u.getEmail());
        dto.setPerfil(u.getPerfil() != null ? u.getPerfil().getNombre() : null);
        return dto;
    }

    private void begin() {
        if (!em.getTransaction().isActive()) em.getTransaction().begin();
    }
    private void commit() {
        if (em.getTransaction().isActive()) em.getTransaction().commit();
    }
    private void rollbackSilently() {
        try {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } catch (Exception ignore) {}
    }
}
