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

public class PantallaJuego implements Screen {

    // Transiciones

    private float tiempoFade = 0f;
    private boolean fadeIn = false;
    private boolean fadeOut = false;
    private static final float DURACION_FADE = 0.5f;

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
    private Arma      armaFisica;

    // ── UI táctil ────────────────────────────────────────────────────────────
    private ControlesTouch controles;

    // ── Render ───────────────────────────────────────────────────────────────
    private Box2DDebugRenderer debugRenderer;
    private ShapeRenderer      shapeRenderer;
    private OrthographicCamera camara;
    private SpriteBatch        batch;
    private Texture            texturaTubo;
    private Texture            texturaCuchillo;

    // Mundo
    private GestorEscenarios   gestorEscenarios;
    private int escenarioOrigen = 0;

    private static final float SUELO_AZOTEA = 1.55f;
    private static final float SUELO_CALLE  = 1f;
    private static final float SUELO_MUELLE = 0f;
    private static final float SUELO_INDUSTRIA = 1f;

    // Animadores
    private AnimadorHeroe      animadorHeroe;
    private AnimadorEnemigoDebil animadorEnemigoDebil;

    // Estados
    private float   stateTime           = 0f;
    private boolean mirandoDerecha      = true;
    private String  estadoAnimAnterior  = "idle";
    private boolean agarrarAnteriorPresionado = false;
    private boolean lanzarAnteriorPresionado  = false;
    private boolean isAtacando          = false;
    private float   tiempoAtacando      = 0f;

    // Sistema de vidas y Game Over
    private int     vidas               = 3;
    private boolean isGameOver          = false;
    private Texture texturaGameOver;
    private com.badlogic.gdx.graphics.g2d.BitmapFont font;
    private float   tiempoGameOver      = 0f;
    private boolean isPlayerDying       = false;
    private float   tiempoMuerteJugador = 0f;
    private boolean isArmaRecogida      = false;

    // Estados para la mecánica de agarre
    private boolean isGrabPunching      = false;
    private float   tiempoGrabPunching  = 0f;
    private boolean isThrowing          = false;
    private float   tiempoThrowing      = 0f;
    private float   tiempoRecibeDanoGrab = 0f;

    // Estados para EnemigoDebil
    private float   stateTimeDebil      = 0f;
    private float   cooldownAtaqueDebil = 0f;
    private float   tiempoAtacandoDebil = 0f;
    private boolean isAtacandoDebil     = false;
    private float   tiempoEnSueloMuerto = 0f;

    // ── Tamaño del viewport en metros (zoom) ─────────────────────────────────
    // Reducir estos valores hace zoom in (personajes más grandes en pantalla).
    // 8 × 4.5 = relación 16:9 con el doble de zoom respecto a 16 × 9.
    private static final float VP_ANCHO = 8f;
    private static final float VP_ALTO  = 4.5f;

    // HUD
    private Texture texturaHUDFondo;
    private Texture texturaHUDBarra;


