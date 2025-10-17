package ar.edu.up.bugtracker;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.dao.UserDao;
import ar.edu.up.bugtracker.exceptions.AppException;
import ar.edu.up.bugtracker.exceptions.AuthException;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.exceptions.ValidationException;
import ar.edu.up.bugtracker.service.UserService;
import ar.edu.up.bugtracker.service.cmd.UserLoginCmd;
import ar.edu.up.bugtracker.service.cmd.UserRegisterCmd;
import ar.edu.up.bugtracker.service.cmd.UserUpdateCmd;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf = null;
        EntityManager em = null;

        try {
            // 0) Boot JPA/Hibernate
            emf = Persistence.createEntityManagerFactory("bugtrackerPU");
            em = emf.createEntityManager();

            // 1) Ping DB
            Object one = em.createNativeQuery("SELECT 1").getSingleResult();
            System.out.println("DB OK. SELECT 1 => " + one);

            // 2) Wiring capas
            UserDao UserDao = new UserDao(em);
            UserService UserService = new UserService(UserDao, em);
            UserController UserController = new UserController(UserService);

            // ---------- Smoke test CRUD de Usuario ----------
            String unique = String.valueOf(System.currentTimeMillis());
            String email = "test.user+" + unique + "@example.com";
            String pass = "Secreta123!";
            Long newUserId;

            // 2.1 REGISTER
            UserRegisterCmd reg = new UserRegisterCmd();
            reg.setNombre("Test");
            reg.setApellido("User");
            reg.setEmail(email);
            reg.setPassword(pass);
            reg.setPerfil("USUARIO"); // o "ADMIN" si querés
            newUserId = UserController.register(reg);
            System.out.println("[REGISTER] OK, id=" + newUserId + " email=" + email);

            // 2.2 LOGIN
            UserLoginCmd login = new UserLoginCmd();
            login.setEmail(email);
            login.setPassword(pass);
            UserLoggedInDto logged = UserController.login(login);
            System.out.println("[LOGIN] OK, id=" + logged.getId() + " nombre=" + logged.getNombre() + " perfil=" + logged.getPerfil());

            // 2.3 GET ALL
            List<UserDetailDto> all = UserController.getAll();
            System.out.println("[GET ALL] total=" + all.size());

            // 2.4 GET BY ID
            UserDetailDto detail = UserController.getById(newUserId);
            System.out.println("[GET BY ID] id=" + detail.getId() + " email=" + detail.getEmail() + " creadoEn=" + detail.getCreadoEn());

            // 2.5 UPDATE (cambiar nombre y password)
            UserUpdateCmd upd = new UserUpdateCmd();
            upd.setNombre("Tester");
            upd.setPassword("OtraClave456!");
            UserController.update(newUserId, upd);
            System.out.println("[UPDATE] OK (nombre y password)");

            // 2.6 DELETE
            UserController.delete(newUserId);
            System.out.println("[DELETE] OK id=" + newUserId);

            // 2.7 GET BY ID (debe fallar con NotFound)
            try {
                UserController.getById(newUserId);
                System.err.println("[GET BY ID tras DELETE] ERROR: no debería existir");
            } catch (NotFoundException expected) {
                System.out.println("[GET BY ID tras DELETE] NotFound esperado ✔");
            }

            System.out.println("CRUD Usuario E2E ✔");

        } catch (ValidationException e) {
            // Errores “esperables” de negocio/validación
            e.printStackTrace();
        } catch (AuthException e) {
            // Errores “esperables” de negocio/validación
            e.printStackTrace();
        } catch (NotFoundException e) {
            // Errores “esperables” de negocio/validación
            e.printStackTrace();
        } catch (AppException e) {
            // Errores “esperables” de negocio/validación
            e.printStackTrace();
        } catch (Exception e) {
            // Errores inesperados
            e.printStackTrace();
            System.err.println("Fallo general: " + e.getMessage());
        } finally {
            if (em != null) em.close();
            if (emf != null) emf.close();
        }
    }
}
