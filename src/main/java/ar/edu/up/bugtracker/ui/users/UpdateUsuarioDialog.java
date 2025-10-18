package ar.edu.up.bugtracker.ui.users;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.controller.UserRoleController;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.models.PerfilUsuario;
import ar.edu.up.bugtracker.service.cmd.UserUpdateCmd;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.util.List;

public class UpdateUsuarioDialog extends JDialog {

    private final UserController controller;
    private final UserRoleController roleController;
    private final Long userId;
    private final Runnable onSaved;

    private final JComboBox<PerfilUsuario> cbPerfil = new JComboBox<>();
    private final JLabel lblRolesHint = new JLabel("Cargando roles...");

    private JButton btnOk;

    public UpdateUsuarioDialog(Window owner,
                               UserController controller,
                               UserRoleController roleController,
                               Long userId,
                               Runnable onSaved) {
        super(owner, "Actualizar usuario", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.roleController = roleController;
        this.userId = userId;
        this.onSaved = onSaved;

        buildUI();
        loadRolesAndUserAsync();

        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Rol
        gbc.gridx=0; gbc.gridy=0; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Rol*:"), gbc);

        cbPerfil.setRenderer(new RoleRenderer());
        gbc.gridx=1; gbc.anchor = GridBagConstraints.WEST;
        add(cbPerfil, gbc);

        // Hint roles
        lblRolesHint.setFont(lblRolesHint.getFont().deriveFont(Font.PLAIN, 11f));
        lblRolesHint.setForeground(Color.DARK_GRAY);
        gbc.gridy++; gbc.gridx=1; gbc.anchor = GridBagConstraints.WEST;
        add(lblRolesHint, gbc);

        // Botones (Cancelar izq, Guardar der)
        JButton btnCancel = new JButton("Cancelar");
        btnOk     = new JButton("Guardar");

        gbc.gridy++; gbc.gridx=0; gbc.anchor = GridBagConstraints.WEST;
        add(btnCancel, gbc);
        gbc.gridx=1; gbc.anchor = GridBagConstraints.EAST;
        add(btnOk, gbc);

        btnOk.addActionListener(e -> doSave());
        btnCancel.addActionListener(e -> dispose());

        // Deshabilitar hasta cargar roles
        btnOk.setEnabled(false);
        cbPerfil.setEnabled(false);
    }

    /** Carga roles desde BD y datos del usuario para preseleccionar su rol actual. */
    private void loadRolesAndUserAsync() {
        new SwingWorker<LoadResult, Void>() {
            private Exception error;
            @Override protected LoadResult doInBackground() {
                try {
                    List<PerfilUsuario> roles = roleController.getAll();
                    UserDetailDto dto = controller.getById(userId);
                    return new LoadResult(roles, dto);
                } catch (Exception ex) { this.error = ex; return null; }
            }
            @Override protected void done() {
                if (error != null) {
                    lblRolesHint.setText("No se pudieron cargar roles/usuario. Intentalo nuevamente.");
                    return;
                }
                try {
                    LoadResult r = get();
                    DefaultComboBoxModel<PerfilUsuario> model = new DefaultComboBoxModel<>();
                    if (r.roles != null) for (PerfilUsuario p : r.roles) model.addElement(p);
                    cbPerfil.setModel(model);

                    // Selecciona por nombre (UserDetailDto expone nombre de perfil)
                    String currentRoleName = (r.user != null ? r.user.getPerfil() : null);
                    if (currentRoleName != null) selectRoleByName(currentRoleName);

                    boolean hasItems = cbPerfil.getItemCount() > 0;
                    cbPerfil.setEnabled(hasItems);
                    btnOk.setEnabled(hasItems);
                    lblRolesHint.setText(hasItems ? " " : "No hay roles configurados. Contactá al administrador.");
                } catch (Exception e) {
                    lblRolesHint.setText("Error inesperado al preparar la pantalla.");
                }
            }
        }.execute();
    }

    private void selectRoleByName(String name) {
        ComboBoxModel<PerfilUsuario> m = cbPerfil.getModel();
        for (int i = 0; i < m.getSize(); i++) {
            PerfilUsuario r = m.getElementAt(i);
            if (r != null && r.getNombre() != null && r.getNombre().equalsIgnoreCase(name)) {
                cbPerfil.setSelectedIndex(i);
                return;
            }
        }
        if (m.getSize() > 0) cbPerfil.setSelectedIndex(0);
    }

    private void doSave() {
        PerfilUsuario seleccionado = (PerfilUsuario) cbPerfil.getSelectedItem();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un rol válido.");
            return;
        }

        new SwingWorker<Void, Void>() {
            private Exception error;
            @Override protected Void doInBackground() {
                try {
                    UserUpdateCmd cmd = new UserUpdateCmd();
                    // Enviar ID del rol (sin hardcode)
                    cmd.setPerfilId(seleccionado.getId());
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

    // Helper para carga inicial (compatible Java 11)
    private static class LoadResult {
        final List<PerfilUsuario> roles;
        final UserDetailDto user;
        LoadResult(List<PerfilUsuario> roles, UserDetailDto user) {
            this.roles = roles;
            this.user = user;
        }
    }

    // Renderer para mostrar nombre del rol
    private static class RoleRenderer extends BasicComboBoxRenderer {
        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PerfilUsuario) {
                setText(((PerfilUsuario) value).getNombre());
            } else if (value == null) {
                setText("");
            }
            return this;
        }
    }
}
