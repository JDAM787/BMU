package com.example.bmu.controladores;

import com.badlogic.gdx.physics.box2d.Body;
import com.example.bmu.fisicas.EntidadFisica;
import com.example.bmu.fisicas.SistemaAgarre;
import com.example.bmu.modelos.Cuchillo;
import com.example.bmu.modelos.Enemigo;
import com.example.bmu.modelos.Jugador;
import com.example.bmu.ui.ControlesTouch;

public class ControladorJugador {

    private EntidadFisica  entJugador;
    private ControlesTouch controles;
    private SistemaAgarre  sistemaAgarre;
    private GestorEnemigos gestorEnemigos;

    public float   stateTime           = 0f;
    public boolean mirandoDerecha      = true;
    public boolean isAtacando          = false;
    public float   tiempoAtacando      = 0f;
    public boolean isGrabPunching      = false;
    public float   tiempoGrabPunching  = 0f;
    public boolean isThrowing          = false;
    public float   tiempoThrowing      = 0f;
    public float   tiempoRecibeDanoGrab = 0f;
    public boolean isArmaRecogida      = false;

    private boolean agarrarAnteriorPresionado = false;
    private boolean lanzarAnteriorPresionado  = false;

    private static final float RANGO_GOLPE  = 1.8f;

    public ControladorJugador(EntidadFisica entJugador, ControlesTouch controles,
                               SistemaAgarre sistemaAgarre, GestorEnemigos gestorEnemigos) {
        this.entJugador    = entJugador;
        this.controles     = controles;
        this.sistemaAgarre = sistemaAgarre;
        this.gestorEnemigos = gestorEnemigos;
    }

    public void manejarEntradaTactil(Body cuerpoArma) {

        boolean enHurt = entJugador.getModelo().tiempoHurt > 0;

        // ── Movimiento: siempre disponible, incluso en hurt ──────────────────
        float dirX              = controles.getDirX();
        float xJugadorMetros    = entJugador.getCuerpo().getPosition().x;
        float limiteIzquierdo   = 0.8f;
        float limiteDerecho     = 19.2f;

        if (dirX < 0 && xJugadorMetros <= limiteIzquierdo) {
            dirX = 0;
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
        }
        if (dirX > 0 && xJugadorMetros >= limiteDerecho) {
            dirX = 0;
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
        }

        if (Math.abs(dirX) > 0.001f) {
            float vel   = controles.isCorriendoRapido() ? 6f : 3f;
            float signo = dirX > 0 ? 1f : -1f;
            entJugador.getCuerpo().setLinearVelocity(signo * vel,
                    entJugador.getCuerpo().getLinearVelocity().y);
            mirandoDerecha = dirX > 0;
        } else {
            entJugador.getCuerpo().setLinearVelocity(0,
                    entJugador.getCuerpo().getLinearVelocity().y);
        }

        if (controles.saltarPresionado) entJugador.saltar();

        // ── Acciones de combate: bloqueadas durante hurt ──────────────────────
        if (enHurt) {
            // Actualizar flags de estado previo para no disparar acciones al salir del hurt
            agarrarAnteriorPresionado = controles.agarrarPresionado;
            lanzarAnteriorPresionado  = controles.lanzarPresionado;
            return;
        }

        // ── Golpe ─────────────────────────────────────────────────────────────
        if (controles.golpePresionado) {
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                if (!isGrabPunching) {
                    isGrabPunching      = true;
                    tiempoGrabPunching  = 0f;
                    stateTime           = 0f;

                    Enemigo objetivo = sistemaAgarre.getEnemigoAgarrado();
                    if (objetivo != null) {
                        Jugador jug = (Jugador) entJugador.getModelo();
                        jug.atacar(objetivo);
                        tiempoRecibeDanoGrab = 0.25f;

                        if (!objetivo.estaVivo()) {
                            float dirEmpuje = mirandoDerecha ? 5f : -5f;
                            Body cEnemigo   = sistemaAgarre.getCuerpoAgarrado();
                            if (cEnemigo != null) cEnemigo.setLinearVelocity(dirEmpuje, 4f);
                            sistemaAgarre.soltarAgarre();
                        }
                    }
                }
            } else if (!isAtacando) {
                isAtacando      = true;
                tiempoAtacando  = 0f;
                stateTime       = 0f;

                Jugador jug     = (Jugador) entJugador.getModelo();
                Enemigo objetivo = enemigoMasCercano();
                if (objetivo != null) {
                    boolean estabaVivo = objetivo.estaVivo();
                    jug.atacar(objetivo);
                    if (estabaVivo && !objetivo.estaVivo()) {
                        float dirEmpuje = mirandoDerecha ? 5f : -5f;
                        Body cEnemigo   = buscarCuerpoEnemigo(objetivo);
                        if (cEnemigo != null) cEnemigo.setLinearVelocity(dirEmpuje, 4f);
                    }
                }
            }
        }

