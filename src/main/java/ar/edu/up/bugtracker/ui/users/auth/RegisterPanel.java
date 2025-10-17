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
    private final JLabel lblPassHint = new JLabel("La contraseña debe tener al menos 6 caracteres.");
    private final JComboBox<String> cbPerfil = new JComboBox<>(new String[] {"USUARIO", "ADMIN"});

    public RegisterPanel(PanelManager manager, UserController controller) {
        this.manager = manager;
        this.controller = controller;
        buildUI();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.getRootPane(this).setDefaultButton(btnOk);
    }

    private JButton btnOk;     // "Registrarme"
    private JButton btnCancel; // "Cancelar"

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Registro de usuario");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(title, gbc);

        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Nombre*:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; add(txtNombre, gbc);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Apellido*:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; add(txtApellido, gbc);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Email*:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; add(txtEmail, gbc);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Contraseña*:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; add(txtPassword, gbc);

        // Hint de contraseña (texto pequeño)
        lblPassHint.setFont(lblPassHint.getFont().deriveFont(Font.PLAIN, 11f));
        lblPassHint.setForeground(Color.DARK_GRAY);
        gbc.gridy++; gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(lblPassHint, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; add(new JLabel("Rol*:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; add(cbPerfil, gbc);

        // Botones: Cancelar (izq) y Registrarme (der, grande)
        btnCancel = new JButton("Cancelar");
        btnOk = new JButton("Registrarme");
        btnOk.setPreferredSize(new Dimension(180, 36));

        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.WEST;
        add(btnCancel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST;
        add(btnOk, gbc);

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
        String apellido = txtApellido.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword());

        // Validaciones solicitadas
        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completá los campos obligatorios (*).");
            return;
        }
        if (pass.length() < 6) {
            JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        new SwingWorker<Long, Void>() {
            private Exception error;
            @Override protected Long doInBackground() {
                try {
                    UserRegisterCmd cmd = new UserRegisterCmd();
                    cmd.setNombre(nombre);
                    cmd.setApellido(apellido);
                    cmd.setEmail(email);
                    cmd.setPassword(pass);
                    cmd.setPerfil(cbPerfil.getSelectedItem().toString());
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
