package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.ComentarioController;
import ar.edu.up.bugtracker.controller.IncidenciaController;
import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.ForbiddenException;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;

public class ProyectoDetailPanel extends JPanel {

    private final ProyectoController proyectoController;
    private final IncidenciaController incidenciaController;
    private final ComentarioController comentarioController;
    private final UserController userController;
    private final UserLoggedInDto currentUser;
    private final Long proyectoId;
    private final boolean isAdmin;
    private final Consumer<Long> onViewIncidencia;

    private final IncidenciasTableModel tableModel = new IncidenciasTableModel();
    private final JTable table = new JTable(tableModel);
    private Proyecto proyecto;
    private JLabel lblNombre;
    private JLabel lblDescripcion;

    public ProyectoDetailPanel(ProyectoController proyectoController,
                               IncidenciaController incidenciaController,
                               ComentarioController comentarioController,
                               UserController userController,
                               UserLoggedInDto currentUser,
                               Long proyectoId,
                               Consumer<Long> onViewIncidencia) {
        this.proyectoController = proyectoController;
        this.incidenciaController = incidenciaController;
        this.comentarioController = comentarioController;
        this.userController = userController;
        this.currentUser = currentUser;
        this.proyectoId = proyectoId;
        this.isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getPerfil());
        this.onViewIncidencia = onViewIncidencia;
        buildUI();
        loadProyecto();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Panel superior
        JPanel topPanel = new JPanel(new BorderLayout());
        JButton btnVolver = new JButton("← Volver");
        btnVolver.addActionListener(e -> {
            // Volver al home usando PanelManager
            Window window = SwingUtilities.getWindowAncestor(ProyectoDetailPanel.this);
            if (window instanceof ar.edu.up.bugtracker.ui.PanelManager) {
                ((ar.edu.up.bugtracker.ui.PanelManager) window).showHome();
            }
        });
        topPanel.add(btnVolver, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        
        JPanel proyectoInfoPanel = new JPanel(new BorderLayout());
        proyectoInfoPanel.setBorder(new EmptyBorder(10, 0, 15, 0));
        
        JPanel nombreDescPanel = new JPanel(new BorderLayout());
        nombreDescPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        lblNombre = new JLabel("<html>Cargando proyecto...</html>");
        lblNombre.setFont(lblNombre.getFont().deriveFont(Font.BOLD, 18f));
        lblNombre.setVerticalAlignment(SwingConstants.CENTER);
        nombreDescPanel.add(lblNombre, BorderLayout.NORTH);
        
        lblDescripcion = new JLabel("<html></html>");
        lblDescripcion.setFont(lblDescripcion.getFont().deriveFont(14f));
        lblDescripcion.setVerticalAlignment(SwingConstants.TOP);
        nombreDescPanel.add(lblDescripcion, BorderLayout.CENTER);
        
        proyectoInfoPanel.add(nombreDescPanel, BorderLayout.CENTER);
        
        JPanel botonesPanel = new JPanel();
        botonesPanel.setLayout(new BoxLayout(botonesPanel, BoxLayout.Y_AXIS));
        
        JButton btnMetricas = new JButton("Métricas");
        btnMetricas.setPreferredSize(new Dimension(120, 30));
        btnMetricas.setMaximumSize(new Dimension(120, 30));
        btnMetricas.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMetricas.addActionListener(e -> onMetricasClick());
        botonesPanel.add(btnMetricas);
        
        botonesPanel.add(Box.createVerticalStrut(5));
        
        JButton btnCrearIncidencia = new JButton("+ Incidencia");
        btnCrearIncidencia.setPreferredSize(new Dimension(120, 30));
        btnCrearIncidencia.setMaximumSize(new Dimension(120, 30));
        btnCrearIncidencia.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCrearIncidencia.addActionListener(e -> onCreateIncidenciaClick());
        botonesPanel.add(btnCrearIncidencia);
        
        proyectoInfoPanel.add(botonesPanel, BorderLayout.EAST);
        
        centerPanel.add(proyectoInfoPanel, BorderLayout.NORTH);

        // Tabla de incidencias
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(10, 0, 10, 0));
        centerPanel.add(scroll, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        
        // Configurar columna de descripción para permitir múltiples líneas
        table.getColumnModel().getColumn(0).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JTextArea textArea = new JTextArea();
                textArea.setText(value != null ? value.toString() : "");
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                textArea.setOpaque(true);
                textArea.setBorder(new EmptyBorder(4, 4, 4, 4));
                
                if (isSelected) {
                    textArea.setBackground(table.getSelectionBackground());
                    textArea.setForeground(table.getSelectionForeground());
                } else {
                    textArea.setBackground(table.getBackground());
                    textArea.setForeground(table.getForeground());
                }
                
                int height = textArea.getPreferredSize().height;
                if (height > table.getRowHeight(row)) {
                    table.setRowHeight(row, Math.min(height + 8, 200)); // Máximo 200px
                }
                
                return textArea;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(500);

        int actionsCol = tableModel.getColumnCount() - 1;
        table.getColumnModel().getColumn(actionsCol).setCellRenderer(new ActionsRenderer());
        table.getColumnModel().getColumn(actionsCol).setCellEditor(new ActionsEditor());
        table.getColumnModel().getColumn(actionsCol).setPreferredWidth(120);
    }

