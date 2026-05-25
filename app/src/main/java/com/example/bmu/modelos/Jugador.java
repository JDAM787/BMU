package com.example.bmu.modelos;

public class Jugador extends Personaje {
    private Arma armaEquipada;

    public Jugador(int vidaMaxima, int dañoBase) {
        super(vidaMaxima, dañoBase);
    }

    public void equiparArma(Arma nuevaArma) {
        this.armaEquipada = nuevaArma;
        System.out.println("Jugador equipa " + nuevaArma.getClass().getSimpleName() + ".");
    }

    @Override
    public void atacar(Personaje objetivo) {
        int dañoTotal = this.dañoBase;
        
        if (armaEquipada != null && !armaEquipada.estaRota()) {
            dañoTotal += armaEquipada.getdañoAdicional();
            System.out.println("Jugador ataca a " + objetivo.getClass().getSimpleName() + " usando " + armaEquipada.getClass().getSimpleName() + " haciendo " + dañoTotal + " de daño total.");
            armaEquipada.usarArma();
        } else {
            System.out.println("Jugador ataca a " + objetivo.getClass().getSimpleName() + " con los puños haciendo " + dañoTotal + " de daño.");
            if (armaEquipada != null && armaEquipada.estaRota()) {
                System.out.println("(El arma está rota y no hace daño adicional)");
            }
        }
        
        objetivo.recibirdaño(dañoTotal);
    }
}
