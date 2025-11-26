package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.IncidenciaController;
import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.ForbiddenException;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.models.Usuario;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class IncidenciaDialog extends JDialog {

    private final IncidenciaController controller;
    private final UserController userController;
    private final UserLoggedInDto currentUser;
    private final Long proyectoId;
    private final Runnable onSaved;

    private final JTextArea txtDescripcion = new JTextArea(5, 30);
    private final JTextField txtEstimacionHoras = new JTextField(10);
    private final JComboBox<UserDetailDto> comboResponsable = new JComboBox<>();
    private JButton btnOk;

    public IncidenciaDialog(Window owner,
                          IncidenciaController controller,
                          UserController userController,
                          UserLoggedInDto currentUser,
                          Long proyectoId,
                          Runnable onSaved) {
        super(owner, "Crear incidencia", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.userController = userController;
        this.currentUser = currentUser;
        this.proyectoId = proyectoId;
        this.onSaved = onSaved;

        buildUI();
        loadUsuarios();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        add(new JLabel("Descripción:"), gbc);

        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);
        scrollDesc.setPreferredSize(new Dimension(300, 100));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        add(scrollDesc, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Estimación de horas (opcional):"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        txtEstimacionHoras.setPreferredSize(new Dimension(150, 25));
        add(txtEstimacionHoras, gbc);

        // Responsable
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Responsable (opcional):"), gbc);

        comboResponsable.setPreferredSize(new Dimension(300, 25));
        comboResponsable.addItem(null); // Opción "Sin asignar"
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        add(comboResponsable, gbc);

        // Botones
        JButton btnCancel = new JButton("Cancelar");
        btnOk = new JButton("Crear");

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        add(btnCancel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        add(btnOk, gbc);

        btnOk.addActionListener(e -> doSave());
        btnCancel.addActionListener(e -> dispose());
    }

    private void loadUsuarios() {
        new SwingWorker<List<UserDetailDto>, Void>() {
            private Exception error;

            @Override
            protected List<UserDetailDto> doInBackground() {
                try {
                    return userController.getAll();
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(IncidenciaDialog.this,
                            "Error al cargar usuarios: " + error.getMessage());
                    return;
                }
                try {
                    List<UserDetailDto> usuarios = get();
                    if (usuarios != null) {
                        comboResponsable.removeAllItems();
                        comboResponsable.removeAllItems();
                        comboResponsable.addItem(null); // Opción "Sin asignar"
                        for (UserDetailDto usuario : usuarios) {
                            comboResponsable.addItem(usuario);
                        }
                        // Configurar renderer para mostrar nombre y apellido
                        comboResponsable.setRenderer(new DefaultListCellRenderer() {
                            @Override
                            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                          boolean isSelected, boolean cellHasFocus) {
                                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                                if (value == null) {
                                    setText("Sin asignar");
                                } else if (value instanceof UserDetailDto) {
                                    UserDetailDto usuario = (UserDetailDto) value;
                                    String nombreCompleto = usuario.getNombre() != null ? usuario.getNombre() : "";
                                    String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
                                    if (!apellido.isEmpty()) {
                                        nombreCompleto += " " + apellido;
                                    }
                                    setText(nombreCompleto.isEmpty() ? usuario.getEmail() : nombreCompleto);
                                }
                                return this;
                            }
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(IncidenciaDialog.this, "Error inesperado al cargar usuarios.");
                }
            }
        }.execute();
    }

    private void doSave() {
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";

        if (descripcion.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La descripción de la incidencia es obligatoria.");
            txtDescripcion.requestFocus();
            return;
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setDescripcion(descripcion);
        
        // Asignar el proyecto
        Proyecto proyecto = new Proyecto();
        proyecto.setId(proyectoId);
        incidencia.setProyecto(proyecto);

        String estimacionText = txtEstimacionHoras.getText() != null ? txtEstimacionHoras.getText().trim() : "";
        if (!estimacionText.isEmpty()) {
            try {
                BigDecimal estimacion = new BigDecimal(estimacionText);
                if (estimacion.compareTo(BigDecimal.ZERO) < 0) {
                    JOptionPane.showMessageDialog(this, "La estimación de horas debe ser un número positivo.");
                    txtEstimacionHoras.requestFocus();
                    return;
                }
                incidencia.setEstimacionHoras(estimacion);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "La estimación de horas debe ser un número válido.");
                txtEstimacionHoras.requestFocus();
                return;
            }
        }

        // Responsable (opcional)
        UserDetailDto responsableSeleccionado = (UserDetailDto) comboResponsable.getSelectedItem();
        if (responsableSeleccionado != null && responsableSeleccionado.getId() != null) {
            Usuario responsable = new Usuario();
            responsable.setId(responsableSeleccionado.getId());
            incidencia.setResponsable(responsable);
        }

        btnOk.setEnabled(false);

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    controller.create(incidencia, currentUser);
                    return null;
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                btnOk.setEnabled(true);
                if (error != null) {
                    String msg;
                    if (error instanceof ForbiddenException) {
                        msg = "No tenés permisos para realizar esta acción.";
                    } else if (error instanceof ValidationException) {
                        msg = error.getMessage();
                    } else if (error instanceof NotFoundException) {
                        msg = "Proyecto no encontrado.";
                    } else {
                        String errorMsg = error.getMessage();
                        msg = "Error guardando incidencia: " + errorMsg;
                    }
                    JOptionPane.showMessageDialog(IncidenciaDialog.this, msg);
                    return;
                }
                JOptionPane.showMessageDialog(IncidenciaDialog.this, "Incidencia creada exitosamente.");
                if (onSaved != null) onSaved.run();
                dispose();
            }
        }.execute();
    }
}