    private void loadProyecto() {
        new SwingWorker<Proyecto, Void>() {
            private Exception error;

            @Override
            protected Proyecto doInBackground() {
                try {
                    return proyectoController.getById(proyectoId);
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    String msg = (error instanceof NotFoundException)
                            ? "Proyecto no encontrado."
                            : "Error al cargar proyecto: " + error.getMessage();
                    JOptionPane.showMessageDialog(ProyectoDetailPanel.this, msg);
                    return;
                }
                try {
                    proyecto = get();
                    if (proyecto != null) {
                        updateTitle();
                        loadIncidencias();
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProyectoDetailPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    private void updateTitle() {
        if (proyecto != null) {
            String nombre = proyecto.getNombre() != null ? proyecto.getNombre() : "";
            nombre = nombre.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            lblNombre.setText("<html><div style='width: 600px;'>" + nombre + "</div></html>");
            
            String desc = proyecto.getDescripcion();
            if (desc != null && !desc.trim().isEmpty()) {                desc = desc.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                lblDescripcion.setText("<html><div style='width: 600px;'>" + desc + "</div></html>");
            } else {
                lblDescripcion.setText("<html></html>");
            }
        }
    }
    
    private void onMetricasClick() {
        if (proyecto == null) return;
        
        Window window = SwingUtilities.getWindowAncestor(ProyectoDetailPanel.this);
        if (window instanceof ar.edu.up.bugtracker.ui.PanelManager) {
            ((ar.edu.up.bugtracker.ui.PanelManager) window).showProyectoMetricas(proyectoId);
        }
    }
    
    private void onCreateIncidenciaClick() {
        if (proyecto == null) return;
        
        IncidenciaDialog dlg = new IncidenciaDialog(
                SwingUtilities.getWindowAncestor(this),
                incidenciaController,
                userController,
                currentUser,
                proyectoId,
                this::loadIncidencias
        );
        dlg.setVisible(true);
    }

    private void loadIncidencias() {
        new SwingWorker<List<Incidencia>, Void>() {
            private Exception error;

            @Override
            protected List<Incidencia> doInBackground() {
                try {
                    return incidenciaController.findByProyecto(proyectoId);
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(ProyectoDetailPanel.this,
                            "Error al cargar incidencias: " + error.getMessage());
                    return;
                }
                try {
                    List<Incidencia> incidencias = get();
                    tableModel.setData(incidencias != null ? incidencias : new ArrayList<>());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProyectoDetailPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    // Tabla de incidencias
    private static class IncidenciasTableModel extends AbstractTableModel {
        private final String[] cols = {"Descripción", "Acciones"};
        private List<Incidencia> data = new ArrayList<>();

        public void setData(List<Incidencia> d) {
            this.data = d != null ? d : new ArrayList<>();
            fireTableDataChanged();
        }

        public Incidencia getAt(int row) {
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
            Incidencia i = data.get(row);
            if (i == null) return "";
            switch (col) {
                case 0:
                    return i.getDescripcion() != null ? i.getDescripcion() : "";
                case 1:
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
        private final JButton btnView = new JButton("Ver");
        private final JButton btnDelete = new JButton("Eliminar");

        public ActionsRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 4, 2));
            btnView.setToolTipText("Ver detalle");
            btnView.setFont(btnView.getFont().deriveFont(12f));
            btnView.setMargin(new Insets(2, 8, 2, 8));
            add(btnView);
            
            if (isAdmin) {
                btnDelete.setToolTipText("Eliminar");
                btnDelete.setFont(btnDelete.getFont().deriveFont(12f));
                btnDelete.setMargin(new Insets(2, 8, 2, 8));
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
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        private final JButton btnView = new JButton("Ver");
        private final JButton btnDelete = new JButton("Eliminar");
        private int editingRow = -1;

        public ActionsEditor() {
            btnView.setToolTipText("Ver detalle");
            btnView.setFont(btnView.getFont().deriveFont(12f));
            btnView.setMargin(new Insets(2, 8, 2, 8));
            panel.add(btnView);
            
            if (isAdmin) {
                btnDelete.setToolTipText("Eliminar");
                btnDelete.setFont(btnDelete.getFont().deriveFont(12f));
                btnDelete.setMargin(new Insets(2, 8, 2, 8));
                panel.add(btnDelete);
            }

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

    private void onViewRow(int row) {
        Incidencia incidencia = tableModel.getAt(row);
        if (incidencia == null) return;

        if (onViewIncidencia != null) {
            onViewIncidencia.accept(incidencia.getId());
        }
    }

    private void onDeleteRow(int row) {
        Incidencia incidencia = tableModel.getAt(row);
        if (incidencia == null) return;

        int opt = JOptionPane.showOptionDialog(
                this,
                "¿Estás seguro que deseas eliminar la incidencia? Esta acción no puede deshacerse.",
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
                System.out.println("[ProyectoDetailPanel] Iniciando eliminación de incidencia con ID: " + incidencia.getId());
                try {
                    incidenciaController.delete(incidencia.getId());
                    System.out.println("[ProyectoDetailPanel] Incidencia eliminada exitosamente");
                    return null;
                } catch (Exception ex) {
                    System.out.println("[ProyectoDetailPanel] ERROR durante la eliminación:");
                    System.out.println("[ProyectoDetailPanel] Tipo: " + ex.getClass().getName());
                    System.out.println("[ProyectoDetailPanel] Mensaje: " + ex.getMessage());
                    System.out.println("[ProyectoDetailPanel] Stack trace:");
                    ex.printStackTrace();
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    String msg;
                    if (error instanceof NotFoundException) {
                        msg = "Incidencia no encontrada.";
                    } else {
                        msg = "Error al eliminar: " + error.getMessage();
                    }
                    JOptionPane.showMessageDialog(ProyectoDetailPanel.this, msg);
                    return;
                }
                JOptionPane.showMessageDialog(ProyectoDetailPanel.this, "Incidencia eliminada.");
                loadIncidencias();
            }
        }.execute();
    }
}

