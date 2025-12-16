package ar.edu.up.bugtracker.ui.components.tables;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Renderer reutilizable para botones de acción en tablas.
public class ActionButtonsRenderer extends JPanel implements TableCellRenderer {
    private final List<JButton> buttons;
    private final FlowLayout layout;

    public ActionButtonsRenderer(List<String> buttonLabels) {
        this.layout = new FlowLayout(FlowLayout.RIGHT, 6, 2);
        setLayout(layout);
        buttons = new ArrayList<>();
        
        for (String label : buttonLabels) {
            JButton btn = new JButton(label);
            btn.setFont(btn.getFont().deriveFont(12f));
            btn.setMargin(new Insets(2, 8, 2, 8));
            buttons.add(btn);
            add(btn);
        }
        
        setOpaque(true);
    }

    public ActionButtonsRenderer(List<String> buttonLabels, int alignment) {
        this.layout = new FlowLayout(alignment, 6, 2);
        setLayout(layout);
        buttons = new ArrayList<>();
        
        for (String label : buttonLabels) {
            JButton btn = new JButton(label);
            btn.setFont(btn.getFont().deriveFont(12f));
            btn.setMargin(new Insets(2, 8, 2, 8));
            buttons.add(btn);
            add(btn);
        }
        
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return this;
    }
    
    /**
     * Permite filtrar qué botones mostrar según condiciones (ej: permisos de admin).
     * 
     * @param visibility Lista de booleanos indicando qué botones mostrar
     */
    public void setButtonVisibility(List<Boolean> visibility) {
        removeAll();
        for (int i = 0; i < buttons.size(); i++) {
            if (i < visibility.size() && visibility.get(i)) {
                add(buttons.get(i));
            }
        }
        revalidate();
        repaint();
    }

    public List<JButton> getButtons() {
        return new ArrayList<>(buttons);
    }
}