    @Override
    public void show() {
        // 1. Mundo físico
        mundo   = new MundoFisico();
        fabrica = new FabricaCuerpos(mundo.getWorld());

        // 2. Listener de colisiones
        escucha = new EscuchaColisiones();
        mundo.getWorld().setContactListener(escucha);

        // 3. Modelos
        Jugador      jugador = new Jugador(100, 10);
        EnemigoDebil enD     = new EnemigoDebil();
        EnemigoFuerte enF    = new EnemigoFuerte();

        // 4. Cuerpos Box2D
        Body cJugador = fabrica.crearCuerpoJugador(200, 304, 100, 160, jugador);
        Body cDebil   = fabrica.crearCuerpoEnemigo(500, 304, 100, 160, true,  enD);
        Body cFuerte  = fabrica.crearCuerpoEnemigo(750, 304, 120, 180, false, enF);


        // 5. Entidades físicas
        entJugador       = new EntidadFisica(cJugador, jugador);
        entEnemigoDebil  = new EntidadFisica(cDebil,   enD);
        entEnemigoFuerte = new EntidadFisica(cFuerte,  enF);

        // 6. Arma (Cuchillo en el suelo, jugador inicia desarmado)
        armaFisica = new Cuchillo();
        cuerpoArma = fabrica.crearCuerpoArma(300, 80, 30, 10, armaFisica);

        // 7. Sistema de agarre
        sistemaAgarre = new SistemaAgarre(jugador, cJugador);

        // ── Cámara con zoom ×2 (VP_ANCHO × VP_ALTO metros) ──────────────────
        camara = new OrthographicCamera();
        camara.setToOrtho(false,
                Gdx.graphics.getWidth()  / MundoFisico.PPM,
                Gdx.graphics.getHeight() / MundoFisico.PPM);
        camara.update();

        // 8. Controles
        controles = new ControlesTouch();
        Gdx.input.setInputProcessor(controles);

        // 9. Render
        debugRenderer       = new Box2DDebugRenderer();
        shapeRenderer        = new ShapeRenderer();
        batch                = new SpriteBatch();
        texturaTubo          = new Texture("armas/tubo.png");
        texturaCuchillo      = new Texture("armas/cuchillo.png");
        
        gestorEscenarios     = new GestorEscenarios();
        gestorEscenarios.setCallbackCambioEscenario(() -> {
           //Reposiciona al jugador al cambiar de escenario
           reposicionarJugadorPorEscenario(); 
        });

        animadorHeroe        = new AnimadorHeroe();
        animadorEnemigoDebil = new AnimadorEnemigoDebil(1);

        // HUD
        texturaHUDFondo = new Texture("HUD/salud/salud_fondo.png");
        texturaHUDBarra = new Texture("HUD/salud/salud_barra.png");

        // Game Over y fuentes
        texturaGameOver = new Texture("HUD/gameover.png");
        font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        font.getData().setScale(1.5f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (isGameOver) {
            tiempoGameOver += delta;
            if (Gdx.input.justTouched()) {
                reiniciarJuegoCompleto();
            }
        } else {
            stateTime += delta;
            if (!isPlayerDying) {
                manejarEntradaTactil();
                if (!gestorEscenarios.estaEnTransicion()) {
                    verificarCambioEscenario();
                }
            }
            actualizarEnemigos(delta);
            mundo.actualizar(delta);
            escucha.procesarEventosPendientes();
        }

        // ── Arrastrar enemigo agarrado ───────────────────────────────────────
        if (!isGameOver && sistemaAgarre.tienEnemigoAgarrado()) {
            com.badlogic.gdx.math.Vector2 posJ = entJugador.getCuerpo().getPosition();
            float offsetX = mirandoDerecha ? 0.8f : -0.8f;
            Body cAgarrado = sistemaAgarre.getCuerpoAgarrado();
            if (cAgarrado != null) {
                cAgarrado.setTransform(posJ.x + offsetX, posJ.y, 0f);
                cAgarrado.setLinearVelocity(0, 0);
            }
        }

        // ── Cámara: seguir al jugador con clamp para no salir del escenario ──
        float px = entJugador.getCuerpo().getPosition().x;
        float mitadAncho = (Gdx.graphics.getWidth() / MundoFisico.PPM) / 2f;
        float escenarioAnchoM = 20f;
        camara.position.x = MathUtils.clamp(px, mitadAncho, escenarioAnchoM - mitadAncho);
        // Y fija: centrada en el suelo
        camara.position.y = (Gdx.graphics.getHeight() / MundoFisico.PPM) / 2f;
        camara.update();

        // ── Sprites ──────────────────────────────────────────────────────────
        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        // Escenario de fondo
        float anchoPantallaM = Gdx.graphics.getWidth()  / MundoFisico.PPM;
        float altoPantallaM  = Gdx.graphics.getHeight() / MundoFisico.PPM;
        float camaraIzqX = camara.position.x - anchoPantallaM / 2f;
        float camaraAbajoY = camara.position.y - altoPantallaM / 2f;
        gestorEscenarios.dibujar(batch, camaraIzqX, camaraAbajoY, anchoPantallaM, altoPantallaM);

        // ── Animación del héroe ──────────────────────────────────────────────
        float velX = entJugador.getCuerpo().getLinearVelocity().x;
        float velY = entJugador.getCuerpo().getLinearVelocity().y;

        final float DURACION_GOLPE = 8 * 0.07f;
        if (isAtacando) {
            tiempoAtacando += delta;
            if (tiempoAtacando >= DURACION_GOLPE) {
                isAtacando     = false;
                tiempoAtacando = 0f;
            }
        }

        final float DURACION_GRAB_PUNCH = 0.3f;
        if (isGrabPunching) {
            tiempoGrabPunching += delta;
            if (tiempoGrabPunching >= DURACION_GRAB_PUNCH) {
                isGrabPunching     = false;
                tiempoGrabPunching = 0f;
            }
        }

        final float DURACION_THROW = 0.3f;
        if (isThrowing) {
            tiempoThrowing += delta;
            if (tiempoThrowing >= DURACION_THROW) {
                isThrowing     = false;
                tiempoThrowing = 0f;
            }
        }

        if (tiempoRecibeDanoGrab > 0) {
            tiempoRecibeDanoGrab -= delta;
        }

        String estadoAnim;

        if (isPlayerDying) {
            if (tiempoMuerteJugador < 1.0f && !entJugador.estaEnSuelo()) {
                estadoAnim = "deathFall";
            } else {
                estadoAnim = "dead";
            }
        } else if (entJugador.getModelo().tiempoHurt > 0) {
            estadoAnim = "hurt";
        } else if (isThrowing) {
            estadoAnim = "throw";
        } else if (isGrabPunching) {
            estadoAnim = "grabPunch";
        } else if (sistemaAgarre.tienEnemigoAgarrado()) {
            estadoAnim = "grab";
        } else if (isAtacando) {
            estadoAnim = "punch";
        } else if (velY > 0.5f && animadorHeroe.animJump != null) {
            estadoAnim = "jump";
        } else if (velY < -0.5f && animadorHeroe.animFall != null) {
            estadoAnim = "fall";
        } else if (Math.abs(velX) > 3.5f) {
            estadoAnim = "run";
        } else if (Math.abs(velX) > 0.3f) {
            estadoAnim = "walk";
        } else {
            estadoAnim = "idle";
        }

        if (!estadoAnim.equals(estadoAnimAnterior)) {
            stateTime          = 0f;
            estadoAnimAnterior = estadoAnim;
        }

        TextureRegion frameActual;
        switch (estadoAnim) {
            case "hurt":      frameActual = animadorHeroe.animHurt.getKeyFrame(stateTime, false);   break;
            case "punch":     frameActual = animadorHeroe.animPunch.getKeyFrame(stateTime, false);    break;
            case "jump":      frameActual = animadorHeroe.animJump.getKeyFrame(stateTime, false);     break;
            case "fall":      frameActual = animadorHeroe.animFall.getKeyFrame(stateTime, true);      break;
            case "run":       frameActual = animadorHeroe.animRun.getKeyFrame(stateTime, true);       break;
            case "walk":      frameActual = animadorHeroe.animWalk.getKeyFrame(stateTime, true);      break;
            case "grab":      frameActual = animadorHeroe.animGrab.getKeyFrame(stateTime, true);      break;
            case "grabPunch": frameActual = animadorHeroe.animGrabPunch.getKeyFrame(stateTime, false); break;
            case "throw":     frameActual = animadorHeroe.animThrow.getKeyFrame(stateTime, false);     break;
            case "deathFall": frameActual = animadorHeroe.animDeathFall.getKeyFrame(tiempoMuerteJugador, false); break;
            case "dead":      frameActual = animadorHeroe.animDead.getKeyFrame(tiempoMuerteJugador, false); break;
            default:          frameActual = animadorHeroe.animIdle.getKeyFrame(stateTime, true);      break;
        }

        if (!isAtacando && !isGrabPunching && !isThrowing && !isPlayerDying) {
            if (controles.getDirX() > 0) mirandoDerecha = true;
            if (controles.getDirX() < 0) mirandoDerecha = false;
        }

        // Escala del personaje: ajustada a la normalización de 249 x 240 px
        float altoCanvasM  = 240f / MundoFisico.PPM;
        float anchoCanvasM = 249f / MundoFisico.PPM;

        float posX = entJugador.getCuerpo().getPosition().x;
        float posY = entJugador.getCuerpo().getPosition().y;
        float altoCuerpoFisicoM = 160f / MundoFisico.PPM;
        float piesFisicosY = posY - (altoCuerpoFisicoM / 2f);
        float dibY = piesFisicosY;
        float dibX = posX - (anchoCanvasM / 2f);

        boolean mostrarJugador = true;
        if (isPlayerDying && tiempoMuerteJugador > 1.5f) {
            mostrarJugador = ((int)((tiempoMuerteJugador - 1.5f) * 15)) % 2 == 0;
        }

        if (mostrarJugador) {
            TextureRegion drawFrame = new TextureRegion(frameActual);
            if (!mirandoDerecha) drawFrame.flip(true, false);
            batch.draw(drawFrame, dibX, dibY, anchoCanvasM, altoCanvasM);
        }

        // Enemigo débil
        dibujarEnemigoDebil(batch, entEnemigoDebil, animadorEnemigoDebil, stateTimeDebil, isAtacandoDebil);

        // Arma (solo si no está recogida)
        if (!isArmaRecogida && cuerpoArma != null) {
            float armaX    = cuerpoArma.getPosition().x;
            float armaY    = cuerpoArma.getPosition().y;
            float anguloA  = cuerpoArma.getAngle() * MathUtils.radiansToDegrees;
            float anchoArma = 30f / MundoFisico.PPM;
            float altoArma  = 10f / MundoFisico.PPM;
            Texture texArma = (armaFisica instanceof Cuchillo) ? texturaCuchillo : texturaTubo;
            batch.draw(texArma,
                    armaX - anchoArma / 2f, armaY - altoArma / 2f,
                    anchoArma / 2f, altoArma / 2f,
                    anchoArma, altoArma,
                    1f, 1f, anguloA,
                    0, 0, texArma.getWidth(), texArma.getHeight(),
                    false, false);
        }

        // HUD salud
        dibujarHUD(batch);

        batch.end();

        // Actualizar tiempo de fade
        if (fadeIn || fadeOut) {
            tiempoFade += delta;
        }

        // Dibujar fade si está activo
        if (fadeIn || fadeOut) {
            float alpha = 0f; // inicialización segura
            if (fadeIn) {
                alpha = Math.min(1f, tiempoFade / DURACION_FADE);
                if (tiempoFade >= DURACION_FADE) {
                    fadeIn = false;
                    // Cambiar al escenario destino (esto activa el callback que reposiciona)
                    gestorEscenarios.cambiarEscenario(gestorEscenarios.getEscenarioDestino());
                    // Iniciar fadeOut
                    fadeOut = true;
                    tiempoFade = 0f;
                }
            } else if (fadeOut) {
                alpha = 1f - Math.min(1f, tiempoFade / DURACION_FADE);
                if (tiempoFade >= DURACION_FADE) {
                    fadeOut = false;
                    // Transición completada
                }
            }
            // Dibujar rectángulo negro semitransparente
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, alpha);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
        }

        // ── HUD táctil (coordenadas de pantalla en píxeles) ──────────────────
        shapeRenderer.setProjectionMatrix(
                new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()));
        controles.dibujar(shapeRenderer);

