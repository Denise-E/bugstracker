package ar.edu.up.bugtracker;

import ar.edu.up.bugtracker.controller.ComentarioController;
import ar.edu.up.bugtracker.controller.IncidenciaController;
import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.controller.UserRoleController; // usás este nombre: OK
import ar.edu.up.bugtracker.dao.ComentarioDao;
import ar.edu.up.bugtracker.dao.IncidenciaDao;
import ar.edu.up.bugtracker.dao.IncidenciaVersionDao;
import ar.edu.up.bugtracker.dao.PerfilUsuarioDao;
import ar.edu.up.bugtracker.dao.ProyectoDao;
import ar.edu.up.bugtracker.dao.UserDao;
import ar.edu.up.bugtracker.service.ComentarioService;
import ar.edu.up.bugtracker.service.IncidenciaService;
import ar.edu.up.bugtracker.service.PerfilUsuarioService;
import ar.edu.up.bugtracker.service.ProyectoService;
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
        );

        // JPA/Hibernate
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("bugtrackerPU", props);
        EntityManager em = emf.createEntityManager();

        // Inicialización clases
        PerfilUsuarioDao perfilDao = new PerfilUsuarioDao(em);
        UserDao usuarioDao = new UserDao(em);
        UserService usuarioService = new UserService(usuarioDao, perfilDao, em);
        UserController usuarioController = new UserController(usuarioService);
        PerfilUsuarioService perfilService = new PerfilUsuarioService(perfilDao);
        UserRoleController roleController = new UserRoleController(perfilService);

        ProyectoDao proyectoDao = new ProyectoDao(em);
        IncidenciaDao incidenciaDao = new IncidenciaDao(em);
        ComentarioDao comentarioDao = new ComentarioDao(em);
        IncidenciaVersionDao incidenciaVersionDao = new IncidenciaVersionDao(em);
        ProyectoService proyectoService = new ProyectoService(proyectoDao, incidenciaDao, comentarioDao, incidenciaVersionDao, em);
        ProyectoController proyectoController = new ProyectoController(proyectoService);

        IncidenciaService incidenciaService = new IncidenciaService(incidenciaDao, incidenciaVersionDao, 
                                                                    usuarioDao, proyectoDao, comentarioDao, em);
        IncidenciaController incidenciaController = new IncidenciaController(incidenciaService);
        ComentarioService comentarioService = new ComentarioService(comentarioDao, usuarioDao, incidenciaDao, em);
        ComentarioController comentarioController = new ComentarioController(comentarioService);

        // Levantar UI
        SwingUtilities.invokeLater(() -> {
            PanelManager app = new PanelManager(usuarioController, roleController, proyectoController,
                    incidenciaController, comentarioController);
            app.setVisible(true);
        });

        // Cierre
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { if (em.isOpen()) em.close(); } catch (Exception ignored) {}
            try { if (emf.isOpen()) emf.close(); } catch (Exception ignored) {}
        }));
    }
}
