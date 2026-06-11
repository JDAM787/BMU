package com.example.bmu.modelos;

public class EnemigoDebil extends Enemigo {
    public EnemigoDebil() {
        // Asignamos estadísticas normales (50 de vida, 5 de daño)
        super(50, 5);
        // Este enemigo es un enemigo normal/débil, por lo tanto SÍ se puede agarrar
        this.esAferrable = true;
    }
}