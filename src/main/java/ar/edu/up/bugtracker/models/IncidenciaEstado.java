package ar.edu.up.bugtracker.models;

import jakarta.persistence.*;

@Entity
@Table(name = "incidencia_estado")
public class IncidenciaEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NUEVA, EN_PROCESO, BLOQUEADA, EN_REVISION, TERMINADA 
    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
