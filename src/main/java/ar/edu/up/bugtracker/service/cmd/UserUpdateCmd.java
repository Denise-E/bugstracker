package ar.edu.up.bugtracker.service.cmd;

/**
 * Body esperado para actualizar un usuario.
 * Todos los campos son opcionales; el Controller valida que al menos uno venga informado.
 */
public class UserUpdateCmd {
    private String nombre;    // opcional
    private String apellido;  // opcional
    private String password;  // opcional (si viene, se re-hashea)
    private String perfil;    // opcional ("ADMIN" | "USUARIO")

    public UserUpdateCmd() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
}
