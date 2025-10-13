package ar.edu.up.bugtracker;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {
    public static void main(String[] args) {
        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            emf = Persistence.createEntityManagerFactory("bugtrackerPU");
            em = emf.createEntityManager();

            // Prueba de round-trip con la base: SELECT 1
            Object result = em.createNativeQuery("SELECT 1").getSingleResult();
            System.out.println("Conexión OK. SELECT 1 => " + result);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al inicializar JPA/Hibernate: " + e.getMessage());
        } finally {
            if (em != null) em.close();
            if (emf != null) emf.close();
        }
    }
}