package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class HomePanel extends JPanel {

    private ProyectosListPanel proyectosListPanel;
    private ProyectoDetailPanel proyectoDetailPanel;

    public HomePanel(ProyectoController proyectoController, UserLoggedInDto currentUser, Consumer<Long> onViewProyecto) {
        setLayout(new BorderLayout());
        proyectosListPanel = new ProyectosListPanel(proyectoController, currentUser, onViewProyecto);
        add(proyectosListPanel, BorderLayout.CENTER);
    }
}
