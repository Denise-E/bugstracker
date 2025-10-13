package ar.edu.up.bugtracker.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencia")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Proyecto al que pertenece
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    // Responsable asignado (puede ser null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @Lob
    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    // Horas estimadas (nullable según tu script)
    @Column(name = "estimacion_horas", precision = 10, scale = 2)
    private BigDecimal estimacionHoras;

    // Puntero a la versión actual (nullable hasta crear la primera versión)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private IncidenciaVersion currentVersion;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public Usuario getResponsable() { return responsable; }
    public void setResponsable(Usuario responsable) { this.responsable = responsable; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getEstimacionHoras() { return estimacionHoras; }
    public void setEstimacionHoras(BigDecimal estimacionHoras) { this.estimacionHoras = estimacionHoras; }
    public IncidenciaVersion getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(IncidenciaVersion currentVersion) { this.currentVersion = currentVersion; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
