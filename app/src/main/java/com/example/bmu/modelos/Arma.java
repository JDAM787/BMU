package com.example.bmu.modelos;

public class Arma {
    protected int dañoAdicional;
    protected int durabilidad;

    public Arma(int dañoAdicional, int durabilidad) {
        this.dañoAdicional = dañoAdicional;
        this.durabilidad = durabilidad;
    }

    public void usarArma() {
        if (durabilidad > 0) {
            durabilidad--;
            System.out.println(this.getClass().getSimpleName() + " usada. Durabilidad restante: " + durabilidad);
        }
    }

    public boolean estaRota() {
        return durabilidad <= 0;
    }

    public int getdañoAdicional() {
        return estaRota() ? 0 : dañoAdicional;
    }
}
