package ar.edu.up.bugtracker.ui.users.auth;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.AuthException;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.service.cmd.UserLoginCmd;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import ar.edu.up.bugtracker.ui.PanelManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

    @Override
    public void addNotify() {
        super.addNotify();
        // Botón por defecto (Enter) = Iniciar sesión
        SwingUtilities.getRootPane(this).setDefaultButton(btnLogin);
    }

    // --- UI ---

    private JButton btnLogin; // necesitamos referencia para setDefaultButton

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("Iniciar sesión");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        // Título centrado
        gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(title, gbc);

        // Email
        gbc.gridwidth = 1; gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(txtEmail, gbc);

        // Password
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(txtPassword, gbc);

        // Botón principal centrado
        btnLogin = new JButton("Iniciar sesión");
        btnLogin.setPreferredSize(new Dimension(180, 36));

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(btnLogin, gbc);

        // “¿Todavía no tenés un usuario? Registrate acá” como hipervínculo, centrado debajo
        JLabel linkRegister = new JLabel(
                "<html><a href='#'>¿Todavía no tenés un usuario? Registrate acá</a></html>",
                SwingConstants.CENTER
        );
        linkRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(linkRegister, gbc);

        // Listeners
        btnLogin.addActionListener(e -> doLogin());
        linkRegister.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                manager.showRegister();
            }
        });
    }

    // --- Acciones ---

    private void doLogin() {
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completá email y contraseña.");
            return;
        }

        new SwingWorker<UserLoggedInDto, Void>() {
            private Exception error;
            @Override protected UserLoggedInDto doInBackground() {
                try {
                    UserLoginCmd cmd = new UserLoginCmd();
                    cmd.setEmail(email);
                    cmd.setPassword(pass);
                    return controller.login(cmd);
                } catch (Exception ex) {
                    this.error = ex; return null;
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
