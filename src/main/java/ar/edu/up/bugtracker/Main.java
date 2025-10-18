package ar.edu.up.bugtracker;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.dao.UserDao;
import ar.edu.up.bugtracker.service.UserService;
import ar.edu.up.bugtracker.ui.PanelManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException {
        // Asegurar zona horaria coherente con la DB
        System.setProperty("user.timezone", "UTC");

        Properties p = new Properties();

        try (var in = Files.newInputStream(Paths.get("config/local.properties"))) { p.load(in); }
        Map<String,Object> props = Map.of(
                "jakarta.persistence.jdbc.url",  p.getProperty("db.url"),
                "jakarta.persistence.jdbc.user", p.getProperty("db.user"),
                "jakarta.persistence.jdbc.password", p.getProperty("db.pass")
        );

        // Boot de JPA/Hibernate
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("bugtrackerPU", props);
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
