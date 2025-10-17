package ar.edu.up.bugtracker.ui.users;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;
import ar.edu.up.bugtracker.ui.PanelManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UsuariosListPanel extends JPanel {

    private final PanelManager manager;
    private final UserController controller;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID","Nombre","Apellido","Email","Perfil","Creado"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public UsuariosListPanel(PanelManager manager, UserController controller) {
        this.manager = manager;
        this.controller = controller;
        buildUI();
        refresh();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefresh = new JButton("Refrescar");
        JButton btnUpdate = new JButton("Actualizar");
        JButton btnDelete = new JButton("Eliminar");
        actions.add(btnRefresh);
        actions.add(btnUpdate);
        actions.add(btnDelete);
        add(actions, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refresh());
        btnUpdate.addActionListener(e -> openUpdateDialog());
        btnDelete.addActionListener(e -> doDelete());
    }

    private void refresh() {
        new SwingWorker<List<UserDetailDto>, Void>() {
            private Exception error;
            @Override protected List<UserDetailDto> doInBackground() {
                try { return controller.getAll(); }
                catch (Exception ex) { this.error = ex; return null; }
            }
            @Override protected void done() {
                model.setRowCount(0);
                if (error != null) {
                    JOptionPane.showMessageDialog(UsuariosListPanel.this, "Error al cargar usuarios.");
                    return;
                }
                try {
                    List<UserDetailDto> list = get();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    for (UserDetailDto u : list) {
                        model.addRow(new Object[]{
                                u.getId(), u.getNombre(), u.getApellido(),
                                u.getEmail(), u.getPerfil(),
                                (u.getCreadoEn()!=null? u.getCreadoEn().format(fmt): "")
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsuariosListPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    private Long getSelectedUserId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return (Long) model.getValueAt(row, 0);
    }

    private void openUpdateDialog() {
        Long id = getSelectedUserId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un usuario.");
            return;
        }
        // Solo puede editar email y rol
        UpdateUsuarioDialog dlg = new UpdateUsuarioDialog(SwingUtilities.getWindowAncestor(this), controller, id, () -> refresh());
        dlg.setVisible(true);
    }

    private void doDelete() {
        Long id = getSelectedUserId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un usuario.");
            return;
        }
        int opt = JOptionPane.showConfirmDialog(this, "¿Eliminar usuario " + id + "?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            private Exception error;
            @Override protected Void doInBackground() {
                try { controller.delete(id); return null; }
                catch (Exception ex) { this.error = ex; return null; }
            }
            @Override protected void done() {
                if (error != null) {
                    String msg = (error instanceof NotFoundException) ? "Usuario no encontrado." : "Error al eliminar.";
                    JOptionPane.showMessageDialog(UsuariosListPanel.this, msg);
                    return;
                }
                JOptionPane.showMessageDialog(UsuariosListPanel.this, "Usuario eliminado.");
                refresh();
            }
        }.execute();
    }
}
