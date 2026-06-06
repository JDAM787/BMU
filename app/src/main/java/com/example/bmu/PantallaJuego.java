package com.example.bmu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;

import com.example.bmu.fisicas.*;
import com.example.bmu.modelos.*;
import com.example.bmu.ui.ControlesTouch;
import com.example.bmu.mundo.GestorEscenarios;
import com.example.bmu.vista.AnimadorHeroe;
import com.example.bmu.vista.AnimadorEnemigoDebil;

/**
 * Cómo añadir esta pantalla desde tu Game principal:
 *   game.setScreen(new PantallaJuego());
 */
public class PantallaJuego implements Screen {

    // ── Físicas ──────────────────────────────────────────────────────────────
    private MundoFisico       mundo;
    private FabricaCuerpos    fabrica;
    private EscuchaColisiones escucha;
    private SistemaAgarre     sistemaAgarre;

    // ── Entidades ────────────────────────────────────────────────────────────
    private EntidadFisica entJugador;
    private EntidadFisica entEnemigoDebil;
    private EntidadFisica entEnemigoFuerte;

    private Body      cuerpoArma;
    private TuboMetal armaFisica;

    // ── UI táctil ────────────────────────────────────────────────────────────
    private ControlesTouch controles;

    // ── Render ───────────────────────────────────────────────────────────────
    private Box2DDebugRenderer debugRenderer;
    private ShapeRenderer      shapeRenderer;
    private OrthographicCamera camara;
    private SpriteBatch        batch;
    private Texture            texturaTubo;
    private Texture            texturaCuchillo;
    private GestorEscenarios   gestorEscenarios;
    private AnimadorHeroe      animadorHeroe;
    private AnimadorEnemigoDebil animadorEnemigoDebil;
    private float              stateTime = 0f;
    private boolean            mirandoDerecha = true;
    // Estado de animación anterior para detectar cambios
    private String             estadoAnimAnterior = "idle";
    // Para detectar el momento exacto en que se presiona el botón de agarre (edge detection)
    private boolean            agarrarAnteriorPresionado = false;
    // Para detectar el momento exacto en que se presiona el botón de lanzar (edge detection)
    private boolean            lanzarAnteriorPresionado = false;
    // Estado de ataque: bloquea otras animaciones mientras dura el golpe
    private boolean            isAtacando = false;
    private float              tiempoAtacando = 0f;        // cuánto llevas en el golpe
    private boolean            estabaCorriendoAlGolpear = false; // qué fila del sheet usar

    // Estados para EnemigoDebil
    private float stateTimeDebil = 0f;
    private float cooldownAtaqueDebil = 0f;
    private float tiempoAtacandoDebil = 0f;
    private boolean isAtacandoDebil = false;
    private float tiempoEnSueloMuerto = 0f;

    @Override
    public void show() {
        // 1. Mundo físico
        mundo   = new MundoFisico();
        fabrica = new FabricaCuerpos(mundo.getWorld());

        // 2. Listener de colisiones
        escucha = new EscuchaColisiones();
        mundo.getWorld().setContactListener(escucha);

        // 3. Modelos del juego
        Jugador      jugador  = new Jugador(100, 20);
        EnemigoDebil  enD     = new EnemigoDebil();
        EnemigoFuerte enF     = new EnemigoFuerte();

        // 4. Cuerpos Box2D (Aumentamos las cajas de colisión para que encajen con la nueva escala del sprite)
        // spawn cerca del suelo físico (Y en píxeles; 3.5m * 64 PPM = 224 + mitad cuerpo ~80 = 304)
        Body cJugador = fabrica.crearCuerpoJugador( 200, 304, 100, 160, jugador);
        Body cDebil   = fabrica.crearCuerpoEnemigo( 500, 304, 100, 160, true,  enD);
        Body cFuerte  = fabrica.crearCuerpoEnemigo( 750, 304, 120, 180, false, enF);

        // 5. Entidades físicas
        entJugador       = new EntidadFisica(cJugador, jugador);
        entEnemigoDebil  = new EntidadFisica(cDebil,   enD);
        entEnemigoFuerte = new EntidadFisica(cFuerte,  enF);

        // 6. Arma lanzable
        armaFisica = new TuboMetal();
        jugador.equiparArma(armaFisica);
        cuerpoArma = fabrica.crearCuerpoArma(200, 80, 30, 10, armaFisica);

        // 7. Sistema de agarre
        sistemaAgarre = new SistemaAgarre(jugador, cJugador);

        // 8. Controles táctiles → registrar como InputProcessor de LibGDX
        controles = new ControlesTouch();
        Gdx.input.setInputProcessor(controles);

        // 9. Render
        debugRenderer = new Box2DDebugRenderer();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        texturaTubo = new Texture("armas/tubo.png");
        texturaCuchillo = new Texture("armas/cuchillo.png");
        gestorEscenarios = new GestorEscenarios();
        animadorHeroe = new AnimadorHeroe();
        animadorEnemigoDebil = new AnimadorEnemigoDebil(1);
        camara = new OrthographicCamera();
        // Vista en metros para el debug renderer de Box2D
        camara.setToOrtho(false,
                Gdx.graphics.getWidth()  / MundoFisico.PPM,
                Gdx.graphics.getHeight() / MundoFisico.PPM);
    }

