package com.example.bmu.modelos;

public class Personaje {
    protected int vidaActual;
    protected int vidaMaxima;
    protected int dañoBase;
    protected boolean esAferrable; // Propiedad para saber si se puede agarrar/lanzar

    public float tiempoHurt = 0f;
    protected boolean lanzado = false;

    public Personaje(int vidaMaxima, int dañoBase) {
        this.vidaMaxima = vidaMaxima;
        this.vidaActual = vidaMaxima;
        this.dañoBase = dañoBase;
        this.esAferrable = true; // Por defecto, es posible agarrar a los personajes
    }

    public boolean isEsAferrable() {
        return esAferrable;
    }

    public void recibirDaño(int cantidad) {
        this.vidaActual -= cantidad;
        if (this.vidaActual < 0) {
            this.vidaActual = 0;
        }
        this.tiempoHurt = 0.5f; // Duración de la animación de recibir daño
        System.out.println(this.getClass().getSimpleName() + " recibe " + cantidad + " de daño. Vida restante: " + this.vidaActual);
    }

    public void actualizar(float delta) {
        if (tiempoHurt > 0) {
            tiempoHurt -= delta;
        }
    }

    public void atacar(Personaje objetivo) {
        System.out.println(this.getClass().getSimpleName() + " ataca a " + objetivo.getClass().getSimpleName() + " haciendo " + this.dañoBase + " de daño.");
        objetivo.recibirDaño(this.dañoBase);
    }

    public int getVidaActual() {
        return vidaActual;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public int getDañoBase() {
        return dañoBase;
    }

    public boolean estaVivo() {
        return vidaActual > 0;
    }

    public void revivir(int vida) {
        this.vidaActual = vida;
        this.tiempoHurt = 0f;
    }

    public boolean isLanzado() {
        return lanzado;
    }

    public void setLanzado(boolean lanzado) {
        this.lanzado = lanzado;
    }
}