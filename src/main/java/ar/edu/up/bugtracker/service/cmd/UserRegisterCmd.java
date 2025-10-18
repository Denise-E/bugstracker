package ar.edu.up.bugtracker.service.cmd;

/**
 * Body esperado para el registro de un usuario.
 * Campos obligatorios: nombre, apellido, email, password, perfilId.
 */
public class UserRegisterCmd {
    private String nombre;
    private String apellido;
    private String email;
    private String password;
    private Long perfilId;

    public UserRegisterCmd() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getPerfilId() { return perfilId; }
    public void setPerfilId(Long perfilId) { this.perfilId = perfilId; }
}
