package com.example.bmu.fisicas;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Gestiona el mundo físico Box2D del juego.
 * Controla la gravedad, el paso de tiempo y los cuerpos físicos globales.
 */
public class MundoFisico {
    public static final float PPM = 120f;
    private static final Vector2 GRAVEDAD = new Vector2(0, -9.8f);
    private static final float TIME_STEP      = 1 / 60f;
    private static final int   VELOCITY_ITERS = 6;
    private static final int   POSITION_ITERS = 2;

    private final World world;
    private float acumulador = 0f;
    private Body cuerpoSuelo; // ← AÑADIR referencia al suelo

    public MundoFisico() {
        Box2D.init();
        world = new World(GRAVEDAD, true);
        crearSuelo(1.5f); // valor inicial, se ajustará al cargar escenario
    }

    /** Crea un suelo estático para que los objetos no caigan infinitamente. */
    public void crearSuelo(float ySuelo) {
        // Destruir el suelo anterior si existe
        if (cuerpoSuelo != null) {
            world.destroyBody(cuerpoSuelo);
            cuerpoSuelo = null;
        }

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        def.position.set(0, 0);

        cuerpoSuelo = world.createBody(def);

        EdgeShape forma = new EdgeShape();
        forma.set(new Vector2(-100, ySuelo), new Vector2(100, ySuelo));

        FixtureDef fixture = new FixtureDef();
        fixture.shape       = forma;
        fixture.friction    = 0.8f;
        fixture.restitution = 0.1f;
        fixture.filter.categoryBits = FabricaCuerpos.CAT_SUELO;
        fixture.filter.maskBits     = -1; // colisiona con todo

        cuerpoSuelo.createFixture(fixture);
        forma.dispose();
    }

    public void actualizar(float delta) {
        acumulador += delta;
        while (acumulador >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);
            acumulador -= TIME_STEP;
        }
    }

    public void dispose() { 
        world.dispose(); 
    }

    public World getWorld() {
        return world;
    }

    public static float aPx(float metros){
        return metros * PPM;
    }
    public static float aMetros(float px){
        return px / PPM;
    }
}