        // ── Debug Box2D ──────────────────────────────────────────────────────
        camara.update();
        debugRenderer.render(mundo.getWorld(), camara.combined);

        // ── GAME OVER OVERLAY ────────────────────────────────────────────────
        if (isGameOver) {
            // 1. Dibujar overlay oscuro
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(
                    new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
                            0, 0,
                            Gdx.graphics.getWidth(),
                            Gdx.graphics.getHeight()));
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.75f);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            // 2. Dibujar imagen de Game Over y texto en coordenadas de pantalla
            batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
                    0, 0,
                    Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight()));
            batch.begin();

            float goAncho = Gdx.graphics.getWidth() * 0.45f;
            float goAlto  = goAncho * ((float) texturaGameOver.getHeight() / texturaGameOver.getWidth());
            float goX     = (Gdx.graphics.getWidth() - goAncho) / 2f;
            float goY     = (Gdx.graphics.getHeight() - goAlto) / 2f + 40f;

            batch.draw(texturaGameOver, goX, goY, goAncho, goAlto);

            // Dibujar texto instructivo con escala dinámica y efecto de pulso (parpadeo tipo "insert coin" retro-arcade)
            float textAlpha = 0.3f + 0.7f * (float) Math.abs(Math.sin(tiempoGameOver * 3.5f));
            font.setColor(1f, 1f, 1f, textAlpha);
            
            float scale = Gdx.graphics.getHeight() / 480f * 0.8f;
            if (scale < 1.0f) scale = 1.0f;
            font.getData().setScale(scale);
            
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, "TOCA LA PANTALLA PARA REINICIAR");
            font.draw(batch, layout, (Gdx.graphics.getWidth() - layout.width) / 2f, goY - 40f);

            batch.end();
        }
    }

    // Dibujar HUD

    private void dibujarHUD(SpriteBatch batch) {
        Jugador jug   = (Jugador) entJugador.getModelo();
        float pct     = (float) jug.getVidaActual() / jug.getVidaMaxima();

        float hudAncho = 4.5f;
        float hudAlto  = hudAncho * ((float) texturaHUDFondo.getHeight() / texturaHUDFondo.getWidth());
        float hudX = camara.position.x - (Gdx.graphics.getWidth() / MundoFisico.PPM) / 2f;
        float hudY = camara.position.y + (Gdx.graphics.getHeight() / MundoFisico.PPM) / 2f - hudAlto;

        // Posición y tamaño de la zona de la barra dentro del HUD usando proporciones exactas de la textura de 1536x1024
        // El espacio vacío donde va la barra empieza en X=403 (fracción 403/1536) y Y=503 (fracción 503/1024)
        // El ancho de la barra es 874px (fracción 874/1536) y el alto es 98px (fracción 98/1024)
        float barraX     = hudX   + hudAncho * (403f / 1536f);
        float barraY     = hudY   + hudAlto  * (503f / 1024f);
        float barraAncho = hudAncho * (874f / 1536f); // ancho total de la barra al 100%
        float barraAlto  = hudAlto  * (98f / 1024f);

        // 1. Dibujar la barra roja escalada según vida (usando TextureRegion para recortar)
        TextureRegion regionBarra = new TextureRegion(texturaHUDBarra,
                0, 0,
                (int)(texturaHUDBarra.getWidth() * pct), // recortar en píxeles según pct
                texturaHUDBarra.getHeight());
        batch.draw(regionBarra, barraX, barraY, barraAncho * pct, barraAlto);

        // 2. Dibujar el marco encima (tapa los bordes de la barra)
        batch.draw(texturaHUDFondo, hudX, hudY, hudAncho, hudAlto);

        // 3. Dibujar las vidas en la esquina superior derecha
        float rightX = camara.position.x + (Gdx.graphics.getWidth() / MundoFisico.PPM) / 2f;
        float topY    = camara.position.y + (Gdx.graphics.getHeight() / MundoFisico.PPM) / 2f;

        TextureRegion regionCara = new TextureRegion(texturaHUDFondo, 141, 350, 262, 259);
        float tamanoVida = 0.45f; // en metros
        float espaciado = 0.5f;
        float vidasX = rightX - 0.2f - tamanoVida;
        float vidasY = topY - 0.2f - tamanoVida;

        for (int i = 0; i < vidas; i++) {
            batch.draw(regionCara, vidasX - (i * espaciado), vidasY, tamanoVida, tamanoVida);
        }
    }

    // ── Entrada táctil ───────────────────────────────────────────────────────

    private void manejarEntradaTactil() {
        if (entJugador.getModelo().tiempoHurt > 0) {
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
            return;
        }

        float dirX = controles.getDirX();
        float limiteDerechoMetros = 19.2f;

        float xJugadorMetros       = entJugador.getCuerpo().getPosition().x;
        float limiteIzquierdoMetros = 0.8f;
        
        if (dirX < 0 && xJugadorMetros <= limiteIzquierdoMetros) {
            dirX = 0;
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
        }

        if (dirX > 0 && xJugadorMetros >= limiteDerechoMetros) {
            dirX = 0;
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
        }

        if (Math.abs(dirX) > 0.001f) {
            float vel    = controles.isCorriendoRapido() ? 6f : 3f;
            float signo  = dirX > 0 ? 1f : -1f;
            entJugador.getCuerpo().setLinearVelocity(signo * vel, entJugador.getCuerpo().getLinearVelocity().y);
        } else {
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
        }

        if (controles.saltarPresionado) entJugador.saltar();

        if (controles.golpePresionado) {
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                if (!isGrabPunching) {
                    isGrabPunching = true;
                    tiempoGrabPunching = 0f;
                    stateTime = 0f;
                    
                    Enemigo objetivo = sistemaAgarre.getEnemigoAgarrado();
                    if (objetivo != null) {
                        Jugador jug = (Jugador) entJugador.getModelo();
                        jug.atacar(objetivo);
                        tiempoRecibeDanoGrab = 0.25f; // Show RecibeDano2 for 0.25 seconds
                        
                        // Si el ataque mató al enemigo, lo empujamos y soltamos
                        if (!objetivo.estaVivo()) {
                            float dirEmpuje = mirandoDerecha ? 5f : -5f;
                            Body cEnemigo = sistemaAgarre.getCuerpoAgarrado();
                            if (cEnemigo != null) {
                                cEnemigo.setLinearVelocity(dirEmpuje, 4f);
                            }
                            sistemaAgarre.soltarAgarre();
                        }
                    }
                }
            } else if (!isAtacando) {
                isAtacando     = true;
                tiempoAtacando = 0f;
                stateTime      = 0f;
                float vx = entJugador.getCuerpo().getLinearVelocity().x;

                Jugador jug      = (Jugador) entJugador.getModelo();
                Enemigo objetivo = enemigoMasCercano();
                if (objetivo != null) {
                    boolean estabaVivo = objetivo.estaVivo();
                    jug.atacar(objetivo);
                    if (estabaVivo && !objetivo.estaVivo()) {
                        float dirEmpuje = mirandoDerecha ? 5f : -5f;
                        Body cEnemigo = null;
                        if (entEnemigoDebil  != null && objetivo == entEnemigoDebil.getModelo())  cEnemigo = entEnemigoDebil.getCuerpo();
                        else if (entEnemigoFuerte != null && objetivo == entEnemigoFuerte.getModelo()) cEnemigo = entEnemigoFuerte.getCuerpo();
                        if (cEnemigo != null) cEnemigo.setLinearVelocity(dirEmpuje, 4f);
                    }
                }
            }
        }

        boolean agarrarAhora = controles.agarrarPresionado;
        if (agarrarAhora && !agarrarAnteriorPresionado) {
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                sistemaAgarre.soltarAgarre();
            } else {
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
                if (masCercano != null && minDist <= RANGO_AGARRE)
                    sistemaAgarre.jugadorIntentaAgarrar((Enemigo) masCercano.getModelo(), masCercano.getCuerpo());
            }
        }
        agarrarAnteriorPresionado = agarrarAhora;

        boolean lanzarAhora = controles.lanzarPresionado;
        if (lanzarAhora && !lanzarAnteriorPresionado) {
            int dir = controles.moviendoIzquierda() ? -1 : 1;
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                isThrowing = true;
                tiempoThrowing = 0f;
                stateTime = 0f;
                Enemigo en = sistemaAgarre.getEnemigoAgarrado();
                if (en != null) {
                    en.setLanzado(true);
                }
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

    private Enemigo enemigoMasCercano() {
        float xJ      = entJugador.getCuerpo().getPosition().x;
        float minDist  = Float.MAX_VALUE;
        Enemigo mejor  = null;
        if (entEnemigoDebil != null && entEnemigoDebil.getModelo().estaVivo()) {
            float d = Math.abs(xJ - entEnemigoDebil.getCuerpo().getPosition().x);
            if (d < minDist) { minDist = d; mejor = (Enemigo) entEnemigoDebil.getModelo(); }
        }
        if (entEnemigoFuerte.getModelo().estaVivo()) {
            float d = Math.abs(xJ - entEnemigoFuerte.getCuerpo().getPosition().x);
            if (d < minDist) { minDist = d; mejor = (Enemigo) entEnemigoFuerte.getModelo(); }
        }
        return mejor;
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Override public void resize(int w, int h) {
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
        texturaHUDFondo.dispose();
        texturaHUDBarra.dispose();
        if (texturaGameOver != null) texturaGameOver.dispose();
        if (font != null) font.dispose();
        if (animadorEnemigoDebil != null) animadorEnemigoDebil.dispose();
    }

    // ── IA enemigos ──────────────────────────────────────────────────────────

    private void actualizarEnemigos(float delta) {
        entJugador.getModelo().actualizar(delta);

        // Detección de recoger arma si el jugador está desarmado y cerca del arma
        Jugador jug = (Jugador) entJugador.getModelo();
        if (!isPlayerDying && !isGameOver && !isArmaRecogida && cuerpoArma != null && jug.getArmaEquipada() == null) {
            float dist = entJugador.getCuerpo().getPosition().dst(cuerpoArma.getPosition());
            if (dist < 1.0f) {
                jug.equiparArma(armaFisica);
                isArmaRecogida = true;
                System.out.println("[Arma] Jugador recoge " + armaFisica.getClass().getSimpleName() + " del suelo.");
            }
        }
        if (!jug.estaVivo() && !isGameOver && !isPlayerDying) {
            isPlayerDying = true;
            tiempoMuerteJugador = 0f;
            if (sistemaAgarre.tienEnemigoAgarrado()) {
                sistemaAgarre.soltarAgarre();
            }
            isAtacando = false;
            isGrabPunching = false;
            isThrowing = false;
        }

        if (isPlayerDying) {
            tiempoMuerteJugador += delta;
            // Detener movimiento horizontal, pero permitir caída libre física
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);
            
            if (tiempoMuerteJugador >= 3.0f) {
                isPlayerDying = false;
                vidas--;
                if (vidas > 0) {
                    jug.revivir(jug.getVidaMaxima());
                    reposicionarJugadorPorEscenario();
                } else {
                    isGameOver = true;
                }
            }
        }

        if (entEnemigoDebil != null) {
            entEnemigoDebil.getModelo().actualizar(delta);
            
            // Si el enemigo estaba lanzado y toca el suelo, deja de estar lanzado
            if (entEnemigoDebil.getModelo().isLanzado() && entEnemigoDebil.estaEnSuelo()) {
                entEnemigoDebil.getModelo().setLanzado(false);
            }
            
            actualizarIA(delta, entEnemigoDebil);

            if (!entEnemigoDebil.getModelo().estaVivo()) {
                tiempoEnSueloMuerto += delta;
                if (tiempoEnSueloMuerto >= 3.0f) {
                    if (sistemaAgarre.getEnemigoAgarrado() == entEnemigoDebil.getModelo())
                        sistemaAgarre.soltarAgarre();
                    mundo.getWorld().destroyBody(entEnemigoDebil.getCuerpo());
                    entEnemigoDebil = null;
                }
            }
        }

        if (entEnemigoFuerte != null) {
            entEnemigoFuerte.getModelo().actualizar(delta);
            // Si el enemigo fuerte estaba lanzado y toca el suelo, deja de estar lanzado
            if (entEnemigoFuerte.getModelo().isLanzado() && entEnemigoFuerte.estaEnSuelo()) {
                entEnemigoFuerte.getModelo().setLanzado(false);
            }
        }
    }

    private void actualizarIA(float delta, EntidadFisica enemigo) {
        if (!enemigo.getModelo().estaVivo()) {
            if (enemigo.estaEnSuelo()) enemigo.detener();
            return;
        }

        if (sistemaAgarre.tienEnemigoAgarrado() && sistemaAgarre.getEnemigoAgarrado() == enemigo.getModelo()) return;

        stateTimeDebil += delta;
        if (cooldownAtaqueDebil > 0) cooldownAtaqueDebil -= delta;
        if (isAtacandoDebil) {
            tiempoAtacandoDebil += delta;
            if (tiempoAtacandoDebil >= 0.3f) { isAtacandoDebil = false; tiempoAtacandoDebil = 0f; }
        }

        if (enemigo.getModelo().tiempoHurt > 0 || isAtacandoDebil) {
            enemigo.detener();
            return;
        }

        float xJ = entJugador.getCuerpo().getPosition().x;
        float xE = enemigo.getCuerpo().getPosition().x;
        float dx = xJ - xE;

        if (Math.abs(dx) > 1.2f) {
            enemigo.mover(Math.signum(dx) * 0.5f);
        } else {
            enemigo.detener();
            float yJ = entJugador.getCuerpo().getPosition().y;
            float yE = enemigo.getCuerpo().getPosition().y;
            float dy = yJ - yE;

            if (entJugador.getModelo().estaVivo() && Math.abs(dy) <= 1.0f && cooldownAtaqueDebil <= 0) {
                isAtacandoDebil     = true;
                tiempoAtacandoDebil = 0f;
                stateTimeDebil      = 0f;
                cooldownAtaqueDebil = 1.5f;
                enemigo.getModelo().atacar(entJugador.getModelo());
            }
        }
    }

    private void dibujarEnemigoDebil(SpriteBatch batch, EntidadFisica enemigo,
                                     AnimadorEnemigoDebil animador,
                                     float stateTime, boolean isAtacando) {
        if (enemigo == null) return;

        if (!enemigo.getModelo().estaVivo() && tiempoEnSueloMuerto > 1.5f) {
            boolean mostrar = ((int)((tiempoEnSueloMuerto - 1.5f) * 15)) % 2 == 0;
            if (!mostrar) return;
        }

        float enX = enemigo.getCuerpo().getPosition().x;
        float enY = enemigo.getCuerpo().getPosition().y;

        TextureRegion frameActual;
        TextureRegion primerFrame;

        // Escala del enemigo débil ajustada a la normalización de 193 x 250 px
        float altoCanvasM  = 250f / MundoFisico.PPM;
        float anchoCanvasM = 193f / MundoFisico.PPM;

        if (!enemigo.getModelo().estaVivo()) {
            if (tiempoEnSueloMuerto < 1.0f && !enemigo.estaEnSuelo()) {
                frameActual = animador.animFall.getKeyFrame(stateTime, false);
                primerFrame  = animador.animFall.getKeyFrame(0f);
            } else {
                frameActual = animador.animDead.getKeyFrame(stateTime, false);
                primerFrame  = animador.animDead.getKeyFrame(0f);
                anchoCanvasM = 225f / MundoFisico.PPM; // Ancho especial del sprite de muerte (normalizado a 225x250)
            }
        } else if (sistemaAgarre.tienEnemigoAgarrado() && sistemaAgarre.getEnemigoAgarrado() == enemigo.getModelo()) {
            // Mientras está en agarre
            if (tiempoRecibeDanoGrab > 0) {
                frameActual = animador.animHurt.getKeyFrame(0.15f, false); // RecibeDano2
            } else {
                frameActual = animador.animHurt.getKeyFrame(0f, false);    // RecibeDano1
            }
            primerFrame = animador.animHurt.getKeyFrame(0f, false);
        } else if (enemigo.getModelo().isLanzado()) {
            // Si está lanzado, usar el sprite Caida
            frameActual = animador.animFall.getKeyFrame(stateTime, false);
            primerFrame  = animador.animFall.getKeyFrame(0f);
        } else if (enemigo.getModelo().tiempoHurt > 0) {
            frameActual = animador.animHurt.getKeyFrame(enemigo.getModelo().tiempoHurt, false);
            primerFrame  = animador.animHurt.getKeyFrame(0f);
        } else if (isAtacando) {
            frameActual = animador.animPunch.getKeyFrame(stateTime, false);
            primerFrame  = animador.animPunch.getKeyFrame(0f);
        } else if (Math.abs(enemigo.getCuerpo().getLinearVelocity().x) > 0.3f) {
            frameActual = animador.animWalk.getKeyFrame(stateTime, true);
            primerFrame  = animador.animWalk.getKeyFrame(0f);
        } else {
            frameActual = animador.animIdle.getKeyFrame(stateTime, true);
            primerFrame  = animador.animIdle.getKeyFrame(0f);
        }

        float altoCuerpoFisicoM = 160f / MundoFisico.PPM;
        float dibX = enX - (anchoCanvasM / 2f);
        float dibY = enY - (altoCuerpoFisicoM / 2f);
            
        boolean mirandoDerechaEnemigo;
        if (Math.abs(enemigo.getCuerpo().getLinearVelocity().x) > 0.1f)
            mirandoDerechaEnemigo = enemigo.getCuerpo().getLinearVelocity().x > 0;
        else
            mirandoDerechaEnemigo = entJugador.getCuerpo().getPosition().x > enX;

        TextureRegion drawFrame = new TextureRegion(frameActual);
        if (!mirandoDerechaEnemigo) drawFrame.flip(true, false);

        batch.draw(drawFrame, dibX, dibY, anchoCanvasM, altoCanvasM);
    }

    private void verificarCambioEscenario() {
        if (fadeIn || fadeOut) return;

        float xJugador = entJugador.getCuerpo().getPosition().x;
        float yJugador = entJugador.getCuerpo().getPosition().y;
        int escenarioActual = gestorEscenarios.getEscenarioActivo();

        final float LIMITE_DERECHO   = 18f;
        final float LIMITE_IZQUIERDO = 2f;
        final float PUNTO_CAIDA      = 2f;

        if (escenarioActual == 0 && (yJugador < PUNTO_CAIDA || xJugador > LIMITE_DERECHO)) {
                iniciarFadeYTransicion(1);
            } else if (escenarioActual == 1 && xJugador > LIMITE_DERECHO) {  
                iniciarFadeYTransicion(2);
            } else if (escenarioActual == 1 && xJugador < LIMITE_IZQUIERDO) { // ← izquierda vuelve a azotea
                iniciarFadeYTransicion(0);
            } else if (escenarioActual == 2 && xJugador > LIMITE_DERECHO) {   // ← NUEVO: derecha pasa a industria
                iniciarFadeYTransicion(3);
            } else if (escenarioActual == 2 && xJugador < LIMITE_IZQUIERDO) { // ← izquierda vuelve a calle
                iniciarFadeYTransicion(1);
            } else if (escenarioActual == 3 && xJugador < LIMITE_IZQUIERDO) { // ← izquierda vuelve a muelle
                iniciarFadeYTransicion(2);
            }
    }

    private void iniciarFadeYTransicion(int nuevoEscenario) {
        escenarioOrigen = gestorEscenarios.getEscenarioActivo();
        fadeIn = true;
        fadeOut = false;
        tiempoFade = 0f;
        gestorEscenarios.setEscenarioDestino(nuevoEscenario); 
    }


    private void reposicionarJugadorPorEscenario() {
        reposicionarJugadorPorEscenario(false);
    }

    private void reposicionarJugadorPorEscenario(boolean respawnSeguro) {
        int escenario = gestorEscenarios.getEscenarioActivo();
        Body cuerpoJugador = entJugador.getCuerpo();

        float ySuelo;
        float xJugador;

        if (escenario == 0) {
            ySuelo    = SUELO_AZOTEA;
            if (respawnSeguro) {
                xJugador = calcularPosicionSeguraSpawn();
            } else if (escenarioOrigen == 1) {
                xJugador = 16f;
            } else {
                xJugador = 2f;
            }
        } else if (escenario == 1) {
            ySuelo    = SUELO_CALLE;
            if (respawnSeguro) {
                xJugador = calcularPosicionSeguraSpawn();
            } else {
                if (escenarioOrigen == 2) {
                    xJugador = 16f; 
                } else {
                    xJugador = 3f;
                }
            }
        } else if (escenario == 2) { 
            ySuelo    = SUELO_MUELLE;
            if (respawnSeguro) {
                xJugador = calcularPosicionSeguraSpawn();
            } else {
                if (escenarioOrigen == 3) {
                    xJugador = 16f;
                } else {
                    xJugador = 3f;
                }
            }
        } else { 
            ySuelo    = SUELO_INDUSTRIA;
            xJugador  = respawnSeguro ? calcularPosicionSeguraSpawn() : 3f;
        }

        mundo.crearSuelo(ySuelo);

        float mitadCuerpo = 160f / MundoFisico.PPM / 2f;
        cuerpoJugador.setTransform(xJugador, ySuelo + mitadCuerpo, 0);
        cuerpoJugador.setLinearVelocity(0, 0);
        cuerpoJugador.setAngularVelocity(0);

        camara.position.x = cuerpoJugador.getPosition().x;
        camara.update();
    }

    private float calcularPosicionSeguraSpawn() {
        int escenario = gestorEscenarios.getEscenarioActivo();
        float xMin = 3.0f;
        float xMax = 16.0f;
        
        java.util.List<Float> enemigosX = new java.util.ArrayList<>();
        if (entEnemigoDebil != null && entEnemigoDebil.getModelo().estaVivo()) {
            enemigosX.add(entEnemigoDebil.getCuerpo().getPosition().x);
        }
        if (entEnemigoFuerte != null && entEnemigoFuerte.getModelo().estaVivo()) {
            enemigosX.add(entEnemigoFuerte.getCuerpo().getPosition().x);
        }
        
        if (enemigosX.isEmpty()) {
            if (escenario == 0) return 16f;
            return 3f;
        }
        
        float mejorX = -1f;
        float maxDistMinima = -1f;
        
        for (float x = xMin; x <= xMax; x += 0.5f) {
            float distMinima = Float.MAX_VALUE;
            for (float ex : enemigosX) {
                float d = Math.abs(x - ex);
                if (d < distMinima) {
                    distMinima = d;
                }
            }
            if (distMinima > maxDistMinima) {
                maxDistMinima = distMinima;
                mejorX = x;
            }
        }
        
        if (maxDistMinima < 3.0f) {
            System.out.println("[Spawn] No hay lugar seguro (todos a menos de 3.0m). Usando posición aleatoria.");
            return xMin + (float) Math.random() * (xMax - xMin);
        }
        
        System.out.println("[Spawn] Posición segura óptima X = " + mejorX + " (distancia mínima de enemigos: " + maxDistMinima + "m).");
        return mejorX;
    }

    private void reiniciarJuegoCompleto() {
        vidas = 3;
        isGameOver = false;
        tiempoGameOver = 0f;
        isPlayerDying = false;
        tiempoMuerteJugador = 0f;
        escucha.limpiarEventos();

        Jugador jug = (Jugador) entJugador.getModelo();
        jug.revivir(jug.getVidaMaxima());

        if (sistemaAgarre.tienEnemigoAgarrado()) {
            sistemaAgarre.soltarAgarre();
        }
        isAtacando = false;
        isGrabPunching = false;
        isThrowing = false;

        escenarioOrigen = 0;
        isArmaRecogida = false;

        gestorEscenarios.cambiarEscenario(0); // Volver al primer escenario (azotea)
        reposicionarJugadorPorEscenario();

        reiniciarEnemigos();

        // Reiniciar arma (Cuchillo en el suelo, jugador desarmado)
        if (cuerpoArma != null) {
            mundo.getWorld().destroyBody(cuerpoArma);
        }
        armaFisica = new Cuchillo();
        jug.equiparArma(null);
        cuerpoArma = fabrica.crearCuerpoArma(300, 80, 30, 10, armaFisica);
    }

    private void reiniciarEnemigos() {
        // Eliminar cuerpos viejos si todavía existen
        if (entEnemigoDebil != null) {
            mundo.getWorld().destroyBody(entEnemigoDebil.getCuerpo());
            entEnemigoDebil = null;
        }
        if (entEnemigoFuerte != null) {
            mundo.getWorld().destroyBody(entEnemigoFuerte.getCuerpo());
            entEnemigoFuerte = null;
        }

        // Recrear enemigo débil
        EnemigoDebil enD = new EnemigoDebil();
        Body cDebil = fabrica.crearCuerpoEnemigo(500, 304, 100, 160, true, enD);
        entEnemigoDebil = new EntidadFisica(cDebil, enD);
        tiempoEnSueloMuerto = 0f;

        // Recrear enemigo fuerte
        EnemigoFuerte enF = new EnemigoFuerte();
        Body cFuerte = fabrica.crearCuerpoEnemigo(750, 304, 120, 180, false, enF);
        entEnemigoFuerte = new EntidadFisica(cFuerte, enF);
    }
}