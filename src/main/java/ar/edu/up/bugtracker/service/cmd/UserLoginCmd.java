package ar.edu.up.bugtracker.service.cmd;

/**
 * Body esperado para el login de un usuario.
 * Campos obligatorios: email, password.
 */
public class UserLoginCmd {
    private String email;
    private String password;

    public UserLoginCmd() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
