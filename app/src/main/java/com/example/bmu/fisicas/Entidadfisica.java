package com.example.bmu.fisicas;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.example.bmu.modelos.Personaje;

/**
 * Entidad Física: une un modelo de juego (Personaje, Arma…) con su cuerpo Box2D.
 *
 * Se encarga de:
 *   • Sincronizar la posición Box2D → posición en pantalla
 *   • Aplicar movimiento (caminar, saltar)
 *   • Exponer la posición en píxeles para el renderer
 */
public class EntidadFisica {

    private final Body    cuerpo;
    private final Personaje modelo;

    // Velocidad de movimiento horizontal (m/s)
    private static final float VEL_CAMINAR = 4f;
    // Impulso vertical del salto (m/s)
    private static final float VEL_SALTO   = 7f;

    // Umbral de velocidad vertical para considerar que está en el suelo
    private static final float UMBRAL_SUELO = 0.05f;

    public EntidadFisica(Body cuerpo, Personaje modelo) {
        this.cuerpo = cuerpo;
        this.modelo = modelo;
    }

    // ── Movimiento ───────────────────────────────────────────────────────────

    /**
     * Mueve la entidad horizontalmente.
     *
     * @param direccion +1 derecha / -1 izquierda / 0 parado
     */
    public void mover(float direccion) {
        float vx = direccion * VEL_CAMINAR;
        float vy = cuerpo.getLinearVelocity().y; // Conservar velocidad vertical
        cuerpo.setLinearVelocity(vx, vy);
    }

    /**
     * Aplica un impulso de salto si la entidad está en el suelo.
     *
     * @return {@code true} si el salto se ejecutó
     */
    public boolean saltar() {
        if (!estaEnSuelo()) return false;
        Vector2 vel = cuerpo.getLinearVelocity();
        cuerpo.setLinearVelocity(vel.x, VEL_SALTO);
        System.out.println(modelo.getClass().getSimpleName() + " salta.");
        return true;
    }

    /** Detiene el movimiento horizontal de inmediato. */
    public void detener() {
        cuerpo.setLinearVelocity(0, cuerpo.getLinearVelocity().y);
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    /**
     * Comprueba si la entidad está sobre el suelo (velocidad vertical ~0).
     */
    public boolean estaEnSuelo() {
        return Math.abs(cuerpo.getLinearVelocity().y) < UMBRAL_SUELO;
    }

    /**
     * Posición X en píxeles (para el renderer/sprite).
     */
    public float getXPx() {
        return MundoFisico.aPx(cuerpo.getPosition().x);
    }

    /**
     * Posición Y en píxeles (para el renderer/sprite).
     */
    public float getYPx() {
        return MundoFisico.aPx(cuerpo.getPosition().y);
    }

    /**
     * Ángulo de rotación en grados (útil para armas giradas en vuelo).
     */
    public float getAnguloGrados() {
        return (float) Math.toDegrees(cuerpo.getAngle());
    }

    public Body    getCuerpo()  { return cuerpo;  }
    public Personaje getModelo() { return modelo; }
}