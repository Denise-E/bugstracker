package ar.edu.up.bugtracker.ui.users.auth;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.service.cmd.UserRegisterCmd;
import ar.edu.up.bugtracker.ui.PanelManager;

import javax.swing.*;
import java.awt.*;

public class RegisterPanel extends JPanel {

    private final PanelManager manager;
    private final UserController controller;

    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtApellido = new JTextField(18);
    private final JTextField txtEmail = new JTextField(24);
    private final JPasswordField txtPassword = new JPasswordField(24);
    private final JComboBox<String> cbPerfil = new JComboBox<>(new String[] {"USUARIO"}); // Autoregistro: solo USUARIO

    public RegisterPanel(PanelManager manager, UserController controller) {
        this.manager = manager;
        this.controller = controller;
        buildUI();
    }

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Registro de usuario");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; add(title, gbc);
        gbc.gridwidth = 1;

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Nombre*:"), gbc);
        gbc.gridx = 1; add(txtNombre, gbc);

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Apellido:"), gbc);
        gbc.gridx = 1; add(txtApellido, gbc);

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Email*:"), gbc);
        gbc.gridx = 1; add(txtEmail, gbc);

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Contraseña*:"), gbc);
        gbc.gridx = 1; add(txtPassword, gbc);

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Perfil:"), gbc);
        gbc.gridx = 1; add(cbPerfil, gbc);

        JButton btnOk = new JButton("Registrarme");
        JButton btnCancel = new JButton("Cancelar");

        gbc.gridy++; gbc.gridx = 0; add(btnOk, gbc);
        gbc.gridx = 1; add(btnCancel, gbc);

        btnOk.addActionListener(e -> doRegister());
        btnCancel.addActionListener(e -> {
            clearForm();
            manager.showLogin();
        });
    }

    private void clearForm() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtEmail.setText("");
        txtPassword.setText("");
        cbPerfil.setSelectedIndex(0);
    }

    private void doRegister() {
        String nombre = txtNombre.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completá los campos obligatorios (*).");
            return;
        }

        new SwingWorker<Long, Void>() {
            private Exception error;
            @Override protected Long doInBackground() {
                try {
                    UserRegisterCmd cmd = new UserRegisterCmd();
                    cmd.setNombre(nombre);
                    cmd.setApellido(txtApellido.getText().trim());
                    cmd.setEmail(email);
                    cmd.setPassword(pass);
                    cmd.setPerfil(cbPerfil.getSelectedItem().toString()); // USUARIO
                    return controller.register(cmd);
                } catch (Exception ex) {
                    this.error = ex; return null;
                }
            }
            @Override protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(RegisterPanel.this,
                            (error instanceof ValidationException) ? error.getMessage() : "Error inesperado");
                    return;
                }
                JOptionPane.showMessageDialog(RegisterPanel.this, "Registro exitoso. Iniciá sesión.");
                clearForm();
                manager.showLogin();
            }
        }.execute();
    }
}