        // ── Agarre ────────────────────────────────────────────────────────────
        boolean agarrarAhora = controles.agarrarPresionado;
        if (agarrarAhora && !agarrarAnteriorPresionado) {
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                sistemaAgarre.soltarAgarre();
            } else {
                final float RANGO_AGARRE = 2.0f;
                EntidadFisica masCercano = null;
                float minDist = Float.MAX_VALUE;

                for (EntidadFisica e : gestorEnemigos.getEntidadesDebiles()) {
                    if (e.getModelo().estaVivo() && e.getModelo().isEsAferrable()) {
                        float d = Math.abs(xJugadorMetros - e.getCuerpo().getPosition().x);
                        if (d < minDist) { minDist = d; masCercano = e; }
                    }
                }
                for (EntidadFisica e : gestorEnemigos.getEntidadesFuertes()) {
                    if (e.getModelo().estaVivo() && e.getModelo().isEsAferrable()) {
                        float d = Math.abs(xJugadorMetros - e.getCuerpo().getPosition().x);
                        if (d < minDist) { minDist = d; masCercano = e; }
                    }
                }
                EntidadFisica eJefe2 = gestorEnemigos.getEntJefe2();
                if (eJefe2 != null && eJefe2.getModelo().estaVivo()
                        && gestorEnemigos.isJefe2Activado()
                        && eJefe2.getModelo().isEsAferrable()) {
                    float d = Math.abs(xJugadorMetros - eJefe2.getCuerpo().getPosition().x);
                    if (d < minDist) { minDist = d; masCercano = eJefe2; }
                }
                if (masCercano != null && minDist <= RANGO_AGARRE)
                    sistemaAgarre.jugadorIntentaAgarrar(
                            (Enemigo) masCercano.getModelo(), masCercano.getCuerpo());
            }
        }
        agarrarAnteriorPresionado = agarrarAhora;

        // ── Lanzar ────────────────────────────────────────────────────────────
        boolean lanzarAhora = controles.lanzarPresionado;
        if (lanzarAhora && !lanzarAnteriorPresionado) {
            int dir = controles.moviendoIzquierda() ? -1 : 1;
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                isThrowing      = true;
                tiempoThrowing  = 0f;
                stateTime       = 0f;
                Enemigo en      = sistemaAgarre.getEnemigoAgarrado();
                if (en != null) en.setLanzado(true);
                sistemaAgarre.lanzarEnemigo(dir);
            } else {
                Jugador jug = (Jugador) entJugador.getModelo();
                if (jug.getArmaEquipada() != null && jug.getArmaEquipada() instanceof Cuchillo) {
                    isArmaRecogida = false;
                    sistemaAgarre.lanzarArma(cuerpoArma, dir, 15f);
                    jug.equiparArma(null);
                } else {
                    System.out.println("[Lanzar] No tienes un cuchillo equipado para lanzar.");
                }
            }
        }
        lanzarAnteriorPresionado = lanzarAhora;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Body buscarCuerpoEnemigo(Enemigo objetivo) {
        for (EntidadFisica e : gestorEnemigos.getEntidadesDebiles())
            if (objetivo == e.getModelo()) return e.getCuerpo();
        for (EntidadFisica e : gestorEnemigos.getEntidadesFuertes())
            if (objetivo == e.getModelo()) return e.getCuerpo();
        if (gestorEnemigos.getEntJefe1() != null && objetivo == gestorEnemigos.getEntJefe1().getModelo())
            return gestorEnemigos.getEntJefe1().getCuerpo();
        if (gestorEnemigos.getEntJefe2() != null && objetivo == gestorEnemigos.getEntJefe2().getModelo())
            return gestorEnemigos.getEntJefe2().getCuerpo();
        if (gestorEnemigos.getEntJefe3() != null && objetivo == gestorEnemigos.getEntJefe3().getModelo())
            return gestorEnemigos.getEntJefe3().getCuerpo();
        return null;
    }

    private Enemigo enemigoMasCercano() {
        float xJ      = entJugador.getCuerpo().getPosition().x;
        float minDist = Float.MAX_VALUE;
        Enemigo mejor = null;

        for (EntidadFisica e : gestorEnemigos.getEntidadesDebiles()) {
            if (e.getModelo().estaVivo()) {
                float d = Math.abs(xJ - e.getCuerpo().getPosition().x);
                if (d < minDist) { minDist = d; mejor = (Enemigo) e.getModelo(); }
            }
        }
        for (EntidadFisica e : gestorEnemigos.getEntidadesFuertes()) {
            if (e.getModelo().estaVivo()) {
                float d = Math.abs(xJ - e.getCuerpo().getPosition().x);
                if (d < minDist) { minDist = d; mejor = (Enemigo) e.getModelo(); }
            }
        }
        EntidadFisica eJefe1 = gestorEnemigos.getEntJefe1();
        if (eJefe1 != null && eJefe1.getModelo().estaVivo() && gestorEnemigos.isJefe1Activado()) {
            float d = Math.abs(xJ - eJefe1.getCuerpo().getPosition().x);
            if (d < minDist) { minDist = d; mejor = (Enemigo) eJefe1.getModelo(); }
        }
        EntidadFisica eJefe2 = gestorEnemigos.getEntJefe2();
        if (eJefe2 != null && eJefe2.getModelo().estaVivo() && gestorEnemigos.isJefe2Activado()) {
            float d = Math.abs(xJ - eJefe2.getCuerpo().getPosition().x);
            if (d < minDist) { minDist = d; mejor = (Enemigo) eJefe2.getModelo(); }
        }
        EntidadFisica eJefe3 = gestorEnemigos.getEntJefe3();
        if (eJefe3 != null && eJefe3.getModelo().estaVivo() && gestorEnemigos.isJefe3Activado()) {
            float d = Math.abs(xJ - eJefe3.getCuerpo().getPosition().x);
            if (d < minDist) { minDist = d; mejor = (Enemigo) eJefe3.getModelo(); }
        }
        if (minDist > RANGO_GOLPE) return null;
        return mejor;
    }

    public void reset() {
        isAtacando      = false;
        isGrabPunching  = false;
        isThrowing      = false;
        isArmaRecogida  = false;
    }
}