    @Override
    public void render(float delta) {
        // ── Limpiar pantalla ────────────────────────────────────────────────
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── Leer controles táctiles ─────────────────────────────────────────
        stateTime += delta;
        controles.actualizar();
        manejarEntradaTactil();

        // ── Actualizar Modelos e IA de Enemigos ─────────────────────────────
        actualizarEnemigos(delta);

        // ── Paso de física ──────────────────────────────────────────────────
        mundo.actualizar(delta);
        escucha.procesarEventosPendientes(); // siempre DESPUÉS de world.step

        // ── Arrastrar enemigo agarrado con el jugador ───────────────────────
        // Solo actualizamos la posición del ENEMIGO, nunca del jugador.
        // El jugador se mueve libre; el enemigo lo sigue sin ejercer fuerza de vuelta.
        if (sistemaAgarre.tienEnemigoAgarrado()) {
            com.badlogic.gdx.math.Vector2 posJ = entJugador.getCuerpo().getPosition();
            // Offset lateral: el enemigo va al lado del jugador mirando la dirección del héroe
            float offsetX = mirandoDerecha ? 0.8f : -0.8f;
            Body cAgarrado = sistemaAgarre.getCuerpoAgarrado();
            if (cAgarrado != null) {
                cAgarrado.setTransform(posJ.x + offsetX, posJ.y, 0f);
                cAgarrado.setLinearVelocity(0, 0); // no agrega inercia al jugador
            }
        }

        camara.update();


        // ── Renderizado de Sprites ──────────────────────────────────────────
        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        
        // Dibujar el escenario de fondo con el gestor
        float anchoPantallaM = Gdx.graphics.getWidth() / MundoFisico.PPM;
        float altoPantallaM = Gdx.graphics.getHeight() / MundoFisico.PPM;
        gestorEscenarios.dibujar(batch, anchoPantallaM, altoPantallaM);
        
        // Determinar qué animación usar según velocidad real + intensidad del joystick
        float velX = entJugador.getCuerpo().getLinearVelocity().x;
        float velY = entJugador.getCuerpo().getLinearVelocity().y;

        // Duración total del golpe = 8 frames x 0.07s = 0.56s
        final float DURACION_GOLPE = 8 * 0.07f;

        // Actualizar temporizador de ataque
        if (isAtacando) {
            tiempoAtacando += delta;
            if (tiempoAtacando >= DURACION_GOLPE) {
                isAtacando = false;
                tiempoAtacando = 0f;
            }
        }

        String estadoAnim;
        if (isAtacando) {
            // Usar la fila correcta según si estaba corriendo o no cuando golpeó
            estadoAnim = estabaCorriendoAlGolpear ? "punchRun" : "punch";
        } else if (Math.abs(velY) > 0.5f && animadorHeroe.animFall != null) {
            estadoAnim = "fall";
        } else if (Math.abs(velX) > 3.5f && animadorHeroe.animRun != null) {
            estadoAnim = "run";
        } else if (Math.abs(velX) > 0.3f && animadorHeroe.animWalk != null) {
            estadoAnim = "walk";
        } else {
            estadoAnim = "idle";
        }

        // Resetear el tiempo de animación cuando cambia el estado
        if (!estadoAnim.equals(estadoAnimAnterior)) {
            stateTime = 0f;
            estadoAnimAnterior = estadoAnim;
        }

        com.badlogic.gdx.graphics.g2d.TextureRegion frameActual;
        com.badlogic.gdx.graphics.g2d.TextureRegion primerFrame;
        switch (estadoAnim) {
            case "punch":
                frameActual = animadorHeroe.animPunch.getKeyFrame(stateTime, false);
                primerFrame = animadorHeroe.animPunch.getKeyFrame(0f);
                break;
            case "punchRun":
                frameActual = animadorHeroe.animPunchRun.getKeyFrame(stateTime, false);
                primerFrame = animadorHeroe.animPunchRun.getKeyFrame(0f);
                break;
            case "fall":
                frameActual = animadorHeroe.animFall.getKeyFrame(stateTime, true);
                primerFrame = animadorHeroe.animFall.getKeyFrame(0f);
                break;
            case "run":
                frameActual = animadorHeroe.animRun.getKeyFrame(stateTime, true);
                primerFrame = animadorHeroe.animRun.getKeyFrame(0f);
                break;
            case "walk":
                frameActual = animadorHeroe.animWalk.getKeyFrame(stateTime, true);
                primerFrame = animadorHeroe.animWalk.getKeyFrame(0f);
                break;
            default:
                frameActual = animadorHeroe.animIdle.getKeyFrame(stateTime, true);
                primerFrame = animadorHeroe.animIdle.getKeyFrame(0f);
                break;
        }

        // Actualizar dirección solo si no está atacando
        if (!isAtacando) {
            if (controles.getDirX() > 0) mirandoDerecha = true;
            if (controles.getDirX() < 0) mirandoDerecha = false;
        }

        float jugX = entJugador.getCuerpo().getPosition().x;
        float jugY = entJugador.getCuerpo().getPosition().y;
        
        // La caja de colisión del jugador mide 160 de alto (radio 80).
        float altoCuerpoM = 160f / MundoFisico.PPM;
        
        // Tamaño del sprite basado en su proporción original para evitar deformación
        // Usamos el primer frame de la animación como base para el aspect ratio para evitar deformación horizontal dinámica (wobble)
        float altoSpriteM = 350f / MundoFisico.PPM;
        float aspect = (float) primerFrame.getRegionWidth() / primerFrame.getRegionHeight();
        float anchoSpriteM = altoSpriteM * aspect;
        
        // Ajuste fino (offset) para alinear los pies visuales del sprite con la línea física.
        // Como tu imagen tiene espacio transparente debajo de los pies, bajamos el dibujo un poco.
        float offsetY_M = -45f / MundoFisico.PPM; 
        
        // Clonar la región de textura para evitar modificar permanentemente el frame compartido
        TextureRegion drawFrame = new TextureRegion(frameActual);
        if (!mirandoDerecha) {
            drawFrame.flip(true, false);
        }
        
        // Alinear la base de la imagen con la base de la caja de colisión, más el ajuste fino
        float dibX = jugX - anchoSpriteM / 2f;
        float dibY = jugY - (altoCuerpoM / 2f) + offsetY_M; 
        
        batch.draw(drawFrame, dibX, dibY, anchoSpriteM, altoSpriteM);

        // Dibujar Enemigo Debil
        dibujarEnemigoDebil(batch, entEnemigoDebil, animadorEnemigoDebil, stateTimeDebil, isAtacandoDebil);
        
        // Dibujar el tubo de metal atado a la física
        // El cuerpo del arma mide 30x10 px (según FabricaCuerpos).
        float armaX = cuerpoArma.getPosition().x;
        float armaY = cuerpoArma.getPosition().y;
        float anguloArma = cuerpoArma.getAngle() * MathUtils.radiansToDegrees;
        
        float anchoArma = 30f / MundoFisico.PPM;
        float altoArma = 10f / MundoFisico.PPM;
        
        // Se dibuja centrando la textura en la posición física
        batch.draw(texturaTubo, 
                armaX - anchoArma / 2f, 
                armaY - altoArma / 2f, 
                anchoArma / 2f, altoArma / 2f, 
                anchoArma, altoArma, 
                1f, 1f, 
                anguloArma, 
                0, 0, texturaTubo.getWidth(), texturaTubo.getHeight(), 
                false, false);
                
        batch.end();

        // ── Dibujar HUD táctil ──────────────────────────────────────────────
        // ShapeRenderer usa coordenadas de pantalla (píxeles), no metros
        shapeRenderer.setProjectionMatrix(
                new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );
        controles.dibujar(shapeRenderer);
        
        // ── Debug render de Box2D (Dibuja las cajas de colisión y el suelo) ─────────
        // Lo ponemos al final para que las líneas no queden tapadas por el fondo
        camara.update();
        debugRenderer.render(mundo.getWorld(), camara.combined);
    }

