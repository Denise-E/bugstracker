package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import javax.swing.*;
import java.awt.*;

public class HomePanel extends JPanel {

    public HomePanel(ProyectoController proyectoController, UserLoggedInDto currentUser) {
        setLayout(new BorderLayout());
        ProyectosListPanel proyectosPanel = new ProyectosListPanel(proyectoController, currentUser);
        add(proyectosPanel, BorderLayout.CENTER);
    }
}
