package ar.edu.up.bugtracker.models;

public enum IncidenciaEstadoEnum {
    NUEVA("NUEVA"),
    EN_PROCESO("EN_PROCESO"),
    BLOQUEADA("BLOQUEADA"),
    EN_REVISION("EN_REVISION"),
    TERMINADA("TERMINADA");

    private final String nombre;

    IncidenciaEstadoEnum(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}