    // ── Lógica de entrada táctil ─────────────────────────────────────────────

    private void manejarEntradaTactil() {
        float dirX = controles.getDirX();

        // Bloquear retroceso: el jugador no puede pasar el borde izquierdo del escenario
        float xJugadorMetros = entJugador.getCuerpo().getPosition().x;
        float limiteIzquierdoMetros = 0.8f;
        if (dirX < 0 && xJugadorMetros <= limiteIzquierdoMetros) {
            dirX = 0;
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
        }

        // Velocidad diferenciada: correr vs caminar según intensidad del joystick
        if (Math.abs(dirX) > 0.001f) {
            float vel = controles.isCorriendoRapido() ? 6f : 3f; // m/s
            float signo = dirX > 0 ? 1f : -1f;
            entJugador.getCuerpo().setLinearVelocity(
                signo * vel,
                entJugador.getCuerpo().getLinearVelocity().y
            );
        } else {
            // Sin input de joystick: frenar horizontalmente
            entJugador.getCuerpo().setLinearVelocity(
                0,
                entJugador.getCuerpo().getLinearVelocity().y
            );
        }

        // Salto
        if (controles.saltarPresionado) {
            entJugador.saltar();
        }

        // Golpe (botón rojo): activar solo en el primer frame de la pulsación
        if (controles.golpePresionado && !isAtacando) {
            isAtacando = true;
            tiempoAtacando = 0f;
            stateTime = 0f; // reiniciar animación desde frame 0
            // Guardar si estaba corriendo para elegir la fila correcta del sheet
            float vx = entJugador.getCuerpo().getLinearVelocity().x;
            estabaCorriendoAlGolpear = Math.abs(vx) > 3.5f;
            // Aplicar daño al enemigo más cercano
            Jugador jug = (Jugador) entJugador.getModelo();
            Enemigo objetivo = enemigoMasCercano();
            if (objetivo != null) {
                boolean estabaVivoAntes = objetivo.estaVivo();
                jug.atacar(objetivo);
                
                // Si el enemigo muere por este golpe, aplicar empuje hacia atrás
                if (estabaVivoAntes && !objetivo.estaVivo()) {
                    float dirEmpuje = mirandoDerecha ? 5f : -5f;
                    Body cEnemigo = null;
                    if (entEnemigoDebil != null && objetivo == entEnemigoDebil.getModelo()) {
                        cEnemigo = entEnemigoDebil.getCuerpo();
                    } else if (entEnemigoFuerte != null && objetivo == entEnemigoFuerte.getModelo()) {
                        cEnemigo = entEnemigoFuerte.getCuerpo();
                    }
                    if (cEnemigo != null) {
                        cEnemigo.setLinearVelocity(dirEmpuje, 4f);
                    }
                }
            }
        }

        // Agarrar: EDGE DETECTION — solo se ejecuta en el frame exacto que se presiona el botón
        boolean agarrarAhora = controles.agarrarPresionado;
        if (agarrarAhora && !agarrarAnteriorPresionado) {
            // Primera pulsación: si ya tiene enemigo agarrado, lo suelta
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                sistemaAgarre.soltarAgarre();
            } else {
                // Si no, intenta agarrar al enemigo más cercano
                float RANGO_AGARRE = 2.0f;
                EntidadFisica masCercano = null;
                float minDist = Float.MAX_VALUE;
                
                if (entEnemigoDebil != null && entEnemigoDebil.getModelo().estaVivo()) {
                    float d = Math.abs(xJugadorMetros - entEnemigoDebil.getCuerpo().getPosition().x);
                    if (d < minDist) { minDist = d; masCercano = entEnemigoDebil; }
                }
                if (entEnemigoFuerte.getModelo().estaVivo()) {
                    float d = Math.abs(xJugadorMetros - entEnemigoFuerte.getCuerpo().getPosition().x);
                    if (d < minDist) { minDist = d; masCercano = entEnemigoFuerte; }
                }
                
                if (masCercano != null && minDist <= RANGO_AGARRE) {
                    sistemaAgarre.jugadorIntentaAgarrar(
                        (Enemigo) masCercano.getModelo(),
                        masCercano.getCuerpo()
                    );
                }
            }
        }
        agarrarAnteriorPresionado = agarrarAhora;

