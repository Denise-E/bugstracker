package ar.edu.up.bugtracker.service.cmd;

/**
 * Body esperado para el registro un usuario.
 * Campos obligatorios: nombre, email, password, perfil ("ADMIN" | "USUARIO").
 */
public class UserRegisterCmd {
    private String nombre;
    private String apellido; // opcional
    private String email;
    private String password;
    private String perfil;

    public UserRegisterCmd() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
}
