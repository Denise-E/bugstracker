package ar.edu.up.bugtracker.ui.users;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class UsuariosListPanel extends JPanel {

    private final UserController controller;

    private final UsersTableModel tableModel = new UsersTableModel();
    private final JTable table = new JTable(tableModel);

    public UsuariosListPanel(UserController controller) {
        this.controller = controller;
        buildUI();
        refresh();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12,12,12,12)); // margen general

        // Título
        JLabel title = new JLabel("Listado de usuarios");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);

        // Tabla con margen (scroll pane con borde vacío adicional)
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(10,0,10,0));
        add(scroll, BorderLayout.CENTER);

        // Config tabla
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);

        // Columna de acciones (última)
        int actionsCol = tableModel.getColumnCount() - 1;
        table.getColumnModel().getColumn(actionsCol).setCellRenderer(new ActionsRenderer());
        table.getColumnModel().getColumn(actionsCol).setCellEditor(new ActionsEditor());

        // Ancho sugerido para acciones
        table.getColumnModel().getColumn(actionsCol).setPreferredWidth(160);
    }

    private void refresh() {
        new SwingWorker<List<UserDetailDto>, Void>() {
            private Exception error;
            @Override protected List<UserDetailDto> doInBackground() {
                try { return controller.getAll(); }
                catch (Exception ex) { this.error = ex; return null; }
            }
            @Override protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(UsuariosListPanel.this, "Error al cargar usuarios.");
                    return;
                }
                try {
                    List<UserDetailDto> list = get();
                    tableModel.setData(list != null ? list : new ArrayList<UserDetailDto>());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsuariosListPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    // ===== Table Model (sin ID visible) =====
    private static class UsersTableModel extends AbstractTableModel {
        private final String[] cols = {"Nombre","Apellido","Email","Perfil","Creado","Acciones"};
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        private List<UserDetailDto> data = new ArrayList<UserDetailDto>();

        public void setData(List<UserDetailDto> d) {
            this.data = d;
            fireTableDataChanged();
        }

        public UserDetailDto getAt(int row) {
            if (row < 0 || row >= data.size()) return null;
            return data.get(row);
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int col) { return cols[col]; }

        @Override public Object getValueAt(int row, int col) {
            UserDetailDto u = data.get(row);
            switch (col) {
                case 0: return u.getNombre();
                case 1: return u.getApellido();
                case 2: return u.getEmail();
                case 3: return u.getPerfil();
                case 4: return (u.getCreadoEn() != null ? u.getCreadoEn().format(fmt) : "");
                case 5: return "ACCIONES"; // placeholder; renderizado con botones
                default: return "";
            }
        }

        @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == (getColumnCount() - 1); // solo columna Acciones
        }

        @Override public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }

    // ===== Renderer de botones =====
    private class ActionsRenderer extends JPanel implements TableCellRenderer {
        private final JButton btnEdit = new JButton("Editar");
        private final JButton btnDelete = new JButton("Eliminar");

        public ActionsRenderer() {
            setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 2));
            add(btnEdit);
            add(btnDelete);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    // ===== Editor de botones (maneja eventos por fila) =====
    private class ActionsEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        private final JButton btnEdit = new JButton("Editar");
        private final JButton btnDelete = new JButton("Eliminar");
        private int editingRow = -1;

        public ActionsEditor() {
            panel.add(btnEdit);
            panel.add(btnDelete);

            btnEdit.addActionListener(e -> {
                int row = editingRow;
                stopCellEditing();
                onEditRow(row);
            });
            btnDelete.addActionListener(e -> {
                int row = editingRow;
                stopCellEditing();
                onDeleteRow(row);
            });
        }

        @Override
        public boolean isCellEditable(EventObject e) { return true; }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            editingRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() { return null; }
    }

    // ===== Acciones por fila =====
    private void onEditRow(int row) {
        UserDetailDto dto = tableModel.getAt(row);
        if (dto == null) return;

        UpdateUsuarioDialog dlg = new UpdateUsuarioDialog(
                SwingUtilities.getWindowAncestor(this),
                controller,
                dto.getId(),     // usamos ID interno sin mostrarlo
                this::refresh
        );
        dlg.setVisible(true);
    }

    private void onDeleteRow(int row) {
        UserDetailDto dto = tableModel.getAt(row);
        if (dto == null) return;

        int opt = JOptionPane.showConfirmDialog(
                this, "¿Eliminar usuario seleccionado?", "Confirmar",
                JOptionPane.YES_NO_OPTION
        );
        if (opt != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            private Exception error;
            @Override protected Void doInBackground() {
                try { controller.delete(dto.getId()); return null; }
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
