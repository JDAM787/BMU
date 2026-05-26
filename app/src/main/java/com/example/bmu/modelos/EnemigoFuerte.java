package com.example.bmu.modelos;

public class EnemigoFuerte extends Enemigo {
    public EnemigoFuerte() {
        // Asignamos el doble de vida de un enemigo normal (100) y mayor daño (10)
        super(100, 10);
        // A diferencia del débil, este NO se puede agarrar ni lanzar
        this.esAferrable = false;
    }
}