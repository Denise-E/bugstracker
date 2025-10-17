package ar.edu.up.bugtracker.ui.users.auth;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.AuthException;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.service.cmd.UserLoginCmd;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import ar.edu.up.bugtracker.ui.PanelManager;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {

    private final PanelManager manager;
    private final UserController controller;

    private final JTextField txtEmail = new JTextField(24);
    private final JPasswordField txtPassword = new JPasswordField(24);

    public LoginPanel(PanelManager manager, UserController controller) {
        this.manager = manager;
        this.controller = controller;
        buildUI();
    }

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Iniciar sesión");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; add(txtEmail, gbc);

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1; add(txtPassword, gbc);

        JButton btnLogin = new JButton("Iniciar sesión");
        JButton btnRegister = new JButton("Registrarme");

        gbc.gridy++; gbc.gridx = 0; add(btnLogin, gbc);
        gbc.gridx = 1; add(btnRegister, gbc);

        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> manager.showRegister());
    }

    private void doLogin() {
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword());

        // Validación superficial (controller también valida)
        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completá email y contraseña.");
            return;
        }

        // No bloquear el EDT
        new SwingWorker<UserLoggedInDto, Void>() {
            private Exception error;
            @Override protected UserLoggedInDto doInBackground() {
                try {
                    UserLoginCmd cmd = new UserLoginCmd();
                    cmd.setEmail(email);
                    cmd.setPassword(pass);
                    return controller.login(cmd);
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }
            @Override protected void done() {
                if (error != null) {
                    String msg = (error instanceof ValidationException) ? error.getMessage()
                            : (error instanceof AuthException) ? "Credenciales inválidas"
                            : "Error inesperado";
                    JOptionPane.showMessageDialog(LoginPanel.this, msg);
                    return;
                }
                try {
                    UserLoggedInDto user = get();
                    manager.onLoginSuccess(user);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(LoginPanel.this, "Error inesperado");
                }
            }
        }.execute();
    }
}
