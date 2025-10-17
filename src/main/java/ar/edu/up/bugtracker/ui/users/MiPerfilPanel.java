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

    // Campos editables
    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtApellido = new JTextField(18);
    private final JTextField txtEmail = new JTextField(24);       // editable? → Sí, tu flujo dice que Mi Perfil puede modificar todo excepto rol y contraseña
    private final JTextField txtPerfil = new JTextField(10);      // NO editable
    private final JPasswordField txtPassword = new JPasswordField(24); // NO visible en edición, lo dejamos disabled

    public MiPerfilPanel(PanelManager manager, UserController controller, UserLoggedInDto session) {
        this.manager = manager;
        this.controller = controller;
        this.session = session;
        buildUI();
        loadData();
    }

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

        JButton btnGuardar = new JButton("Guardar cambios");
        JButton btnReset = new JButton("Revertir");
        gbc.gridy++; gbc.gridx=0; add(btnGuardar, gbc);
        gbc.gridx=1; add(btnReset, gbc);

        btnGuardar.addActionListener(e -> doUpdate());
        btnReset.addActionListener(e -> loadData());
    }

    private void loadData() {
        // Traer detalle actualizado
        try {
            UserDetailDto dto = controller.getById(session.getId());
            txtNombre.setText(dto.getNombre());
            txtApellido.setText(dto.getApellido());
            txtEmail.setText(dto.getEmail());
            txtPerfil.setText(dto.getPerfil());
            txtPassword.setText("********"); // no editable
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el perfil: " + e.getMessage());
        }
    }

    private void doUpdate() {
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String email = txtEmail.getText().trim(); // si no querés que sea editable acá, deshabilitá el campo y eliminá este set

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
                    // Si NO deseás permitir cambio de email en Mi Perfil, comentá la próxima línea.
                    // (Tu definición dice que acá no se edita rol ni contraseña; el email sí puede editarse.)
                    // Si lo permitís, deberías validar unicidad en Service (ya lo hace).
                    // cmd.setPerfil(null);
                    // cmd.setPassword(null);
                    controller.update(session.getId(), cmd);
                    return null;
                } catch (Exception ex) {
                    this.error = ex; return null;
                }
            }
            @Override protected void done() {
                if (error != null) {
                    String msg = (error instanceof ValidationException) ? error.getMessage()
                            : "Error al guardar cambios.";
                    JOptionPane.showMessageDialog(MiPerfilPanel.this, msg);
                    return;
                }
                JOptionPane.showMessageDialog(MiPerfilPanel.this, "Perfil actualizado.");
                loadData();
            }
        }.execute();
    }
}
