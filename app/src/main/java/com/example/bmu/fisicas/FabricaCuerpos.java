package com.example.bmu.fisicas;

import com.badlogic.gdx.physics.box2d.*;

import static com.example.bmu.fisicas.MundoFisico.aMetros;

/**
 * Fábrica de cuerpos Box2D para personajes, enemigos y armas.
 *
 * Categorías de colisión (bits) para filtrar qué choca con qué:
 *   CAT_SUELO    – el suelo estático
 *   CAT_JUGADOR  – el cuerpo del jugador
 *   CAT_ENEMIGO  – cuerpos de enemigos
 *   CAT_ARMA     – armas lanzadas / volando
 */
public class FabricaCuerpos {

    // ── Bits de categoría ────────────────────────────────────────────────────
    public static final short CAT_SUELO   = 0x0001;
    public static final short CAT_JUGADOR = 0x0002;
    public static final short CAT_ENEMIGO = 0x0004;
    public static final short CAT_ARMA    = 0x0008;

    // Máscaras: con qué categorías colisiona cada entidad
    private static final short MASK_JUGADOR = CAT_SUELO | CAT_ENEMIGO | CAT_ARMA;
    private static final short MASK_ENEMIGO = CAT_SUELO | CAT_JUGADOR | CAT_ENEMIGO | CAT_ARMA;
    private static final short MASK_ARMA    = CAT_SUELO | CAT_JUGADOR | CAT_ENEMIGO;

    private final World world;

    public FabricaCuerpos(World world) {
        this.world = world;
    }

    // ── Personajes ───────────────────────────────────────────────────────────

    /**
     * Crea el cuerpo Box2D del jugador.
     *
     * @param xPx posición inicial en píxeles
     * @param yPx posición inicial en píxeles
     * @param anchoPx ancho del sprite en píxeles
     * @param altoPx  alto  del sprite en píxeles
     * @param userData referencia al objeto Jugador (para detectar colisiones)
     */
    public Body crearCuerpoJugador(float xPx, float yPx,
                                   float anchoPx, float altoPx,
                                   Object userData) {
        return crearCuerpoPersonaje(xPx, yPx, anchoPx, altoPx,
                CAT_JUGADOR, MASK_JUGADOR, userData);
    }

    /**
     * Crea el cuerpo Box2D de un enemigo.
     *
     * @param aferrable si es {@code true} se genera con densidad reducida
     *                  (más fácil de lanzar); si es {@code false} se usa
     *                  densidad alta (enemigo pesado/inamovible).
     */
    public Body crearCuerpoEnemigo(float xPx, float yPx,
                                   float anchoPx, float altoPx,
                                   boolean aferrable, Object userData) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(aMetros(xPx), aMetros(yPx));
        def.fixedRotation = !aferrable; // Débil puede rotar al ser lanzado

        Body body = world.createBody(def);
        body.setUserData(userData);

        PolygonShape forma = new PolygonShape();
        forma.setAsBox(aMetros(anchoPx / 2f), aMetros(altoPx / 2f));

        FixtureDef fixture = new FixtureDef();
        fixture.shape    = forma;
        // Enemigo fuerte: muy denso → difícil de mover
        // Enemigo débil:  ligero   → puede ser agarrado y lanzado
        fixture.density     = aferrable ? 1.0f : 5.0f;
        fixture.friction    = 0.6f;
        fixture.restitution = aferrable ? 0.3f : 0.05f;
        fixture.filter.categoryBits = CAT_ENEMIGO;
        fixture.filter.maskBits     = MASK_ENEMIGO;

        body.createFixture(fixture);
        forma.dispose();
        return body;
    }

    // ── Armas ────────────────────────────────────────────────────────────────

    /**
     * Crea el cuerpo de un arma lanzada (Cuchillo, TuboMetal…).
     * Se genera como proyectil (bullet=true) para mayor precisión.
     */
    public Body crearCuerpoArma(float xPx, float yPx,
                                float anchoPx, float altoPx,
                                Object userData) {
        BodyDef def = new BodyDef();
        def.type   = BodyDef.BodyType.DynamicBody;
        def.position.set(aMetros(xPx), aMetros(yPx));
        def.bullet = true;             // detección continua de colisiones
        def.gravityScale = 0.5f;       // caída más lenta → trayectoria de lanzamiento

        Body body = world.createBody(def);
        body.setUserData(userData);

        PolygonShape forma = new PolygonShape();
        forma.setAsBox(aMetros(anchoPx / 2f), aMetros(altoPx / 2f));

        FixtureDef fixture = new FixtureDef();
        fixture.shape       = forma;
        fixture.density     = 0.5f;
        fixture.friction    = 0.3f;
        fixture.restitution = 0.4f;   // Rebota un poco al impactar el suelo
        fixture.filter.categoryBits = CAT_ARMA;
        fixture.filter.maskBits     = MASK_ARMA;

        body.createFixture(fixture);
        forma.dispose();
        return body;
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private Body crearCuerpoPersonaje(float xPx, float yPx,
                                      float anchoPx, float altoPx,
                                      short categoria, short mascara,
                                      Object userData) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(aMetros(xPx), aMetros(yPx));
        def.fixedRotation = true; // El jugador no rota

        Body body = world.createBody(def);
        body.setUserData(userData);

        PolygonShape forma = new PolygonShape();
        forma.setAsBox(aMetros(anchoPx / 2f), aMetros(altoPx / 2f));

        FixtureDef fixture = new FixtureDef();
        fixture.shape    = forma;
        fixture.density  = 1.5f;
        fixture.friction = 0.7f;
        fixture.restitution = 0f;
        fixture.filter.categoryBits = categoria;
        fixture.filter.maskBits     = mascara;

        body.createFixture(fixture);
        forma.dispose();
        return body;
    }
}