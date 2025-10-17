package ar.edu.up.bugtracker.ui.projects;

import javax.swing.*;
import java.awt.*;

public class HomePanel extends JPanel {
    public HomePanel() {
        setLayout(new GridBagLayout());
        JLabel lbl = new JLabel("No hay proyectos creados por el momento");
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 16f));
        add(lbl);
    }
}
