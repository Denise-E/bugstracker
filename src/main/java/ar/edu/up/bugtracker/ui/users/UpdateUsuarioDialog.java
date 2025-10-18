package ar.edu.up.bugtracker.ui.users;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.controller.UserRoleController;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.models.PerfilUsuario;
import ar.edu.up.bugtracker.service.cmd.UserUpdateCmd;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.util.List;

public class UpdateUsuarioDialog extends JDialog {

    private final UserController controller;
    private final UserRoleController roleController;
    private final Long userId;
    private final Runnable onSaved;

    // NUEVO: datos de la fila (no del usuario logueado)
    private final Long initialPerfilId;
    private final String initialPerfilName;

    private final JComboBox<PerfilUsuario> cbPerfil = new JComboBox<>();
    private final JLabel lblRolesHint = new JLabel("Cargando roles...");

    private JButton btnOk;

    public UpdateUsuarioDialog(Window owner,
                               UserController controller,
                               UserRoleController roleController,
                               Long userId,
                               Long initialPerfilId,          // <- NUEVO
                               String initialPerfilName,      // <- NUEVO
                               Runnable onSaved) {
        super(owner, "Actualizar usuario", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.roleController = roleController;
        this.userId = userId;
        this.initialPerfilId = initialPerfilId;
        this.initialPerfilName = initialPerfilName;
        this.onSaved = onSaved;

        buildUI();
        loadRolesAsyncAndPreselect();

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
        add(new JLabel("Rol:"), gbc);

        cbPerfil.setRenderer(new RoleRenderer());
        gbc.gridx=1; gbc.anchor = GridBagConstraints.WEST;
        add(cbPerfil, gbc);

        lblRolesHint.setFont(lblRolesHint.getFont().deriveFont(Font.PLAIN, 11f));
        lblRolesHint.setForeground(Color.DARK_GRAY);
        gbc.gridy++; gbc.gridx=1; gbc.anchor = GridBagConstraints.WEST;
        add(lblRolesHint, gbc);

        JButton btnCancel = new JButton("Cancelar");
        btnOk     = new JButton("Guardar");

        gbc.gridy++; gbc.gridx=0; gbc.anchor = GridBagConstraints.WEST;
        add(btnCancel, gbc);
        gbc.gridx=1; gbc.anchor = GridBagConstraints.EAST;
        add(btnOk, gbc);

        btnOk.addActionListener(e -> doSave());
        btnCancel.addActionListener(e -> dispose());

        btnOk.setEnabled(false);
        cbPerfil.setEnabled(false);
    }

    // Carga roles (solo roles) y preselecciona usando los datos de la fila
    private void loadRolesAsyncAndPreselect() {
        new SwingWorker<List<PerfilUsuario>, Void>() {
            private Exception error;
            @Override protected List<PerfilUsuario> doInBackground() {
                try {
                    return roleController.getAll();
                } catch (Exception ex) { this.error = ex; return null; }
            }
            @Override protected void done() {
                if (error != null) {
                    System.out.println("[UpdateUsuarioDialog] ERROR cargando roles: " + error);
                    lblRolesHint.setText("No se pudieron cargar roles. Intentalo nuevamente.");
                    return;
                }
                try {
                    List<PerfilUsuario> roles = get();

                    System.out.println("[UpdateUsuarioDialog] userId=" + userId);
                    System.out.println("[UpdateUsuarioDialog] Roles recibidos:");
                    if (roles != null) {
                        for (PerfilUsuario p : roles) {
                            System.out.println("  - id=" + p.getId() + " nombre=" + p.getNombre());
                        }
                    } else {
                        System.out.println("  (sin roles)");
                    }
                    System.out.println("[UpdateUsuarioDialog] Valor inicial desde la fila: perfilId="
                            + initialPerfilId + " perfilName=" + initialPerfilName);

                    DefaultComboBoxModel<PerfilUsuario> model = new DefaultComboBoxModel<>();
                    if (roles != null) for (PerfilUsuario p : roles) model.addElement(p);
                    cbPerfil.setModel(model);

                    boolean selected = false;

                    // Primero, si vino ID desde la fila
                    if (initialPerfilId != null) {
                        selected = selectRoleById(initialPerfilId);
                        System.out.println("[UpdateUsuarioDialog] selectRoleById(" + initialPerfilId + ") => " + selected);
                    }

                    // Si no se pudo por ID y vino nombre desde la fila
                    if (!selected && initialPerfilName != null && !initialPerfilName.trim().isEmpty()) {
                        selected = selectRoleByName(initialPerfilName.trim());
                        System.out.println("[UpdateUsuarioDialog] selectRoleByName(" + initialPerfilName + ") => " + selected);
                    }

                    // Sin fallback: si no hay match, dejamos sin selección
                    if (!selected) {
                        cbPerfil.setSelectedItem(null);
                        System.out.println("[UpdateUsuarioDialog] Sin coincidencia -> combo sin selección.");
                    }

                    boolean hasItems = cbPerfil.getItemCount() > 0;
                    cbPerfil.setEnabled(hasItems);
                    btnOk.setEnabled(hasItems);
                    lblRolesHint.setText(hasItems ? " " : "No hay roles configurados. Contactá al administrador.");

                    PerfilUsuario sel = (PerfilUsuario) cbPerfil.getSelectedItem();
                    System.out.println("[UpdateUsuarioDialog] Selección final: " +
                            (sel == null ? "null" : ("id=" + sel.getId() + " nombre=" + sel.getNombre())));
                } catch (Exception e) {
                    System.out.println("[UpdateUsuarioDialog] ERROR preparando pantalla: " + e);
                    lblRolesHint.setText("Error inesperado al preparar la pantalla.");
                }
            }
        }.execute();
    }

    private boolean selectRoleById(Long id) {
        if (id == null) return false;
        ComboBoxModel<PerfilUsuario> m = cbPerfil.getModel();
        for (int i = 0; i < m.getSize(); i++) {
            PerfilUsuario r = m.getElementAt(i);
            Long rid = toLong(r != null ? r.getId() : null);
            System.out.println("[UpdateUsuarioDialog] Comparando combo[i=" + i + "] rid=" + rid + " vs perfilId=" + id);
            if (rid != null && rid.longValue() == id.longValue()) {
                cbPerfil.setSelectedIndex(i);
                System.out.println("[UpdateUsuarioDialog] MATCH por ID en i=" + i);
                return true;
            }
        }
        return false;
    }

    private boolean selectRoleByName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ComboBoxModel<PerfilUsuario> m = cbPerfil.getModel();
        for (int i = 0; i < m.getSize(); i++) {
            PerfilUsuario r = m.getElementAt(i);
            String rn = (r != null ? r.getNombre() : null);
            System.out.println("[UpdateUsuarioDialog] Comparando combo[i=" + i + "] nombre='" + rn + "' vs '" + name + "'");
            if (rn != null && rn.equalsIgnoreCase(name.trim())) {
                cbPerfil.setSelectedIndex(i);
                System.out.println("[UpdateUsuarioDialog] MATCH por NOMBRE en i=" + i);
                return true;
            }
        }
        return false;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            String s = ((String) v).trim();
            if (s.isEmpty()) return null;
            try { return Long.valueOf(s); } catch (NumberFormatException ignored) { return null; }
        }
        return null;
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
                    cmd.setPerfilId(toLong(seleccionado.getId()));
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
