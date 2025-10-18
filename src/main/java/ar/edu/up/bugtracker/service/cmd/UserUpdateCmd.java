package ar.edu.up.bugtracker.service.cmd;

/**
 * Body esperado para actualizar un usuario.
 */
public class UserUpdateCmd {
    private String nombre;
    private String apellido;
    private String password;
    private Long perfilId;

    public UserUpdateCmd() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getPerfilId() { return perfilId; }
    public void setPerfilId(Long perfilId) { this.perfilId = perfilId; }
}
