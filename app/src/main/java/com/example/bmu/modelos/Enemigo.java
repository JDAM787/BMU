package com.example.bmu.modelos;

public class Enemigo extends Personaje {
    public Enemigo(int vidaMaxima, int dañoBase) {
        super(vidaMaxima, dañoBase);
    }

    public boolean isEsAferrable() {
        return true;
    }
}
