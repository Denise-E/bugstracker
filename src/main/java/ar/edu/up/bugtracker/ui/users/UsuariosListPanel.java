package ar.edu.up.bugtracker.ui.users;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.controller.UserRoleController;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;
import ar.edu.up.bugtracker.ui.components.SwingWorkerFactory;
import ar.edu.up.bugtracker.ui.components.tables.ActionButtonsEditor;
import ar.edu.up.bugtracker.ui.components.tables.ActionButtonsRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UsuariosListPanel extends JPanel {

    private final UserController controller;
    private final UserRoleController userController; // controlador de roles
    private final Long currentUserId;

    private final UsersTableModel tableModel = new UsersTableModel();
    private final JTable table = new JTable(tableModel);

    public UsuariosListPanel(UserController controller, Long currentUserId, UserRoleController userController) {
        this.controller = controller;
        this.currentUserId = currentUserId;
        this.userController = userController;
        buildUI();
        refresh();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12,12,12,12)); // margen general

        JLabel title = new JLabel("Listado de usuarios");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(10,0,10,0));
        add(scroll, BorderLayout.CENTER);

        table.setRowHeight(28);
        table.setFillsViewportHeight(true);

        int actionsCol = tableModel.getColumnCount() - 1;
        
        // Botones
        List<String> buttonLabels = Arrays.asList("Editar", "Eliminar");
        List<java.util.function.IntConsumer> actions = Arrays.asList(
            this::onEditRow,
            this::onDeleteRow
        );
        
        ActionButtonsRenderer renderer = new ActionButtonsRenderer(buttonLabels, FlowLayout.RIGHT);
        ActionButtonsEditor editor = new ActionButtonsEditor(buttonLabels, actions, FlowLayout.RIGHT);
        
        table.getColumnModel().getColumn(actionsCol).setCellRenderer(renderer);
        table.getColumnModel().getColumn(actionsCol).setCellEditor(editor);
        table.getColumnModel().getColumn(actionsCol).setPreferredWidth(160);
    }

    private void refresh() {
        SwingWorkerFactory.createWithAutoErrorHandling(
            this,
            () -> {
                List<UserDetailDto> all = controller.getAll();
                List<UserDetailDto> filtered = new ArrayList<>();
                if (all != null) {
                    for (UserDetailDto u : all) {
                        if (currentUserId != null && u.getId() != null && u.getId().equals(currentUserId)) {
                            continue; // ocultar usuario logueado
                        }
                        filtered.add(u);
                    }
                }
                return filtered;
            },
            filtered -> tableModel.setData(filtered)
        ).execute();
    }

    // Tabla
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
                case 3: return u.getPerfil(); // nombre del rol a mostrar
                case 4: return (u.getCreadoEn() != null ? u.getCreadoEn().format(fmt) : "");
                case 5: return "ACCIONES";
                default: return "";
            }
        }

        @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == (getColumnCount() - 1);
        }

        @Override public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }


    // Acciones de las filas
    private void onEditRow(int row) {
        UserDetailDto dto = tableModel.getAt(row);
        if (dto == null) return;

        // Pasamos el usuario de la fila y el rol de la fila (por nombre).
        // Si más adelante tu DTO trae perfilId, podés pasar ese Long en lugar del nombre.
        Long rowUserId = dto.getId();
        Long initialPerfilId = null;              // si lo tenés en DTO, reemplazar acá
        String initialPerfilName = dto.getPerfil(); // nombre del rol mostrado en la tabla

        UpdateUsuarioDialog dlg = new UpdateUsuarioDialog(
                SwingUtilities.getWindowAncestor(this),
                controller,
                userController,
                rowUserId,
                initialPerfilId,
                initialPerfilName,
                this::refresh
        );
        dlg.setVisible(true);
    }

    private void onDeleteRow(int row) {
        UserDetailDto dto = tableModel.getAt(row);
        if (dto == null) return;

        int opt = JOptionPane.showOptionDialog(
                this,
                "¿Estás seguro que deseas borrar al usuario? Esta acción no puede deshacerse.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{"Sí", "No"},
                "No"                         // opción por defecto
        );
        if (opt != JOptionPane.YES_OPTION) return;

        SwingWorkerFactory.createVoidWithAutoErrorHandling(
            this,
            () -> controller.delete(dto.getId()),
            () -> {
                JOptionPane.showMessageDialog(UsuariosListPanel.this, "Usuario eliminado.");
                refresh();
            }
        ).execute();
    }
}