        // Lanzar: EDGE DETECTION
        boolean lanzarAhora = controles.lanzarPresionado;
        if (lanzarAhora && !lanzarAnteriorPresionado) {
            int dir = controles.moviendoIzquierda() ? -1 : 1;
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                sistemaAgarre.lanzarEnemigo(dir);
            } else {
                sistemaAgarre.lanzarArma(cuerpoArma, dir, 15f);
            }
        }
        lanzarAnteriorPresionado = lanzarAhora;
    }

    /** Devuelve el enemigo vivo más cercano al jugador (para recibir golpes). */
    private Enemigo enemigoMasCercano() {
        float xJ = entJugador.getCuerpo().getPosition().x;
        float minDist = Float.MAX_VALUE;
        Enemigo masCercano = null;

        if (entEnemigoDebil != null && entEnemigoDebil.getModelo().estaVivo()) {
            float d = Math.abs(xJ - entEnemigoDebil.getCuerpo().getPosition().x);
            if (d < minDist) {
                minDist = d;
                masCercano = (Enemigo) entEnemigoDebil.getModelo();
            }
        }
        if (entEnemigoFuerte.getModelo().estaVivo()) {
            float d = Math.abs(xJ - entEnemigoFuerte.getCuerpo().getPosition().x);
            if (d < minDist) {
                minDist = d;
                masCercano = (Enemigo) entEnemigoFuerte.getModelo();
            }
        }
        return masCercano;
    }

    // ── Ciclo de vida de Screen ───────────────────────────────────────────────

    @Override
    public void resize(int w, int h) {
        camara.setToOrtho(false, w / MundoFisico.PPM, h / MundoFisico.PPM);
    }

    @Override public void pause()  { }
    @Override public void resume() { }
    @Override public void hide()   { }

    @Override
    public void dispose() {
        mundo.dispose();
        debugRenderer.dispose();
        shapeRenderer.dispose();
        batch.dispose();
        texturaTubo.dispose();
        texturaCuchillo.dispose();
        gestorEscenarios.dispose();
        animadorHeroe.dispose();
        if (animadorEnemigoDebil != null) animadorEnemigoDebil.dispose();
    }

    private void actualizarEnemigos(float delta) {
        entJugador.getModelo().actualizar(delta);
        
        if (entEnemigoDebil != null) {
            entEnemigoDebil.getModelo().actualizar(delta);
            actualizarIA(delta, entEnemigoDebil);
            
            // Si está muerto, iniciar temporizador de desaparición de manera robusta (sin depender de estaEnSuelo para evitar vibraciones)
            if (!entEnemigoDebil.getModelo().estaVivo()) {
                tiempoEnSueloMuerto += delta;
                if (tiempoEnSueloMuerto >= 3.0f) {
                    if (sistemaAgarre.getEnemigoAgarrado() == entEnemigoDebil.getModelo()) {
                        sistemaAgarre.soltarAgarre();
                    }
                    mundo.getWorld().destroyBody(entEnemigoDebil.getCuerpo());
                    entEnemigoDebil = null;
                }
            }
        }
    }
    
    private void actualizarIA(float delta, EntidadFisica enemigo) {
        if (!enemigo.getModelo().estaVivo()) {
            if (enemigo.estaEnSuelo()) {
                enemigo.detener();
            }
            return;
        }

        // Si está agarrado por el jugador, no hace nada
        if (sistemaAgarre.tienEnemigoAgarrado() && sistemaAgarre.getEnemigoAgarrado() == enemigo.getModelo()) {
            return;
        }

        // Actualizar cooldown y tiempo de ataque
        stateTimeDebil += delta;
        if (cooldownAtaqueDebil > 0) cooldownAtaqueDebil -= delta;
        if (isAtacandoDebil) {
            tiempoAtacandoDebil += delta;
            if (tiempoAtacandoDebil >= 0.3f) {
                isAtacandoDebil = false;
                tiempoAtacandoDebil = 0f;
            }
        }

        // Si está herido o atacando, detener físicas horizontales
        if (enemigo.getModelo().tiempoHurt > 0 || isAtacandoDebil) {
            enemigo.detener();
            return;
        }

        float xJ = entJugador.getCuerpo().getPosition().x;
        float xE = enemigo.getCuerpo().getPosition().x;
        float dx = xJ - xE;

        if (Math.abs(dx) > 1.2f) {
            // Moverse hacia el jugador
            float dir = Math.signum(dx);
            enemigo.mover(dir * 0.5f); // 0.5f es la velocidad/dirección reducida
        } else {
            enemigo.detener();
            // Atacar si el jugador está vivo y no hay cooldown
            if (entJugador.getModelo().estaVivo() && cooldownAtaqueDebil <= 0) {
                isAtacandoDebil = true;
                tiempoAtacandoDebil = 0f;
                stateTimeDebil = 0f;
                cooldownAtaqueDebil = 1.5f;
                enemigo.getModelo().atacar(entJugador.getModelo());
            }
        }
    }

    private void dibujarEnemigoDebil(SpriteBatch batch, EntidadFisica enemigo, AnimadorEnemigoDebil animador, float stateTime, boolean isAtacando) {
        if (enemigo == null) return;

        // Parpadeo antes de desaparecer (entre 1.5s y 3.0s de estar muerto)
        if (!enemigo.getModelo().estaVivo() && tiempoEnSueloMuerto > 1.5f) {
            boolean mostrar = ((int)((tiempoEnSueloMuerto - 1.5f) * 15)) % 2 == 0;
            if (!mostrar) return; // Se salta el dibujado en este frame para lograr el parpadeo
        }
        
        float enX = enemigo.getCuerpo().getPosition().x;
        float enY = enemigo.getCuerpo().getPosition().y;
        
        float altoCuerpoM = 160f / MundoFisico.PPM;
        float altoSpriteM = 350f / MundoFisico.PPM;
        float offsetY_M = -45f / MundoFisico.PPM;

        TextureRegion frameActual;
        TextureRegion primerFrame;
        
        if (!enemigo.getModelo().estaVivo()) {
            // Si está muerto y lleva menos de 1.0s o no está en el suelo, usar animación de caída
            if (tiempoEnSueloMuerto < 1.0f && !enemigo.estaEnSuelo()) {
                frameActual = animador.animFall.getKeyFrame(stateTime, false);
                primerFrame = animador.animFall.getKeyFrame(0f);
            } else {
                frameActual = animador.animDead.getKeyFrame(stateTime, false);
                primerFrame = animador.animDead.getKeyFrame(0f);
            }
        } else if (enemigo.getModelo().tiempoHurt > 0) {
            frameActual = animador.animHurt.getKeyFrame(enemigo.getModelo().tiempoHurt, false);
            primerFrame = animador.animHurt.getKeyFrame(0f);
        } else if (isAtacando) {
            frameActual = animador.animPunch.getKeyFrame(stateTime, false);
            primerFrame = animador.animPunch.getKeyFrame(0f);
        } else if (Math.abs(enemigo.getCuerpo().getLinearVelocity().x) > 0.3f) {
            frameActual = animador.animWalk.getKeyFrame(stateTime, true);
            primerFrame = animador.animWalk.getKeyFrame(0f);
        } else {
            frameActual = animador.animIdle.getKeyFrame(stateTime, true);
            primerFrame = animador.animIdle.getKeyFrame(0f);
        }

        // Mantener proporción original para evitar deformación usando primerFrame para el aspect ratio
        float aspect = (float) primerFrame.getRegionWidth() / primerFrame.getRegionHeight();
        float anchoSpriteM = altoSpriteM * aspect;

        // Determinar dirección de mirada
        boolean mirandoDerechaEnemigo = true;
        // Si se está moviendo, usa la dirección de movimiento
        if (Math.abs(enemigo.getCuerpo().getLinearVelocity().x) > 0.1f) {
            mirandoDerechaEnemigo = enemigo.getCuerpo().getLinearVelocity().x > 0;
        } else {
            // Si no se mueve, mira hacia el jugador
            mirandoDerechaEnemigo = entJugador.getCuerpo().getPosition().x > enX;
        }

        // Clonar la región de textura para evitar modificar permanentemente el frame compartido
        TextureRegion drawFrame = new TextureRegion(frameActual);
        if (!mirandoDerechaEnemigo) {
            drawFrame.flip(true, false);
        }

        float dibX = enX - anchoSpriteM / 2f;
        float dibY = enY - (altoCuerpoM / 2f) + offsetY_M;

        batch.draw(drawFrame, dibX, dibY, anchoSpriteM, altoSpriteM);
    }
}