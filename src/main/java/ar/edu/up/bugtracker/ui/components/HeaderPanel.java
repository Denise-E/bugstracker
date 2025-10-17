package ar.edu.up.bugtracker.ui.components;

import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {

    public HeaderPanel(Runnable onHome, Runnable onMiPerfil, Runnable onUsuarios, Runnable onLogout, boolean showUsuarios) {
        setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton btnHome = new JButton("Home");
        JButton btnMiPerfil = new JButton("Mi Perfil");
        JButton btnUsuarios = new JButton("Usuarios");
        JButton btnLogout = new JButton("Cerrar sesión");

        btnHome.addActionListener(e -> onHome.run());
        btnMiPerfil.addActionListener(e -> onMiPerfil.run());
        btnLogout.addActionListener(e -> onLogout.run());

        add(btnHome);
        add(btnMiPerfil);

        if (showUsuarios) {
            btnUsuarios.addActionListener(e -> onUsuarios.run());
            add(btnUsuarios);
        }

        add(Box.createHorizontalStrut(30));
        add(btnLogout);
    }
}
