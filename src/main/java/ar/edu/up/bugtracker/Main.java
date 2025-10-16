package ar.edu.up.bugtracker;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.dao.UserDao;
import ar.edu.up.bugtracker.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {
    public static void main(String[] args) {
        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            // wiring básico
            emf = Persistence.createEntityManagerFactory("bugtrackerPU");
            em = emf.createEntityManager();

            UserDao UserDao = new UserDao(em);
            UserService UserService = new UserService(UserDao, em);
            UserController UserController = new UserController(UserService);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al inicializar JPA/Hibernate: " + e.getMessage());
        } finally {
            if (em != null) em.close();
            if (emf != null) emf.close();
        }
    }
}