package ar.edu.up.bugtracker.service.dto;

/**
 * DTO para la respuesta de login exitoso.
 */
public class UserLoggedInDto {
    private Long id;
    private String nombre;
    private String email;
    private String perfil;

    public UserLoggedInDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
}
