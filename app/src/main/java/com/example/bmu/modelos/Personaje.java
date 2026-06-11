package com.example.bmu.modelos;

public class Personaje {
    protected int vidaActual;
    protected int vidaMaxima;
    protected int dañoBase;
    protected boolean esAferrable;

    public float tiempoHurt    = 0f;
    // Invencibilidad tras recibir daño (i-frames): solo el jugador la usa
    public float tiempoInvencible = 0f;
    private static final float DURACION_INVENCIBLE = 0.8f;

    protected boolean lanzado = false;

    public Personaje(int vidaMaxima, int dañoBase) {
        this.vidaMaxima  = vidaMaxima;
        this.vidaActual  = vidaMaxima;
        this.dañoBase    = dañoBase;
        this.esAferrable = true;
    }

    public boolean isEsAferrable() { return esAferrable; }

    public void recibirDaño(int cantidad) {
        // Si está en i-frames, ignorar el golpe
        if (tiempoInvencible > 0) return;

        this.vidaActual -= cantidad;
        if (this.vidaActual < 0) this.vidaActual = 0;

        this.tiempoHurt       = 0.15f;   // animación corta de hurt
        this.tiempoInvencible = DURACION_INVENCIBLE;

        System.out.println(this.getClass().getSimpleName()
                + " recibe " + cantidad + " de daño. Vida restante: " + this.vidaActual);
    }

    public void actualizar(float delta) {
        if (tiempoHurt       > 0) tiempoHurt       -= delta;
        if (tiempoInvencible > 0) tiempoInvencible -= delta;
    }

    public void atacar(Personaje objetivo) {
        System.out.println(this.getClass().getSimpleName()
                + " ataca a " + objetivo.getClass().getSimpleName()
                + " haciendo " + this.dañoBase + " de daño.");
        objetivo.recibirDaño(this.dañoBase);
    }

    public int  getVidaActual()  { return vidaActual;  }
    public int  getVidaMaxima()  { return vidaMaxima;  }
    public int  getDañoBase()    { return dañoBase;    }
    public boolean estaVivo()    { return vidaActual > 0; }

    public void revivir(int vida) {
        this.vidaActual       = vida;
        this.tiempoHurt       = 0f;
        this.tiempoInvencible = 0f;
    }

    public boolean isLanzado()            { return lanzado; }
    public void    setLanzado(boolean v)  { lanzado = v;    }
}