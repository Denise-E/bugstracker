package ar.edu.up.bugtracker.models;

import jakarta.persistence.*;

@Entity
@Table(name = "perfil_usuario")
public class PerfilUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ADMIN, USUARIO 
    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
