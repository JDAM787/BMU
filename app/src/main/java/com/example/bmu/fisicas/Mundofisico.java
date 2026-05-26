package com.example.bmu.fisicas;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Gestiona el mundo físico Box2D del juego.
 * Controla la gravedad, el paso de tiempo y los cuerpos físicos globales.
 */
public class MundoFisico {

    // Escala píxeles → metros para Box2D (Box2D trabaja en metros)
    public static final float PPM = 64f; // 64 píxeles = 1 metro

    // Gravedad: -9.8 m/s² en el eje Y (hacia abajo)
    private static final Vector2 GRAVEDAD = new Vector2(0, -9.8f);

    // Parámetros del paso de simulación
    private static final float TIME_STEP        = 1 / 60f;
    private static final int   VELOCITY_ITERS   = 6;
    private static final int   POSITION_ITERS   = 2;

    private final World world;
    private float acumulador = 0f;

    public MundoFisico() {
        Box2D.init();
        world = new World(GRAVEDAD, true);
        crearSuelo();
    }

    /** Crea un suelo estático para que los objetos no caigan infinitamente. */
    private void crearSuelo() {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        def.position.set(0, 0);

        Body suelo = world.createBody(def);

        EdgeShape forma = new EdgeShape();
        // Línea horizontal en Y=0 que abarca todo el escenario visible
        forma.set(new Vector2(-100, 0), new Vector2(100, 0));

        FixtureDef fixture = new FixtureDef();
        fixture.shape  = forma;
        fixture.friction    = 0.8f;
        fixture.restitution = 0.1f; // Pequeño rebote al caer
        suelo.createFixture(fixture);

        forma.dispose();
    }

    /**
     * Avanza la simulación física con paso de tiempo fijo (acumulador).
     * Llamar desde el game loop principal con el delta real.
     *
     * @param delta segundos transcurridos desde el último frame
     */
    public void actualizar(float delta) {
        acumulador += delta;
        while (acumulador >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);
            acumulador -= TIME_STEP;
        }
    }

    /** Libera recursos de Box2D al destruir la pantalla. */
    public void dispose() {
        world.dispose();
    }

    public World getWorld() {
        return world;
    }

    // ── Helpers de conversión ────────────────────────────────────────────────

    /** Convierte píxeles a metros (para enviar a Box2D). */
    public static float aPx(float metros) {
        return metros * PPM;
    }

    /** Convierte metros a píxeles (para dibujar en pantalla). */
    public static float aMetros(float px) {
        return px / PPM;
    }
}