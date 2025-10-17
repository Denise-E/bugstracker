package ar.edu.up.bugtracker.ui.users;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.service.cmd.UserUpdateCmd;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;

import javax.swing.*;
import java.awt.*;

public class UpdateUsuarioDialog extends JDialog {

    private final UserController controller;
    private final Long userId;
    private final Runnable onSaved;

    private final JComboBox<String> cbPerfil = new JComboBox<>(new String[]{"ADMIN","USUARIO"});

    public UpdateUsuarioDialog(Window owner, UserController controller, Long userId, Runnable onSaved) {
        super(owner, "Actualizar usuario", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.userId = userId;
        this.onSaved = onSaved;

        buildUI();
        loadData();

        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx=0; gbc.gridy=0; add(new JLabel("Rol:"), gbc);
        gbc.gridx=1; add(cbPerfil, gbc);

        JButton btnCancel = new JButton("Cancelar");
        JButton btnOk     = new JButton("Guardar");

        // Determina ubicacion botones
        gbc.gridy++; gbc.gridx=0; add(btnCancel, gbc);
        gbc.gridx=1; add(btnOk, gbc);

        btnOk.addActionListener(e -> doSave());
        btnCancel.addActionListener(e -> dispose());
    }

    private void loadData() {
        try {
            UserDetailDto dto = controller.getById(userId);
            cbPerfil.setSelectedItem(dto.getPerfil() == null ? "USUARIO" : dto.getPerfil());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el usuario.");
            dispose();
        }
    }

    private void doSave() {
        String perfil = (String) cbPerfil.getSelectedItem();

        new SwingWorker<Void, Void>() {
            private Exception error;
            @Override protected Void doInBackground() {
                try {
                    UserUpdateCmd cmd = new UserUpdateCmd();
                    cmd.setPerfil(perfil); // sólo rol
                    controller.update(userId, cmd);
                    return null;
                } catch (Exception ex) { this.error = ex; return null; }
            }
            @Override protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(UpdateUsuarioDialog.this,
                            (error instanceof ValidationException) ? error.getMessage() : "Error guardando cambios.");
                    return;
                }
                JOptionPane.showMessageDialog(UpdateUsuarioDialog.this, "Cambios guardados.");
                if (onSaved != null) onSaved.run();
                dispose();
            }
        }.execute();
    }
}
