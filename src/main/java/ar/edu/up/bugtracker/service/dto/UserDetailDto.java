package ar.edu.up.bugtracker.service.dto;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta del detalle de un usuario.
 */
public class UserDetailDto {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String perfil;
    private LocalDateTime creadoEn;

    public UserDetailDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
