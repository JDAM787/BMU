package com.example.bmu.modelos;

public class Jefe1 extends Enemigo {
    public Jefe1() {
        // Vida alta y daño base
        super(300, 20);
        this.esAferrable = false; // Los jefes no se pueden agarrar
    }
}
