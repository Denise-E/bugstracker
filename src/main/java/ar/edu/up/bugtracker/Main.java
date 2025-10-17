package ar.edu.up.bugtracker;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.dao.UserDao;
import ar.edu.up.bugtracker.service.UserService;
import ar.edu.up.bugtracker.ui.PanelManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // Asegurar zona horaria coherente con la DB
        System.setProperty("user.timezone", "UTC");

        // Boot de JPA/Hibernate
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("bugtrackerPU");
        EntityManager em = emf.createEntityManager();

        // Inicialización de capas
        UserDao usuarioDao = new UserDao(em);
        UserService usuarioService = new UserService(usuarioDao, em);
        UserController usuarioController = new UserController(usuarioService);

        // Levantar la UI en Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            PanelManager app = new PanelManager(usuarioController);
            app.setVisible(true);
        });

        // Hook para cerrar EM/EMF cuando el proceso termina
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { if (em.isOpen()) em.close(); } catch (Exception ignored) {}
            try { if (emf.isOpen()) emf.close(); } catch (Exception ignored) {}
        }));
    }
}
