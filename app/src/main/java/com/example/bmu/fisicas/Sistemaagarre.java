package com.example.bmu.fisicas;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.example.bmu.modelos.Enemigo;
import com.example.bmu.modelos.Jugador;

/**
 * Sistema de Agarre y Lanzamiento (Beat'em up core mechanic).
 *
 * Flujo de uso:
 *   1. jugadorIntentaAgarrar()  – comprueba esAferrable y registra el agarre
 *   2. lanzarEnemigo()          – aplica un impulso al cuerpo del enemigo
 *   3. lanzarArma()             – lanza un arma del inventario del jugador
 *
 * Todos los impulsos se calculan en metros/s (unidades Box2D).
 */
public class SistemaAgarre {

    // ── Fuerza de lanzamiento ────────────────────────────────────────────────
    /** Velocidad horizontal al lanzar un enemigo (m/s). */
    private static final float VEL_LANZAMIENTO_X   = 6f;
    /** Velocidad vertical al lanzar un enemigo (m/s, hacia arriba). */
    private static final float VEL_LANZAMIENTO_Y   = 4f;

    /** Velocidad horizontal al lanzar un arma (m/s). */
    private static final float VEL_ARMA_X          = 10f;
    /** Velocidad vertical al lanzar un arma (m/s). */
    private static final float VEL_ARMA_Y          = 2f;

    // ── Estado del agarre ────────────────────────────────────────────────────
    private Enemigo enemigoAgarrado   = null;
    private Body    cuerpoAgarrado    = null;
    private boolean tieneAgarre       = false;

    // ── Referencia al jugador ────────────────────────────────────────────────
    private final Jugador jugador;
    private final Body    cuerpoJugador;

    public SistemaAgarre(Jugador jugador, Body cuerpoJugador) {
        this.jugador      = jugador;
        this.cuerpoJugador = cuerpoJugador;
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * El jugador intenta agarrar a un enemigo cercano.
     *
     * @param enemigo      modelo del enemigo objetivo
     * @param cuerpoEnemigo cuerpo Box2D del enemigo
     * @return {@code true} si el agarre fue exitoso
     */
    public boolean jugadorIntentaAgarrar(Enemigo enemigo, Body cuerpoEnemigo) {
        if (tieneAgarre) {
            System.out.println("[Agarre] Ya tienes a alguien agarrado.");
            return false;
        }
        if (!enemigo.isEsAferrable()) {
            System.out.println("[Agarre] " + enemigo.getClass().getSimpleName()
                    + " es demasiado grande/fuerte para ser agarrado.");
            return false;
        }

        enemigoAgarrado  = enemigo;
        cuerpoAgarrado   = cuerpoEnemigo;
        tieneAgarre      = true;

        // Mientras está agarrado, el enemigo no se mueve por físicas
        cuerpoEnemigo.setLinearVelocity(0, 0);
        cuerpoEnemigo.setAngularVelocity(0);
        cuerpoEnemigo.setGravityScale(0f); // Suspendido en el aire

        System.out.println("[Agarre] Jugador agarra a "
                + enemigo.getClass().getSimpleName() + ".");
        return true;
    }

    /**
     * Lanza al enemigo agarrado en la dirección indicada.
     *
     * @param direccion +1 para lanzar a la derecha, -1 para la izquierda
     */
    public void lanzarEnemigo(int direccion) {
        if (!tieneAgarre || cuerpoAgarrado == null) {
            System.out.println("[Agarre] No hay enemigo agarrado para lanzar.");
            return;
        }

        // Restaurar gravedad antes del impulso
        cuerpoAgarrado.setGravityScale(1f);

        Vector2 impulso = new Vector2(
                VEL_LANZAMIENTO_X * direccion,
                VEL_LANZAMIENTO_Y
        );
        cuerpoAgarrado.setLinearVelocity(impulso);

        // El enemigo recibe daño de impacto al ser lanzado (golpe inicial)
        enemigoAgarrado.recibirDaño(jugador.getDañoBase() / 2);

        System.out.println("[Agarre] Jugador lanza a "
                + enemigoAgarrado.getClass().getSimpleName()
                + " con velocidad " + impulso + ".");

        soltarAgarre();
    }

    /**
     * Suelta al enemigo sin lanzarlo (por ejemplo si el jugador recibe daño).
     */
    public void soltarAgarre() {
        if (cuerpoAgarrado != null) {
            cuerpoAgarrado.setGravityScale(1f);
        }
        enemigoAgarrado = null;
        cuerpoAgarrado  = null;
        tieneAgarre     = false;
        System.out.println("[Agarre] Agarre liberado.");
    }

    /**
     * Lanza un arma física en la dirección indicada.
     *
     * @param cuerpoArma   cuerpo Box2D del arma (ya creado por FabricaCuerpos)
     * @param direccion    +1 derecha / -1 izquierda
     * @param anguloGrados ángulo de lanzamiento en grados (0 = horizontal)
     */
    public void lanzarArma(Body cuerpoArma, int direccion, float anguloGrados) {
        if (cuerpoArma == null) {
            System.out.println("[Arma] No hay arma para lanzar.");
            return;
        }

        // Posicionar el arma en la mano del jugador antes de lanzarla
        Vector2 posJugador = cuerpoJugador.getPosition();
        cuerpoArma.setTransform(
                posJugador.x + aMetros(20) * direccion,
                posJugador.y + aMetros(10),
                0
        );

        double rad = Math.toRadians(anguloGrados);
        float vx = (float) (VEL_ARMA_X * Math.cos(rad)) * direccion;
        float vy = (float) (VEL_ARMA_Y + VEL_ARMA_X * Math.sin(rad));

        cuerpoArma.setLinearVelocity(vx, vy);
        // Rotación del arma mientras vuela (efecto visual)
        cuerpoArma.setAngularVelocity(direccion * 10f);

        System.out.println("[Arma] Arma lanzada → vel(" + vx + ", " + vy + ").");
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public boolean tienEnemigoAgarrado() {
        return tieneAgarre;
    }

    public Enemigo getEnemigoAgarrado() {
        return enemigoAgarrado;
    }

    // ── Helper privado ───────────────────────────────────────────────────────

    private static float aMetros(float px) {
        return px / MundoFisico.PPM;
    }
}