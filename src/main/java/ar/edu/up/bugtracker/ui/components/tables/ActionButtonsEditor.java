package ar.edu.up.bugtracker.ui.components.tables;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.function.IntConsumer;

// Editor reutilizable para botones de acción en tablas.
public class ActionButtonsEditor extends AbstractCellEditor implements TableCellEditor {
    private final JPanel panel;
    private final List<JButton> buttons;
    private final List<IntConsumer> actions;
    private int editingRow = -1;
    private int alignment;

    /**
     * Crea un editor de botones de acción.
     * 
     * @param buttonLabels Etiquetas de los botones a mostrar
     * @param actions Lista de acciones a ejecutar cuando se hace clic en cada botón.
     *                Cada acción recibe el número de fila como parámetro.
     * @param alignment Alineación del layout (FlowLayout.LEFT, FlowLayout.CENTER, FlowLayout.RIGHT)
     */
    public ActionButtonsEditor(List<String> buttonLabels, List<IntConsumer> actions, int alignment) {
        this.actions = new ArrayList<>(actions);
        this.alignment = alignment;
        this.panel = new JPanel(new FlowLayout(alignment, 6, 2));
        this.buttons = new ArrayList<>();

        for (int i = 0; i < buttonLabels.size(); i++) {
            final int buttonIndex = i;
            String label = buttonLabels.get(i);
            JButton btn = new JButton(label);
            btn.setFont(btn.getFont().deriveFont(12f));
            btn.setMargin(new Insets(2, 8, 2, 8));
            
            btn.addActionListener(e -> {
                int row = editingRow;
                stopCellEditing();
                if (buttonIndex < this.actions.size() && row >= 0) {
                    this.actions.get(buttonIndex).accept(row);
                }
            });
            
            buttons.add(btn);
            panel.add(btn);
        }
    }

    public ActionButtonsEditor(List<String> buttonLabels, List<IntConsumer> actions) {
        this(buttonLabels, actions, FlowLayout.RIGHT);
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        editingRow = row;
        panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    // Permite filtrar qué botones mostrar según condiciones (roles).
    public void setButtonVisibility(List<Boolean> visibility) {
        panel.removeAll();
        for (int i = 0; i < buttons.size(); i++) {
            if (i < visibility.size() && visibility.get(i)) {
                panel.add(buttons.get(i));
            }
        }
        panel.revalidate();
        panel.repaint();
    }
}
