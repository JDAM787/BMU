package com.example.bmu.fisicas;

import com.badlogic.gdx.physics.box2d.*;
import com.example.bmu.modelos.Arma;
import com.example.bmu.modelos.Enemigo;
import com.example.bmu.modelos.Jugador;
import com.example.bmu.modelos.Personaje;

/**
 * Escucha de contactos Box2D.
 *
 * Procesa todas las colisiones del mundo:
 *   • Arma lanzada  → Enemigo  : aplica daño del arma
 *   • Enemigo lanzado → Enemigo : golpe de cadena (splash)
 *   • Enemigo lanzado → Suelo  : daño de caída
 *
 * IMPORTANTE: Box2D prohíbido modificar cuerpos dentro de beginContact/endContact.
 * Por eso los eventos se encolan en {@link #eventosColision} y se procesan
 * de forma segura en {@link #procesarEventosPendientes()}, que debe llamarse
 * UNA VEZ por frame FUERA del world.step().
 */
public class EscuchaColisiones implements ContactListener {

    /** Datos de una colisión pendiente de procesar. */
    public static class EventoColision {
        public final Fixture a;
        public final Fixture b;

        public EventoColision(Fixture a, Fixture b) {
            this.a = a;
            this.b = b;
        }
    }

    // Cola simple (capacidad fija para evitar GC en el game loop)
    private static final int CAPACIDAD = 64;
    private final EventoColision[] cola  = new EventoColision[CAPACIDAD];
    private int cantidad = 0;
    public final java.util.List<Body> cuerposADestruir = new java.util.ArrayList<>();

    // Daño de impacto cuando un enemigo lanzado golpea el suelo
    private static final int DAÑO_CAIDA   = 10;
    // Daño cuando un enemigo lanzado golpea a otro enemigo (cadena)
    private static final int DAÑO_CADENA  = 15;

    // ── ContactListener ──────────────────────────────────────────────────────

    @Override
    public void beginContact(Contact contact) {
        if (cantidad < CAPACIDAD) {
            cola[cantidad++] = new EventoColision(
                    contact.getFixtureA(),
                    contact.getFixtureB()
            );
        }
    }

    @Override public void endContact(Contact contact) { /* no usado */ }
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Object udA = contact.getFixtureA().getBody().getUserData();
        Object udB = contact.getFixtureB().getBody().getUserData();

        if ((udA instanceof Arma && udB instanceof Personaje) ||
            (udB instanceof Arma && udA instanceof Personaje)) {
            contact.setEnabled(false);
        }
    }
    @Override public void postSolve(Contact contact, ContactImpulse impulse) { }

    // ── Procesamiento seguro (fuera de world.step) ───────────────────────────

    /**
     * Llama este método UNA VEZ por frame, DESPUÉS de {@link MundoFisico#actualizar}.
     */
    public void procesarEventosPendientes() {
        for (int i = 0; i < cantidad; i++) {
            procesarColision(cola[i].a, cola[i].b);
            cola[i] = null; // ayuda al GC
        }
        cantidad = 0;
    }

    public void limpiarEventos() {
        for (int i = 0; i < cantidad; i++) {
            cola[i] = null;
        }
        cantidad = 0;
    }

    // ── Lógica de colisión ───────────────────────────────────────────────────

    private void procesarColision(Fixture fA, Fixture fB) {
        Object udA = fA.getBody().getUserData();
        Object udB = fB.getBody().getUserData();

        // Arma → Enemigo
        if (udA instanceof Arma && udB instanceof Enemigo) {
            manejarArmaVsEnemigo((Arma) udA, (Enemigo) udB, fA.getBody());
        } else if (udB instanceof Arma && udA instanceof Enemigo) {
            manejarArmaVsEnemigo((Arma) udB, (Enemigo) udA, fB.getBody());
        }

        // Enemigo lanzado → Enemigo estático (golpe de cadena)
        else if (udA instanceof Enemigo && udB instanceof Enemigo) {
            manejarEnemigoVsEnemigo((Enemigo) udA, (Enemigo) udB, fA, fB);
        }

        // Personaje → Suelo (daño de caída solo si viene de alto)
        else if (udA instanceof Personaje && esCategoria(fB, FabricaCuerpos.CAT_SUELO)) {
            manejarCaida((Personaje) udA, fA.getBody());
        } else if (udB instanceof Personaje && esCategoria(fA, FabricaCuerpos.CAT_SUELO)) {
            manejarCaida((Personaje) udB, fB.getBody());
        }
    }

    private void manejarArmaVsEnemigo(Arma arma, Enemigo enemigo, Body cuerpoArma) {
        if (arma.estaRota()) return;

        // Solo inflige daño si tiene velocidad significativa (está siendo lanzada)
        float velX = cuerpoArma.getLinearVelocity().x;
        if (Math.abs(velX) < 2f) return;

        int daño = arma.getDañoAdicional();
        enemigo.recibirDaño(daño);
        arma.usarArma(); // Consume durabilidad del arma
        System.out.println("[Colisión] Arma impacta a "
                + enemigo.getClass().getSimpleName()
                + " → " + daño + " de daño. Arma rota: " + arma.estaRota());
        if (arma.estaRota()) {
            cuerposADestruir.add(cuerpoArma);
        } else {
            // Rebote / caída del arma al impactar
            cuerpoArma.setLinearVelocity(velX * -0.2f, 2f);
        }
    }

    private void manejarEnemigoVsEnemigo(Enemigo a, Enemigo b,
                                         Fixture fA, Fixture fB) {
        // Solo aplicar daño de cadena si alguno viene con velocidad alta (lanzado)
        float velA = fA.getBody().getLinearVelocity().len();
        float velB = fB.getBody().getLinearVelocity().len();

        if (velA > 2f) {
            b.recibirDaño(DAÑO_CADENA);
            System.out.println("[Cadena] " + a.getClass().getSimpleName()
                    + " lanzado golpea a " + b.getClass().getSimpleName()
                    + " → " + DAÑO_CADENA + " de daño.");
        }
        if (velB > 2f) {
            a.recibirDaño(DAÑO_CADENA);
            System.out.println("[Cadena] " + b.getClass().getSimpleName()
                    + " lanzado golpea a " + a.getClass().getSimpleName()
                    + " → " + DAÑO_CADENA + " de daño.");
        }
    }

    private void manejarCaida(Personaje personaje, Body cuerpo) {
        float velocidadCaida = cuerpo.getLinearVelocity().y;
        // Solo aplica daño si la velocidad de caída es significativa hacia abajo (negativa)
        if (velocidadCaida < -3f) {
            float velAbs = Math.abs(velocidadCaida);
            int daño = (int) (velAbs * DAÑO_CAIDA / 5f);
            personaje.recibirDaño(daño);
            System.out.println("[Caída] " + personaje.getClass().getSimpleName()
                    + " golpea el suelo → " + daño + " de daño (vel: "
                    + String.format("%.1f", velAbs) + " m/s).");
        }
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    private boolean esCategoria(Fixture fixture, short categoria) {
        return (fixture.getFilterData().categoryBits & categoria) != 0;
    }
}