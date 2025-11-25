package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.exceptions.ForbiddenException;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

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
import java.util.function.Consumer;

public class ProyectosListPanel extends JPanel {

    private final ProyectoController controller;
    private final UserLoggedInDto currentUser;
    private final boolean isAdmin;
    private Consumer<Long> onViewProyecto;

    private final ProyectosTableModel tableModel = new ProyectosTableModel();
    private final JTable table = new JTable(tableModel);

    public ProyectosListPanel(ProyectoController controller, UserLoggedInDto currentUser) {
        this(controller, currentUser, null);
    }

    public ProyectosListPanel(ProyectoController controller, UserLoggedInDto currentUser, Consumer<Long> onViewProyecto) {
        this.controller = controller;
        this.currentUser = currentUser;
        this.isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getPerfil());
        this.onViewProyecto = onViewProyecto;
        buildUI();
        refresh();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Panel superior con título y botón crear
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Listado de proyectos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        topPanel.add(title, BorderLayout.WEST);

        if (isAdmin) {
            JButton btnCrear = new JButton("Crear proyecto");
            btnCrear.addActionListener(e -> onCreateClick());
            topPanel.add(btnCrear, BorderLayout.EAST);
        }

        add(topPanel, BorderLayout.NORTH);

        // Tabla
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(10, 0, 10, 0));
        add(scroll, BorderLayout.CENTER);

        table.setRowHeight(28);
        table.setFillsViewportHeight(true);

        int actionsCol = tableModel.getColumnCount() - 1;
        table.getColumnModel().getColumn(actionsCol).setCellRenderer(new ActionsRenderer());
        table.getColumnModel().getColumn(actionsCol).setCellEditor(new ActionsEditor());
        table.getColumnModel().getColumn(actionsCol).setPreferredWidth(160);
        
        int fechaCol = 2; 
        table.getColumnModel().getColumn(fechaCol).setPreferredWidth(140);
        table.getColumnModel().getColumn(fechaCol).setMinWidth(120);
        table.getColumnModel().getColumn(fechaCol).setMaxWidth(160);
    }

    private void onCreateClick() {
        ProyectoDialog dlg = new ProyectoDialog(
                SwingUtilities.getWindowAncestor(this),
                controller,
                currentUser,
                null,
                this::refresh
        );
        dlg.setVisible(true);
    }

    private void refresh() {
        new SwingWorker<List<Proyecto>, Void>() {
            private Exception error;

            @Override
            protected List<Proyecto> doInBackground() {
                try {
                    return controller.getAll();
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(ProyectosListPanel.this,
                            "Error al cargar proyectos: " + error.getMessage());
                    return;
                }
                try {
                    List<Proyecto> proyectos = get();
                    tableModel.setData(proyectos != null ? proyectos : new ArrayList<>());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProyectosListPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    // Tabla
    private static class ProyectosTableModel extends AbstractTableModel {
        private final String[] cols = {"Nombre", "Descripción", "Creado", "Acciones"};
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        private List<Proyecto> data = new ArrayList<>();

        public void setData(List<Proyecto> d) {
            this.data = d != null ? d : new ArrayList<>();
            fireTableDataChanged();
        }

        public Proyecto getAt(int row) {
            if (row < 0 || row >= data.size()) return null;
            return data.get(row);
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int col) {
            return cols[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            Proyecto p = data.get(row);
            if (p == null) return "";
            switch (col) {
                case 0:
                    return p.getNombre();
                case 1:
                    String desc = p.getDescripcion();
                    if (desc != null && desc.length() > 50) {
                        return desc.substring(0, 47) + "...";
                    }
                    return desc != null ? desc : "";
                case 2:
                    return (p.getCreadoEn() != null ? p.getCreadoEn().format(fmt) : "");
                case 3:
                    return "ACCIONES";
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == (getColumnCount() - 1);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }

    private class ActionsRenderer extends JPanel implements TableCellRenderer {
        private final JButton btnEdit = new JButton("Editar");
        private final JButton btnView = new JButton("Ver");
        private final JButton btnDelete = new JButton("Eliminar");

        public ActionsRenderer() {
            setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 2));
            add(btnEdit);
            add(btnView);
            if (isAdmin) {
                add(btnDelete);
            }
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    private class ActionsEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        private final JButton btnEdit = new JButton("Editar");
        private final JButton btnView = new JButton("Ver");
        private final JButton btnDelete = new JButton("Eliminar");
        private int editingRow = -1;

        public ActionsEditor() {
            panel.add(btnEdit);
            panel.add(btnView);
            if (isAdmin) {
                panel.add(btnDelete);
            }

            btnEdit.addActionListener(e -> {
                int row = editingRow;
                stopCellEditing();
                onEditRow(row);
            });

            btnView.addActionListener(e -> {
                int row = editingRow;
                stopCellEditing();
                onViewRow(row);
            });

            btnDelete.addActionListener(e -> {
                int row = editingRow;
                stopCellEditing();
                onDeleteRow(row);
            });
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                      int row, int column) {
            editingRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }

    // Acciones de cada fila
    private void onEditRow(int row) {
        Proyecto proyecto = tableModel.getAt(row);
        if (proyecto == null) return;

        ProyectoDialog dlg = new ProyectoDialog(
                SwingUtilities.getWindowAncestor(this),
                controller,
                currentUser,
                proyecto.getId(),
                this::refresh
        );
        dlg.setVisible(true);
    }

    private void onViewRow(int row) {
        Proyecto proyecto = tableModel.getAt(row);
        if (proyecto == null) return;
        
        if (onViewProyecto != null) {
            onViewProyecto.accept(proyecto.getId());
        }
    }

    private void onDeleteRow(int row) {
        Proyecto proyecto = tableModel.getAt(row);
        if (proyecto == null) return;

        int opt = JOptionPane.showOptionDialog(
                this,
                "¿Estás seguro que deseas eliminar el proyecto \"" + proyecto.getNombre() + "\"?\n" +
                        "Se eliminarán todas las incidencias asociadas. Esta acción no puede deshacerse.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{"Sí", "No"},
                "No"
        );
        if (opt != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    controller.delete(proyecto.getId(), currentUser);
                    return null;
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    String msg;
                    if (error instanceof ForbiddenException) {
                        msg = "No tenés permisos para eliminar proyectos.";
                    } else if (error instanceof NotFoundException) {
                        msg = "Proyecto no encontrado.";
                    } else {
                        msg = "Error al eliminar: " + error.getMessage();
                    }
                    JOptionPane.showMessageDialog(ProyectosListPanel.this, msg);
                    return;
                }
                JOptionPane.showMessageDialog(ProyectosListPanel.this, "Proyecto eliminado.");
                refresh();
            }
        }.execute();
    }
}

