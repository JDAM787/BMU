package com.example.bmu.modelos;

public class Jugador extends Personaje {
    private Arma armaEquipada;

    public Jugador(int vidaMaxima, int dañoBase) {
        super(vidaMaxima, dañoBase);
    }

    public Arma getArmaEquipada() {
        return armaEquipada;
    }

    public void equiparArma(Arma nuevaArma) {
        this.armaEquipada = nuevaArma;
        if (nuevaArma != null) {
            System.out.println("Jugador equipa " + nuevaArma.getClass().getSimpleName() + ".");
        } else {
            System.out.println("Jugador no tiene arma equipada.");
        }
    }

    @Override
    public void atacar(Personaje objetivo) {
        int dañoTotal = this.dañoBase;

        if (armaEquipada != null && !armaEquipada.estaRota()) {
            dañoTotal += armaEquipada.getDañoAdicional();
            System.out.println("Jugador ataca a " + objetivo.getClass().getSimpleName() + " usando " + armaEquipada.getClass().getSimpleName() + " haciendo " + dañoTotal + " de daño total.");
            armaEquipada.usarArma();
            if (armaEquipada.estaRota()) {
                System.out.println("¡El arma " + armaEquipada.getClass().getSimpleName() + " se ha roto!");
                equiparArma(null);
            }
        } else {
            System.out.println("Jugador ataca a " + objetivo.getClass().getSimpleName() + " con los puños haciendo " + dañoTotal + " de daño.");
            if (armaEquipada != null && armaEquipada.estaRota()) {
                System.out.println("(El arma está rota y no hace daño adicional)");
                equiparArma(null); // Just in case a broken weapon was still equipped
            }
        }

        objetivo.recibirDaño(dañoTotal);
    }
}