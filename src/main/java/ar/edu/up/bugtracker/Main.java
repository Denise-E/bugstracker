package ar.edu.up.bugtracker;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.controller.UserRoleController; // usás este nombre: OK
import ar.edu.up.bugtracker.dao.PerfilUsuarioDao;
import ar.edu.up.bugtracker.dao.UserDao;
import ar.edu.up.bugtracker.service.PerfilUsuarioService;
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
        // Zona horaria coherente con la DB
        System.setProperty("user.timezone", "UTC");

        // (Opcional) Etiquetas de diálogos en español
        UIManager.put("OptionPane.yesButtonText", "Sí");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.okButtonText", "Aceptar");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");

        // Cargar credenciales desde config/local.properties
        Properties p = new Properties();
        try (var in = Files.newInputStream(Paths.get("config/local.properties"))) {
            p.load(in);
        }

        String url  = p.getProperty("db.url");
        String user = p.getProperty("db.user");
        String pass = p.getProperty("db.pass");
        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("Faltan db.url/db.user/db.pass en config/local.properties");
        }

        Map<String,Object> props = Map.of(
                "jakarta.persistence.jdbc.url",  url,
                "jakarta.persistence.jdbc.user", user,
                "jakarta.persistence.jdbc.password", pass
                // (opcional) "hibernate.dialect", "org.hibernate.dialect.MySQLDialect"
        );

        // Boot de JPA/Hibernate con overrides
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("bugtrackerPU", props);
        EntityManager em = emf.createEntityManager();

        // Inicialización clases
        UserDao usuarioDao = new UserDao(em);
        UserService usuarioService = new UserService(usuarioDao, em);
        UserController usuarioController = new UserController(usuarioService);

        PerfilUsuarioDao perfilDao = new PerfilUsuarioDao(em);
        PerfilUsuarioService perfilService = new PerfilUsuarioService(perfilDao);
        UserRoleController roleController = new UserRoleController(perfilService);

        // Levantar UI
        SwingUtilities.invokeLater(() -> {
            PanelManager app = new PanelManager(usuarioController, roleController);
            app.setVisible(true);
        });

        // Cierre ordenado
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { if (em.isOpen()) em.close(); } catch (Exception ignored) {}
            try { if (emf.isOpen()) emf.close(); } catch (Exception ignored) {}
        }));
    }
}
