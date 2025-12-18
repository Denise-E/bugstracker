package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import ar.edu.up.bugtracker.ui.components.BaseListPanel;
import ar.edu.up.bugtracker.ui.components.SwingWorkerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class ProyectosListPanel extends BaseListPanel<Proyecto> {

    private final ProyectoController controller;
    private final UserLoggedInDto currentUser;
    private final boolean isAdmin;
    private Consumer<Long> onViewProyecto;

    public ProyectosListPanel(ProyectoController controller, UserLoggedInDto currentUser) {
        this(controller, currentUser, null);
    }

    public ProyectosListPanel(ProyectoController controller, UserLoggedInDto currentUser, Consumer<Long> onViewProyecto) {
        super(2);
        this.controller = controller;
        this.currentUser = currentUser;
        this.isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getPerfil());
        this.onViewProyecto = onViewProyecto;
        configureActionsColumn();
        buildTopPanel();
        refresh();
    }

    private void buildTopPanel() {
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Listado de proyectos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        topPanel.add(title, BorderLayout.WEST);

        if (isAdmin) {
            JButton btnCrear = new JButton("Crear proyecto");
            btnCrear.addActionListener(e -> onCreateClick());
            topPanel.add(btnCrear, BorderLayout.EAST);
        }

        // Mover el scrollPane que BaseListPanel agregó al CENTER y agregar el topPanel al NORTH
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JScrollPane) {
                remove(comp);
                add(topPanel, BorderLayout.NORTH);
                add(comp, BorderLayout.CENTER);
                break;
            }
        }
    }

    @Override
    protected void configureOtherColumns() {
        int fechaCol = 1; 
        table.getColumnModel().getColumn(fechaCol).setPreferredWidth(140);
        table.getColumnModel().getColumn(fechaCol).setMinWidth(120);
        table.getColumnModel().getColumn(fechaCol).setMaxWidth(160);
    }

    @Override
    protected int calculateActionsColumnWidth(int buttonCount) {
        return isAdmin ? 200 : 130;
    }

    @Override
    protected List<String> getActionButtonLabels() {
        List<String> labels = new ArrayList<>();
        labels.add("Editar");
        labels.add("Ver");
        if (isAdmin) {
            labels.add("Eliminar");
        }
        return labels;
    }

    @Override
    protected List<IntConsumer> getActionHandlers() {
        List<IntConsumer> handlers = new ArrayList<>();
        handlers.add(this::onEditRow);
        handlers.add(this::onViewRow);
        if (isAdmin) {
            handlers.add(this::onDeleteRow);
        }
        return handlers;
    }

    @Override
    protected AbstractTableModel createTableModel() {
        return new ProyectosTableModel();
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

    @Override
    public void refresh() {
        SwingWorkerFactory.createWithAutoErrorHandling(
            this,
            () -> controller.getAll(),
            proyectos -> {
                ProyectosTableModel model = (ProyectosTableModel) tableModel;
                model.setData(proyectos != null ? proyectos : new ArrayList<>());
            }
        ).execute();
    }

    // Tabla
    private class ProyectosTableModel extends AbstractTableModel {
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
        ProyectosTableModel model = (ProyectosTableModel) tableModel;
        Proyecto proyecto = model.getAt(row);
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
        ProyectosTableModel model = (ProyectosTableModel) tableModel;
        Proyecto proyecto = model.getAt(row);
        if (proyecto == null) return;
        
        if (onViewProyecto != null) {
            onViewProyecto.accept(proyecto.getId());
        }
    }

    private void onDeleteRow(int row) {
        ProyectosTableModel model = (ProyectosTableModel) tableModel;
        Proyecto proyecto = model.getAt(row);
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

