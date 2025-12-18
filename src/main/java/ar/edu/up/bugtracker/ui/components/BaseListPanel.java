package ar.edu.up.bugtracker.ui.components;

import ar.edu.up.bugtracker.ui.components.tables.ActionButtonsEditor;
import ar.edu.up.bugtracker.ui.components.tables.ActionButtonsRenderer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.IntConsumer;

// Clase base abstracta para paneles que muestran listas en tablas.
public abstract class BaseListPanel<T> extends JPanel {
    protected JTable table;
    protected AbstractTableModel tableModel;
    protected final int actionsColumnIndex;

    /**
     * @param actionsColumnIndex Índice de la columna de acciones (normalmente el último)
     */
    public BaseListPanel(int actionsColumnIndex) {
        this.actionsColumnIndex = actionsColumnIndex;
        setLayout(new BorderLayout());
        buildUI();
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

    /**
     * Refresca los datos de la tabla.
     * Las subclases deben implementar este método para cargar datos desde el backend.
     */
    public abstract void refresh();
}
