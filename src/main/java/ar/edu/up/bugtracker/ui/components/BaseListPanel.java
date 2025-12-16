package ar.edu.up.bugtracker.ui.components;

import ar.edu.up.bugtracker.ui.components.tables.ActionButtonsEditor;
import ar.edu.up.bugtracker.ui.components.tables.ActionButtonsRenderer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

// Clase base abstracta para paneles que muestran listas en tablas.
public abstract class BaseListPanel<T> extends JPanel {
    protected JTable table;
    protected AbstractTableModel tableModel;
    protected final List<T> data;
    protected final String[] columnNames;
    protected final int actionsColumnIndex;

    /**
     * @param columnNames Nombres de las columnas (sin incluir "Acciones")
     * @param actionsColumnIndex Índice de la columna de acciones (normalmente el último)
     */
    public BaseListPanel(String[] columnNames, int actionsColumnIndex) {
        this.data = new ArrayList<>();
        this.columnNames = createColumnNamesWithActions(columnNames);
        this.actionsColumnIndex = actionsColumnIndex;
        setLayout(new BorderLayout());
        buildUI();
    }

    /**
     * Crea el array de nombres de las columnas.
     */
    private String[] createColumnNamesWithActions(String[] baseColumns) {
        String[] result = new String[baseColumns.length + 1];
        System.arraycopy(baseColumns, 0, result, 0, baseColumns.length);
        result[result.length - 1] = "Acciones";
        return result;
    }

    protected void buildUI() {
        tableModel = createTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        configureActionsColumn();
        configureOtherColumns();
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }


    protected void configureActionsColumn() {
        List<String> buttonLabels = getActionButtonLabels();
        List<IntConsumer> actions = getActionHandlers();
        
        // Crear renderer y editor reutilizables
        ActionButtonsRenderer renderer = new ActionButtonsRenderer(buttonLabels, getActionsAlignment());
        ActionButtonsEditor editor = new ActionButtonsEditor(buttonLabels, actions, getActionsAlignment());
        
        table.getColumnModel().getColumn(actionsColumnIndex).setCellRenderer(renderer);
        table.getColumnModel().getColumn(actionsColumnIndex).setCellEditor(editor);
        
        int actionsWidth = calculateActionsColumnWidth(buttonLabels.size());
        table.getColumnModel().getColumn(actionsColumnIndex).setPreferredWidth(actionsWidth);
        table.getColumnModel().getColumn(actionsColumnIndex).setResizable(false);
    }


    protected int calculateActionsColumnWidth(int buttonCount) {
        return buttonCount * 65 + 20;
    }


    protected void configureOtherColumns() {
    }

    protected abstract List<String> getActionButtonLabels();

    protected abstract List<IntConsumer> getActionHandlers();

    protected int getActionsAlignment() {
        return FlowLayout.CENTER;
    }

    protected abstract AbstractTableModel createTableModel();

    public void updateData(List<T> data) {
        this.data.clear();
        if (data != null) {
            this.data.addAll(data);
        }
        tableModel.fireTableDataChanged();
    }

    /**
     * Obtiene la entidad en la fila especificada.
     * 
     * @param row Índice de la fila
     * @return Entidad en esa fila, o null si el índice es inválido
     */
    protected T getEntityAt(int row) {
        if (row >= 0 && row < data.size()) {
            return data.get(row);
        }
        return null;
    }

    /**
     * Obtiene el índice de la fila seleccionada.
     * 
     * @return Índice de la fila seleccionada, o -1 si no hay selección
     */
    protected int getSelectedRow() {
        return table.getSelectedRow();
    }

    /**
     * Refresca los datos de la tabla.
     * Las subclases deben implementar este método para cargar datos desde el backend.
     */
    public abstract void refresh();
}
