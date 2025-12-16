package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
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
        
        // Botones
        List<String> buttonLabels = new ArrayList<>();
        List<java.util.function.IntConsumer> actions = new ArrayList<>();
        
        buttonLabels.add("Editar");
        actions.add(this::onEditRow);
        
        buttonLabels.add("Ver");
        actions.add(this::onViewRow);
        
        if (isAdmin) {
            buttonLabels.add("Eliminar");
            actions.add(this::onDeleteRow);
        }
        
        ActionButtonsRenderer renderer = new ActionButtonsRenderer(buttonLabels, FlowLayout.RIGHT);
        ActionButtonsEditor editor = new ActionButtonsEditor(buttonLabels, actions, FlowLayout.RIGHT);
        
        table.getColumnModel().getColumn(actionsCol).setCellRenderer(renderer);
        table.getColumnModel().getColumn(actionsCol).setCellEditor(editor);
        int accionesWidth = isAdmin ? 200 : 130;
        table.getColumnModel().getColumn(actionsCol).setPreferredWidth(accionesWidth);
        table.getColumnModel().getColumn(actionsCol).setResizable(false); 
        
        int fechaCol = 1; 
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
        SwingWorkerFactory.createWithAutoErrorHandling(
            this,
            () -> controller.getAll(),
            proyectos -> tableModel.setData(proyectos != null ? proyectos : new ArrayList<>())
        ).execute();
    }

    // Tabla
    private static class ProyectosTableModel extends AbstractTableModel {
        private final String[] cols = {"Nombre", "Creado", "Acciones"};
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
                    return (p.getCreadoEn() != null ? p.getCreadoEn().format(fmt) : "");
                case 2:
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

        SwingWorkerFactory.createVoidWithAutoErrorHandling(
            this,
            () -> controller.delete(proyecto.getId(), currentUser),
            () -> {
                JOptionPane.showMessageDialog(ProyectosListPanel.this, "Proyecto eliminado.");
                refresh();
            }
        ).execute();
    }
}

