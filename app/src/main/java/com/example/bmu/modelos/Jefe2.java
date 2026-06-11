package com.example.bmu.modelos;

public class Jefe2 extends Enemigo {
    public Jefe2() {
        // Vida muy alta y mucho daño
        super(400, 20);
        this.esAferrable = false; // Los jefes no se pueden agarrar
    }
}
