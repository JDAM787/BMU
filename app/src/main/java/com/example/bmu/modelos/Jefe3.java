package com.example.bmu.modelos;

public class Jefe3 extends Enemigo {
    public Jefe3() {
        // Vida más alta (jefe final) y mucho daño
        super(500, 30);
        this.esAferrable = false; // Los jefes no se pueden agarrar
    }
}
