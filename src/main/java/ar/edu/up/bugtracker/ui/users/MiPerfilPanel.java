package ar.edu.up.bugtracker.ui.users;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.service.cmd.UserUpdateCmd;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import ar.edu.up.bugtracker.ui.PanelManager;

import javax.swing.*;
import java.awt.*;

public class MiPerfilPanel extends JPanel {

    private final PanelManager manager;
    private final UserController controller;
    private final UserLoggedInDto session;

    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtApellido = new JTextField(18);
    private final JTextField txtEmail = new JTextField(24);
    private final JTextField txtPerfil = new JTextField(10);
    private final JPasswordField txtPassword = new JPasswordField(24);

    public MiPerfilPanel(PanelManager manager, UserController controller, UserLoggedInDto session) {
        this.manager = manager;
        this.controller = controller;
        this.session = session;
        buildUI();
        loadData();
    }

    private JButton btnGuardar;
    private JButton btnCancelar;

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Mi Perfil");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; add(title, gbc);
        gbc.gridwidth=1;

        gbc.gridy++; gbc.gridx=0; add(new JLabel("Nombre:"), gbc);
        gbc.gridx=1; add(txtNombre, gbc);

        gbc.gridy++; gbc.gridx=0; add(new JLabel("Apellido:"), gbc);
        gbc.gridx=1; add(txtApellido, gbc);

        gbc.gridy++; gbc.gridx=0; add(new JLabel("Email:"), gbc);
        gbc.gridx=1; add(txtEmail, gbc);

        gbc.gridy++; gbc.gridx=0; add(new JLabel("Rol:"), gbc);
        txtPerfil.setEnabled(false);
        gbc.gridx=1; add(txtPerfil, gbc);

        gbc.gridy++; gbc.gridx=0; add(new JLabel("Contraseña:"), gbc);
        txtPassword.setEnabled(false);
        gbc.gridx=1; add(txtPassword, gbc);

        // Botones: izq = Cancelar cambios (pequeño), der = Guardar cambios (grande)
        btnCancelar = new JButton("Cancelar cambios");
        btnGuardar  = new JButton("Guardar cambios");
        btnGuardar.setPreferredSize(new Dimension(180, 36)); // más grande a la derecha

        gbc.gridy++; gbc.gridx=0; gbc.anchor = GridBagConstraints.WEST;
        add(btnCancelar, gbc);
        gbc.gridx=1; gbc.anchor = GridBagConstraints.EAST;
        add(btnGuardar, gbc);

        btnGuardar.addActionListener(e -> doUpdate());
        btnCancelar.addActionListener(e -> loadData());
    }

    private void loadData() {
        try {
            UserDetailDto dto = controller.getById(session.getId());
            txtNombre.setText(dto.getNombre());
            txtApellido.setText(dto.getApellido());
            txtEmail.setText(dto.getEmail());
            txtPerfil.setText(dto.getPerfil());
            txtPassword.setText("********");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el perfil: " + e.getMessage());
        }
    }

    private void doUpdate() {
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
            return;
        }

        new SwingWorker<Void, Void>() {
            private Exception error;
            @Override protected Void doInBackground() {
                try {
                    UserUpdateCmd cmd = new UserUpdateCmd();
                    cmd.setNombre(nombre);
                    cmd.setApellido(apellido);
                    controller.update(session.getId(), cmd);
                    return null;
                } catch (Exception ex) { this.error = ex; return null; }
            }
            @Override protected void done() {
                if (error != null) {
                    String msg = (error instanceof ValidationException) ? error.getMessage() : "Error al guardar cambios.";
                    JOptionPane.showMessageDialog(MiPerfilPanel.this, msg);
                    return;
                }
                JOptionPane.showMessageDialog(MiPerfilPanel.this, "Perfil actualizado.");
                loadData();
            }
        }.execute();
    }
}
