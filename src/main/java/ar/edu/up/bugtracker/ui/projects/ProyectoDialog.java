package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.exceptions.ForbiddenException;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import javax.swing.*;
import java.awt.*;

public class ProyectoDialog extends JDialog {

    private final ProyectoController controller;
    private final UserLoggedInDto currentUser;
    private final Long proyectoId; // null al crear un proyecto
    private final Runnable onSaved;

    private final JTextField txtNombre = new JTextField(30);
    private final JTextArea txtDescripcion = new JTextArea(5, 30);
    private final JButton btnOk;

    public ProyectoDialog(Window owner,
                          ProyectoController controller,
                          UserLoggedInDto currentUser,
                          Long proyectoId,
                          Runnable onSaved) {
        super(owner, proyectoId == null ? "Crear proyecto" : "Editar proyecto", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.currentUser = currentUser;
        this.proyectoId = proyectoId;
        this.onSaved = onSaved;

        buildUI();
        if (proyectoId != null) {
            loadProyectoAsync();
        }
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
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Nombre:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        add(txtNombre, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        add(new JLabel("Descripción:"), gbc);

        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);
        scrollDesc.setPreferredSize(new Dimension(300, 100));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(scrollDesc, gbc);

        JButton btnCancel = new JButton("Cancelar");
        btnOk = new JButton(proyectoId == null ? "Crear" : "Guardar");

        gbc.gridx = 0;
        gbc.gridy = 2;
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

        // Validación permisos
        if (proyectoId == null) {
            boolean canCreate = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getPerfil());
            btnOk.setEnabled(canCreate);
            if (!canCreate) {
                txtNombre.setEnabled(false);
                txtDescripcion.setEnabled(false);
            }
        }
    }

    private void loadProyectoAsync() {
        btnOk.setEnabled(false);
        txtNombre.setEnabled(false);
        txtDescripcion.setEnabled(false);

        new SwingWorker<Proyecto, Void>() {
            private Exception error;

            @Override
            protected Proyecto doInBackground() {
                try {
                    return controller.getById(proyectoId);
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    String msg;
                    if (error instanceof NotFoundException) {
                        msg = "Proyecto no encontrado.";
                    } else {
                        msg = "Error al cargar proyecto: " + error.getMessage();
                    }
                    JOptionPane.showMessageDialog(ProyectoDialog.this, msg);
                    dispose();
                    return;
                }
                try {
                    Proyecto proyecto = get();
                    if (proyecto != null) {
                        txtNombre.setText(proyecto.getNombre() != null ? proyecto.getNombre() : "");
                        txtDescripcion.setText(proyecto.getDescripcion() != null ? proyecto.getDescripcion() : "");
                    }
                    txtNombre.setEnabled(true);
                    txtDescripcion.setEnabled(true);
                    btnOk.setEnabled(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProyectoDialog.this, "Error inesperado.");
                    dispose();
                }
            }
        }.execute();
    }

    private void doSave() {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre del proyecto es obligatorio.");
            txtNombre.requestFocus();
            return;
        }

        Proyecto proyecto = new Proyecto();
        proyecto.setNombre(nombre);
        proyecto.setDescripcion(descripcion.isEmpty() ? null : descripcion);

        btnOk.setEnabled(false);

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    if (proyectoId == null) {
                        // Crear
                        controller.create(proyecto, currentUser);
                    } else {
                        // Actualizar
                        controller.update(proyectoId, proyecto);
                    }
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
                        msg = "Error guardando proyecto: " + error.getMessage();
                    }
                    JOptionPane.showMessageDialog(ProyectoDialog.this, msg);
                    return;
                }
                JOptionPane.showMessageDialog(ProyectoDialog.this,
                        proyectoId == null ? "Proyecto creado exitosamente." : "Cambios guardados.");
                if (onSaved != null) onSaved.run();
                dispose();
            }
        }.execute();
    }
}

