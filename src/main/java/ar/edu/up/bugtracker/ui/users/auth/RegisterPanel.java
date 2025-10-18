package ar.edu.up.bugtracker.ui.users.auth;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.controller.UserRoleController;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.models.PerfilUsuario;
import ar.edu.up.bugtracker.service.cmd.UserRegisterCmd;
import ar.edu.up.bugtracker.ui.PanelManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;


public class RegisterPanel extends JPanel {
    private static final Pattern EMAIL_RX =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PanelManager manager;
    private final UserController controller;
    private final UserRoleController roleController;

    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtApellido = new JTextField(18);
    private final JTextField txtEmail = new JTextField(24);
    private final JPasswordField txtPassword = new JPasswordField(24);
    private final JLabel lblPassHint = new JLabel("La contraseña debe tener al menos 6 caracteres.");
    private final JComboBox<PerfilUsuario> cbPerfil = new JComboBox<>();
    private final JLabel lblRolesHint = new JLabel("Cargando roles...");

    public RegisterPanel(PanelManager manager, UserController controller, UserRoleController roleController) {
        this.manager = manager;
        this.controller = controller;
        this.roleController = roleController;
        buildUI();
        loadRolesAsync();
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

        // Rol (dinámico) + hint de carga
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; add(new JLabel("Rol*:"), gbc);
        cbPerfil.setRenderer(new RoleRenderer());
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; add(cbPerfil, gbc);

        lblRolesHint.setFont(lblRolesHint.getFont().deriveFont(Font.PLAIN, 11f));
        lblRolesHint.setForeground(Color.DARK_GRAY);
        gbc.gridy++; gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(lblRolesHint, gbc);

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

        // Mientras cargan roles, evitar registrar
        btnOk.setEnabled(false);
        cbPerfil.setEnabled(false);
    }

    private void clearForm() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtEmail.setText("");
        txtPassword.setText("");
        // Seleccionar siempre "USUARIO" como default si existe
        selectRoleByName("USUARIO");
    }

    /** Carga de roles desde BD de forma asíncrona y selecciona "USUARIO" por defecto. */
    private void loadRolesAsync() {
        new SwingWorker<List<PerfilUsuario>, Void>() {
            private Exception error;
            @Override protected List<PerfilUsuario> doInBackground() {
                try {
                    return roleController.getAll();
                } catch (Exception ex) {
                    this.error = ex; return null;
                }
            }
            @Override protected void done() {
                if (error != null) {
                    lblRolesHint.setText("No se pudieron cargar los roles. Contactá al administrador.");
                    btnOk.setEnabled(false);
                    cbPerfil.setEnabled(false);
                    return;
                }
                try {
                    List<PerfilUsuario> roles = get();
                    DefaultComboBoxModel<PerfilUsuario> model = new DefaultComboBoxModel<>();
                    if (roles != null) {
                        for (PerfilUsuario r : roles) model.addElement(r);
                    }
                    cbPerfil.setModel(model);

                    boolean hasRoles = cbPerfil.getItemCount() > 0;
                    lblRolesHint.setText(hasRoles ? " " : "No hay roles configurados. Contactá al administrador.");
                    btnOk.setEnabled(hasRoles);
                    cbPerfil.setEnabled(hasRoles);

                    // Default: "USUARIO"
                    selectRoleByName("USUARIO");

                } catch (Exception e) {
                    lblRolesHint.setText("Error inesperado al cargar roles.");
                    btnOk.setEnabled(false);
                    cbPerfil.setEnabled(false);
                }
            }
        }.execute();
    }

    /** Selecciona en el combo el rol cuyo nombre coincida (case-insensitive). */
    private void selectRoleByName(String name) {
        if (name == null) return;
        ComboBoxModel<PerfilUsuario> m = cbPerfil.getModel();
        for (int i = 0; i < m.getSize(); i++) {
            PerfilUsuario r = m.getElementAt(i);
            if (r != null && r.getNombre() != null && r.getNombre().equalsIgnoreCase(name)) {
                cbPerfil.setSelectedIndex(i);
                return;
            }
        }
        // Si no existe "USUARIO" pero hay roles, seleccionar el primero.
        if (m.getSize() > 0 && cbPerfil.getSelectedIndex() < 0) cbPerfil.setSelectedIndex(0);
    }

    private void doRegister() {
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String email = txtEmail.getText().trim();

        String pass = new String(txtPassword.getPassword());
        PerfilUsuario seleccionado = (PerfilUsuario) cbPerfil.getSelectedItem();

        // Validaciones solicitadas
        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || pass.isEmpty() || seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Completá los campos obligatorios (*).");
            return;
        }

        if (!EMAIL_RX.matcher(email).matches()) {
            JOptionPane.showMessageDialog(this, "Mail invalido");
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
                    // enviar FK (ID de rol) al backend
                    cmd.setPerfilId(seleccionado.getId());
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

    /** Renderer: muestra el nombre del rol en el combo. */
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
