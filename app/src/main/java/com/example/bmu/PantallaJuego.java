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
import com.example.bmu.controladores.GestorEnemigos;
import com.example.bmu.controladores.ControladorJugador;
import com.example.bmu.vista.RenderizadorHUD;

public class PantallaJuego implements Screen {

    private BMUGame juego;
    private ScreenshotHandler screenshotHandler;
    private com.badlogic.gdx.math.Rectangle btnScreenshot;
    private boolean menuPausaVisible = false;
    private com.badlogic.gdx.math.Rectangle btnMenuScreenshot;
    private com.badlogic.gdx.math.Rectangle btnMenuPrincipal;
    private boolean tomarScreenshotPendiente = false;

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

    // ── Entidades y Controladores ────────────────────────────────────────────
    private EntidadFisica      entJugador;
    private Body               cuerpoArma;
    private Arma               armaFisica;
    private ControladorJugador controladorJugador;
    private GestorEnemigos     gestorEnemigos;

    // ── UI táctil y HUD ──────────────────────────────────────────────────────
    private ControlesTouch     controles;
    private RenderizadorHUD    renderizadorHUD;
    private com.badlogic.gdx.graphics.g2d.BitmapFont font;

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

    private static final float SUELO_AZOTEA    = 1.55f;
    private static final float SUELO_CALLE     = 1f;
    private static final float SUELO_MUELLE    = 0f;
    private static final float SUELO_INDUSTRIA = 1f;

    // Animadores del Héroe
    private AnimadorHeroe      animadorHeroe;
    private String             estadoAnimAnterior = "idle";

    // Sistema de vidas, Game Over, Victoria
    private int     vidas               = 5;
    private int     escenarioInicial    = 0;
    private boolean isVictoria          = false;
    private float   tiempoVictoria      = 0f;
    private boolean isGameOver          = false;
    private float   tiempoGameOver      = 0f;
    private boolean isPlayerDying       = false;
    private float   tiempoMuerteJugador = 0f;

    private static final float VP_ANCHO = 8f;
    private static final float VP_ALTO  = 4.5f;

    // HUD
    private Texture texturaHUDFondo;
    private Texture texturaHUDBarra;

    // ── Constructor ───────────────────────────────────────────────────────────
    public PantallaJuego(BMUGame juego, int escenarioInicial, int vidasIniciales) {
        this.juego            = juego;
        this.screenshotHandler = juego.screenshotHandler;
        this.escenarioInicial  = escenarioInicial;
        this.vidas             = vidasIniciales;
    }

    @Override
    public void show() {
        float btnW = 80f, btnH = 60f;
        btnScreenshot = new com.badlogic.gdx.math.Rectangle(
            (Gdx.graphics.getWidth() - btnW) / 2f, 
            Gdx.graphics.getHeight() - btnH - 10, 
            btnW, btnH);

        // 1. Mundo físico
        mundo   = new MundoFisico();
        fabrica = new FabricaCuerpos(mundo.getWorld());

        // 2. Listener de colisiones
        escucha = new EscuchaColisiones();
        mundo.getWorld().setContactListener(escucha);

        // 3. Modelos y Cuerpos
        Jugador jugador = new Jugador(350, 20);
        Body cJugador = fabrica.crearCuerpoJugador(200, 304, 100, 160, jugador);
        entJugador = new EntidadFisica(cJugador, jugador);

        // 4. Arma (Cuchillo en el suelo, jugador inicia desarmado)
        armaFisica = new Cuchillo();
        cuerpoArma = fabrica.crearCuerpoArma(300, 80, 30, 10, armaFisica);

        // 5. Sistema de agarre
        sistemaAgarre = new SistemaAgarre(jugador, cJugador);

        // 6. Cámara
        camara = new OrthographicCamera();
        camara.setToOrtho(false,
                Gdx.graphics.getWidth()  / MundoFisico.PPM,
                Gdx.graphics.getHeight() / MundoFisico.PPM);
        camara.update();

        // 7. Controles
        controles = new ControlesTouch();
        Gdx.input.setInputProcessor(controles);

        // 8. Render
        debugRenderer  = new Box2DDebugRenderer();
        shapeRenderer  = new ShapeRenderer();
        batch          = new SpriteBatch();
        texturaTubo    = new Texture("armas/tubo.png");
        texturaCuchillo = new Texture("armas/cuchillo.png");

        gestorEscenarios = new GestorEscenarios();
        gestorEscenarios.setCallbackCambioEscenario(() -> {
            reposicionarJugadorPorEscenario();
        });

        animadorHeroe = new AnimadorHeroe();

        // 9. Controladores y gestores
        gestorEnemigos     = new GestorEnemigos(mundo, fabrica);
        controladorJugador = new ControladorJugador(entJugador, controles, sistemaAgarre, gestorEnemigos);
        renderizadorHUD    = new RenderizadorHUD();

        font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        font.getData().setScale(1.5f);

        // 10. Iniciar en el escenario guardado (o 0 si es nueva partida)
        gestorEscenarios.cambiarEscenario(escenarioInicial);
        float ySueloInicial;
        switch (escenarioInicial) {
            case 1:  ySueloInicial = SUELO_CALLE;     break;
            case 2:  ySueloInicial = SUELO_MUELLE;    break;
            case 3:  ySueloInicial = SUELO_INDUSTRIA; break;
            default: ySueloInicial = SUELO_AZOTEA;    break;
        }
        mundo.crearSuelo(ySueloInicial);
        gestorEnemigos.configurarEnemigosParaEscenario(escenarioInicial, ySueloInicial);
        float mitadCuerpoInicial = 160f / MundoFisico.PPM / 2f;
        entJugador.getCuerpo().setTransform(3f, ySueloInicial + mitadCuerpoInicial, 0);
        entJugador.getCuerpo().setLinearVelocity(0, 0);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (isGameOver) {
            tiempoGameOver += delta;
            if (Gdx.input.justTouched() && tiempoGameOver > 1.5f) {
                juego.setScreen(new PantallaMenu(juego));
                dispose();
                return;
            }
        } else if (!menuPausaVisible) {
            controladorJugador.stateTime += delta;
            if (!isPlayerDying) {
                controladorJugador.manejarEntradaTactil(cuerpoArma);
                if (!gestorEscenarios.estaEnTransicion()) {
                    verificarCambioEscenario();
                }
            }

            actualizarLogicaJugador(delta);
            gestorEnemigos.actualizar(delta, entJugador, sistemaAgarre);
            if (!isVictoria && gestorEnemigos.getEntJefe3() == null && gestorEscenarios.getEscenarioActivo() == 3) {
                isVictoria = true;
            }
            mundo.actualizar(delta);
            escucha.procesarEventosPendientes();
        }

        // ── Arrastrar enemigo agarrado ───────────────────────────────────────
        if (!isGameOver && !menuPausaVisible && sistemaAgarre.tienEnemigoAgarrado()) {
            com.badlogic.gdx.math.Vector2 posJ = entJugador.getCuerpo().getPosition();
            float offsetX = controladorJugador.mirandoDerecha ? 0.8f : -0.8f;
            Body cAgarrado = sistemaAgarre.getCuerpoAgarrado();
            if (cAgarrado != null) {
                cAgarrado.setTransform(posJ.x + offsetX, posJ.y, 0f);
                cAgarrado.setLinearVelocity(0, 0);
            }
        }

        if (isVictoria) {
            tiempoVictoria += delta;
            if (Gdx.input.justTouched() && tiempoVictoria > 1.5f) {
                juego.setScreen(new PantallaMenu(juego));
                dispose();
                return;
            }
        }

        // ── Cámara ───────────────────────────────────────────────────────────
        float px = entJugador.getCuerpo().getPosition().x;
        float mitadAncho = (Gdx.graphics.getWidth() / MundoFisico.PPM) / 2f;
        float escenarioAnchoM = 20f;
        camara.position.x = MathUtils.clamp(px, mitadAncho, escenarioAnchoM - mitadAncho);
        camara.position.y = (Gdx.graphics.getHeight() / MundoFisico.PPM) / 2f;
        camara.update();

        // ── Sprites ──────────────────────────────────────────────────────────
        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        float anchoPantallaM = Gdx.graphics.getWidth()  / MundoFisico.PPM;
        float altoPantallaM  = Gdx.graphics.getHeight() / MundoFisico.PPM;
        float camaraIzqX   = camara.position.x - anchoPantallaM / 2f;
        float camaraAbajoY = camara.position.y - altoPantallaM  / 2f;
        gestorEscenarios.dibujar(batch, camaraIzqX, camaraAbajoY, anchoPantallaM, altoPantallaM);

        // ── Animación del héroe ──────────────────────────────────────────────
        float velX = entJugador.getCuerpo().getLinearVelocity().x;
        float velY = entJugador.getCuerpo().getLinearVelocity().y;

        final float DURACION_GOLPE = 8 * 0.07f;
        if (controladorJugador.isAtacando) {
            controladorJugador.tiempoAtacando += delta;
            if (controladorJugador.tiempoAtacando >= DURACION_GOLPE) {
                controladorJugador.isAtacando    = false;
                controladorJugador.tiempoAtacando = 0f;
            }
        }

        final float DURACION_GRAB_PUNCH = 0.3f;
        if (controladorJugador.isGrabPunching) {
            controladorJugador.tiempoGrabPunching += delta;
            if (controladorJugador.tiempoGrabPunching >= DURACION_GRAB_PUNCH) {
                controladorJugador.isGrabPunching    = false;
                controladorJugador.tiempoGrabPunching = 0f;
            }
        }

        final float DURACION_THROW = 0.3f;
        if (controladorJugador.isThrowing) {
            controladorJugador.tiempoThrowing += delta;
            if (controladorJugador.tiempoThrowing >= DURACION_THROW) {
                controladorJugador.isThrowing    = false;
                controladorJugador.tiempoThrowing = 0f;
            }
        }

        if (controladorJugador.tiempoRecibeDanoGrab > 0) {
            controladorJugador.tiempoRecibeDanoGrab -= delta;
        }

        String estadoAnim;
        if (isPlayerDying) {
            estadoAnim = (tiempoMuerteJugador < 1.0f && !entJugador.estaEnSuelo()) ? "deathFall" : "dead";
        } else if (entJugador.getModelo().tiempoHurt > 0) {
            estadoAnim = "hurt";
        } else if (controladorJugador.isThrowing) {
            estadoAnim = "throw";
        } else if (controladorJugador.isGrabPunching) {
            estadoAnim = "grabPunch";
        } else if (sistemaAgarre.tienEnemigoAgarrado()) {
            estadoAnim = "grab";
        } else if (controladorJugador.isAtacando) {
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
            controladorJugador.stateTime = 0f;
            estadoAnimAnterior = estadoAnim;
        }

        TextureRegion frameActual;
        switch (estadoAnim) {
            case "hurt":      frameActual = animadorHeroe.animHurt.getKeyFrame(controladorJugador.stateTime, false);    break;
            case "punch":     frameActual = animadorHeroe.animPunch.getKeyFrame(controladorJugador.stateTime, false);   break;
            case "jump":      frameActual = animadorHeroe.animJump.getKeyFrame(controladorJugador.stateTime, false);    break;
            case "fall":      frameActual = animadorHeroe.animFall.getKeyFrame(controladorJugador.stateTime, true);     break;
            case "run":       frameActual = animadorHeroe.animRun.getKeyFrame(controladorJugador.stateTime, true);      break;
            case "walk":      frameActual = animadorHeroe.animWalk.getKeyFrame(controladorJugador.stateTime, true);     break;
            case "grab":      frameActual = animadorHeroe.animGrab.getKeyFrame(controladorJugador.stateTime, true);     break;
            case "grabPunch": frameActual = animadorHeroe.animGrabPunch.getKeyFrame(controladorJugador.stateTime, false); break;
            case "throw":     frameActual = animadorHeroe.animThrow.getKeyFrame(controladorJugador.stateTime, false);   break;
            case "deathFall": frameActual = animadorHeroe.animDeathFall.getKeyFrame(tiempoMuerteJugador, false);        break;
            case "dead":      frameActual = animadorHeroe.animDead.getKeyFrame(tiempoMuerteJugador, false);             break;
            default:          frameActual = animadorHeroe.animIdle.getKeyFrame(controladorJugador.stateTime, true);     break;
        }

        if (!controladorJugador.isAtacando && !controladorJugador.isGrabPunching
                && !controladorJugador.isThrowing && !isPlayerDying) {
            if (controles.getDirX() > 0) controladorJugador.mirandoDerecha = true;
            if (controles.getDirX() < 0) controladorJugador.mirandoDerecha = false;
        }

        float altoCanvasM  = 240f / MundoFisico.PPM;
        float anchoCanvasM = 249f / MundoFisico.PPM;

        float posX = entJugador.getCuerpo().getPosition().x;
        float posY = entJugador.getCuerpo().getPosition().y;
        float altoCuerpoFisicoM = 160f / MundoFisico.PPM;
        float dibY = posY - (altoCuerpoFisicoM / 2f);
        float dibX = posX - (anchoCanvasM / 2f);

        boolean mostrarJugador = true;
        if (isPlayerDying && tiempoMuerteJugador > 1.5f) {
            mostrarJugador = ((int)((tiempoMuerteJugador - 1.5f) * 15)) % 2 == 0;
        }

        if (mostrarJugador) {
            TextureRegion drawFrame = new TextureRegion(frameActual);
            if (!controladorJugador.mirandoDerecha) drawFrame.flip(true, false);
            batch.draw(drawFrame, dibX, dibY, anchoCanvasM, altoCanvasM);
        }

        gestorEnemigos.dibujar(batch, entJugador, sistemaAgarre, controladorJugador.tiempoRecibeDanoGrab);

        // Arma (solo si no está recogida)
        if (!controladorJugador.isArmaRecogida && cuerpoArma != null) {
            float armaX   = cuerpoArma.getPosition().x;
            float armaY   = cuerpoArma.getPosition().y;
            float anguloA = cuerpoArma.getAngle() * MathUtils.radiansToDegrees;
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

        batch.end();

        // ── Fade ─────────────────────────────────────────────────────────────
        if (fadeIn || fadeOut) {
            tiempoFade += delta;
            float alpha = 0f;
            if (fadeIn) {
                alpha = Math.min(1f, tiempoFade / DURACION_FADE);
                if (tiempoFade >= DURACION_FADE) {
                    fadeIn = false;
                    gestorEscenarios.cambiarEscenario(gestorEscenarios.getEscenarioDestino());
                    fadeOut    = true;
                    tiempoFade = 0f;
                }
            } else {
                alpha = 1f - Math.min(1f, tiempoFade / DURACION_FADE);
                if (tiempoFade >= DURACION_FADE) fadeOut = false;
            }
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4()
                .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, alpha);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // ── HUD táctil + botón screenshot (misma proyección) ─────────────────
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4()
            .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        controles.dibujar(shapeRenderer);

        // ── Botón pausa (centro-arriba) ──────────────────────────────────────
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Calcular posición del menú desplegable
        float menuW = 400f, menuH = 220f;
        float menuX = (sw - menuW) / 2f;
        float menuY = btnScreenshot.y - menuH - 10f;
        btnMenuScreenshot = new com.badlogic.gdx.math.Rectangle(menuX + 10, menuY + 115f, menuW - 20, 95f);
        btnMenuPrincipal  = new com.badlogic.gdx.math.Rectangle(menuX + 10, menuY + 10f,  menuW - 20, 95f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Botón pausa
        shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 0.85f);
        shapeRenderer.rect(btnScreenshot.x, btnScreenshot.y, btnScreenshot.width, btnScreenshot.height);

        // Menú desplegable
        if (menuPausaVisible) {
            shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.92f);
            shapeRenderer.rect(menuX, menuY, menuW, menuH);
            shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 0.95f);
            shapeRenderer.rect(btnMenuScreenshot.x, btnMenuScreenshot.y, btnMenuScreenshot.width, btnMenuScreenshot.height);
            shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 0.95f);
            shapeRenderer.rect(btnMenuPrincipal.x, btnMenuPrincipal.y, btnMenuPrincipal.width, btnMenuPrincipal.height);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Texto del botón pausa y menú
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4()
            .setToOrtho2D(0, 0, sw, sh));
        batch.begin();
        font.getData().setScale(1.3f);
        font.setColor(1, 1, 1, 1);
        // Centrar "II" en el botón pausa
        com.badlogic.gdx.graphics.g2d.GlyphLayout gl = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, "II");
        font.draw(batch, "II",
            btnScreenshot.x + (btnScreenshot.width  - gl.width)  / 2f,
            btnScreenshot.y + (btnScreenshot.height + gl.height) / 2f);

        if (menuPausaVisible) {
            font.getData().setScale(1.5f);
            font.setColor(0.9f, 0.9f, 0.9f, 1f);
            com.badlogic.gdx.graphics.g2d.GlyphLayout glS = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, "Captura de pantalla");
            font.draw(batch, "Captura de pantalla",
                btnMenuScreenshot.x + (btnMenuScreenshot.width  - glS.width)  / 2f,
                btnMenuScreenshot.y + (btnMenuScreenshot.height + glS.height) / 2f);

            com.badlogic.gdx.graphics.g2d.GlyphLayout glM = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, "Menu principal");
            font.draw(batch, "Menu principal",
                btnMenuPrincipal.x + (btnMenuPrincipal.width  - glM.width)  / 2f,
                btnMenuPrincipal.y + (btnMenuPrincipal.height + glM.height) / 2f);
        }
        font.getData().setScale(1.5f);
        batch.end();

        // Toques del menú
        if (Gdx.input.justTouched()) {
            float tx = Gdx.input.getX();
            float ty = sh - Gdx.input.getY();

            if (btnScreenshot.contains(tx, ty)) {
                menuPausaVisible = !menuPausaVisible;
            } else if (menuPausaVisible) {
                if (btnMenuScreenshot.contains(tx, ty)) {
                    menuPausaVisible = false;
                    tomarScreenshotPendiente = true;
                } else if (btnMenuPrincipal.contains(tx, ty)) {
                    menuPausaVisible = false;
                    juego.setScreen(new PantallaMenu(juego));
                    dispose();
                    return;
                } else {
                    menuPausaVisible = false; // toque fuera cierra el menú
                }
            }
        }

        // Screenshot se toma el frame siguiente con menú ya oculto
        if (tomarScreenshotPendiente) {
            tomarScreenshotPendiente = false;
            if (screenshotHandler != null) screenshotHandler.tomarYCompartirScreenshot();
        }

        // ── HUD salud / game over / victoria ─────────────────────────────────
        camara.update();
        Jugador jug = (Jugador) entJugador.getModelo();
        renderizadorHUD.dibujarHUD(batch, shapeRenderer, font, camara,
                jug, vidas, isGameOver, tiempoGameOver, isVictoria);
    }

    // ── Lógica del jugador ────────────────────────────────────────────────────

    private void actualizarLogicaJugador(float delta) {
        entJugador.getModelo().actualizar(delta);

        Jugador jug = (Jugador) entJugador.getModelo();
        if (!isPlayerDying && !isGameOver && !controladorJugador.isArmaRecogida
                && cuerpoArma != null && jug.getArmaEquipada() == null) {
            float dist = entJugador.getCuerpo().getPosition().dst(cuerpoArma.getPosition());
            if (dist < 1.0f) {
                jug.equiparArma(armaFisica);
                controladorJugador.isArmaRecogida = true;
                System.out.println("[Arma] Jugador recoge " + armaFisica.getClass().getSimpleName());
            }
        }

        if (!jug.estaVivo() && !isGameOver && !isPlayerDying) {
            isPlayerDying       = true;
            tiempoMuerteJugador = 0f;
            if (sistemaAgarre.tienEnemigoAgarrado()) sistemaAgarre.soltarAgarre();
            controladorJugador.reset();
        }

        if (isPlayerDying) {
            tiempoMuerteJugador += delta;
            entJugador.getCuerpo().setLinearVelocity(0, entJugador.getCuerpo().getLinearVelocity().y);

            if (tiempoMuerteJugador >= 3.0f) {
                isPlayerDying = false;
                vidas--;
                if (vidas > 0) {
                    jug.revivir(jug.getVidaMaxima());
                    reposicionarJugadorPorEscenario(true);
                } else {
                    isGameOver = true;
                }
            }
        }
    }

    // ── Cambio de escenario ───────────────────────────────────────────────────

    private void verificarCambioEscenario() {
        if (fadeIn || fadeOut) return;
        if (!gestorEnemigos.todosEnemigosNormalesMuertos()) return;

        float xJugador     = entJugador.getCuerpo().getPosition().x;
        float yJugador     = entJugador.getCuerpo().getPosition().y;
        int   escenarioActual = gestorEscenarios.getEscenarioActivo();

        final float LIMITE_DERECHO   = 18f;
        final float LIMITE_IZQUIERDO = 2f;
        final float PUNTO_CAIDA      = 2f;

        if      (escenarioActual == 0 && (yJugador < PUNTO_CAIDA || xJugador > LIMITE_DERECHO)) iniciarFadeYTransicion(1);
        else if (escenarioActual == 1 && xJugador > LIMITE_DERECHO)  iniciarFadeYTransicion(2);
        else if (escenarioActual == 1 && xJugador < LIMITE_IZQUIERDO) iniciarFadeYTransicion(0);
        else if (escenarioActual == 2 && xJugador > LIMITE_DERECHO)  iniciarFadeYTransicion(3);
        else if (escenarioActual == 2 && xJugador < LIMITE_IZQUIERDO) iniciarFadeYTransicion(1);
        else if (escenarioActual == 3 && xJugador < LIMITE_IZQUIERDO) iniciarFadeYTransicion(2);
    }

    private void iniciarFadeYTransicion(int nuevoEscenario) {
        escenarioOrigen = gestorEscenarios.getEscenarioActivo();
        fadeIn     = true;
        fadeOut    = false;
        tiempoFade = 0f;
        gestorEscenarios.setEscenarioDestino(nuevoEscenario);

        // Guardar progreso solo al avanzar
        if (nuevoEscenario > escenarioOrigen) {
            Gdx.app.getPreferences("bmu_save")
                .putBoolean("tiene_guardado", true)
                .putInteger("escenario", nuevoEscenario)
                .putInteger("vidas", vidas)
                .flush();
            System.out.println("[Save] Progreso guardado: escenario " + nuevoEscenario);
        }
    }

    // ── Reposicionamiento ─────────────────────────────────────────────────────

    private void reposicionarJugadorPorEscenario() {
        reposicionarJugadorPorEscenario(false);
    }

    private void reposicionarJugadorPorEscenario(boolean respawnSeguro) {
        int  escenario    = gestorEscenarios.getEscenarioActivo();
        Body cuerpoJugador = entJugador.getCuerpo();

        float ySuelo;
        float xJugador;

        if (escenario == 0) {
            ySuelo = SUELO_AZOTEA;
            xJugador = respawnSeguro ? gestorEnemigos.calcularPosicionSeguraSpawn(escenario)
                     : (escenarioOrigen == 1 ? 16f : 2f);
            desactivarJefe(1); desactivarJefe(2); desactivarJefe(3);

        } else if (escenario == 1) {
            ySuelo = SUELO_CALLE;
            xJugador = respawnSeguro ? gestorEnemigos.calcularPosicionSeguraSpawn(escenario)
                     : (escenarioOrigen == 2 ? 16f : 3f);
            activarJefeSiVivo(1, ySuelo);
            desactivarJefe(2); desactivarJefe(3);

        } else if (escenario == 2) {
            ySuelo = SUELO_MUELLE;
            xJugador = respawnSeguro ? gestorEnemigos.calcularPosicionSeguraSpawn(escenario)
                     : (escenarioOrigen == 3 ? 16f : 3f);
            desactivarJefe(1);
            activarJefeSiVivo(2, ySuelo);
            desactivarJefe(3);

        } else {
            ySuelo   = SUELO_INDUSTRIA;
            xJugador = respawnSeguro ? gestorEnemigos.calcularPosicionSeguraSpawn(escenario) : 3f;
            desactivarJefe(1); desactivarJefe(2);
            activarJefeSiVivo(3, ySuelo);
        }

        mundo.crearSuelo(ySuelo);
        if (!respawnSeguro) {
            gestorEnemigos.configurarEnemigosParaEscenario(escenario, ySuelo);
        }

        float mitadCuerpo = 160f / MundoFisico.PPM / 2f;
        cuerpoJugador.setTransform(xJugador, ySuelo + mitadCuerpo, 0);
        cuerpoJugador.setLinearVelocity(0, 0);
        cuerpoJugador.setAngularVelocity(0);

        camara.position.x = cuerpoJugador.getPosition().x;
        camara.update();
    }

    private void desactivarJefe(int idx) {
        EntidadFisica ent = idx == 1 ? gestorEnemigos.getEntJefe1()
                          : idx == 2 ? gestorEnemigos.getEntJefe2()
                          :            gestorEnemigos.getEntJefe3();
        boolean activado  = idx == 1 ? gestorEnemigos.isJefe1Activado()
                          : idx == 2 ? gestorEnemigos.isJefe2Activado()
                          :            gestorEnemigos.isJefe3Activado();
        if (ent != null && activado) {
            ent.getCuerpo().setActive(false);
            ent.getCuerpo().setTransform(1000f, 0, 0);
            if (idx == 1) gestorEnemigos.setJefe1Activado(false);
            else if (idx == 2) gestorEnemigos.setJefe2Activado(false);
            else gestorEnemigos.setJefe3Activado(false);
        }
    }

    private void activarJefeSiVivo(int idx, float ySuelo) {
        EntidadFisica ent = idx == 1 ? gestorEnemigos.getEntJefe1()
                          : idx == 2 ? gestorEnemigos.getEntJefe2()
                          :            gestorEnemigos.getEntJefe3();
        boolean activado  = idx == 1 ? gestorEnemigos.isJefe1Activado()
                          : idx == 2 ? gestorEnemigos.isJefe2Activado()
                          :            gestorEnemigos.isJefe3Activado();
        if (ent != null && ent.getModelo().estaVivo() && !activado) {
            ent.getCuerpo().setTransform(15f, ySuelo + (200f / MundoFisico.PPM) / 2f, 0);
            ent.getCuerpo().setActive(true);
            if (idx == 1) gestorEnemigos.setJefe1Activado(true);
            else if (idx == 2) gestorEnemigos.setJefe2Activado(true);
            else gestorEnemigos.setJefe3Activado(true);
        }
    }

    // ── Reinicio completo ─────────────────────────────────────────────────────

    private void reiniciarJuegoCompleto() {
        vidas               = 5;
        isGameOver          = false;
        tiempoGameOver      = 0f;
        isPlayerDying       = false;
        tiempoMuerteJugador = 0f;
        isVictoria          = false;
        tiempoVictoria      = 0f;
        escucha.limpiarEventos();

        Jugador jug = (Jugador) entJugador.getModelo();
        jug.revivir(jug.getVidaMaxima());
        if (sistemaAgarre.tienEnemigoAgarrado()) sistemaAgarre.soltarAgarre();
        controladorJugador.reset();

        escenarioOrigen = 0;
        gestorEscenarios.cambiarEscenario(0);
        gestorEnemigos.reiniciarEnemigos();
        gestorEnemigos.configurarEnemigosParaEscenario(0, SUELO_AZOTEA);
        reposicionarJugadorPorEscenario(false);

        if (cuerpoArma != null) mundo.getWorld().destroyBody(cuerpoArma);
        armaFisica = new Cuchillo();
        jug.equiparArma(null);
        cuerpoArma = fabrica.crearCuerpoArma(300, 80, 30, 10, armaFisica);
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override public void resize(int w, int h) {
        camara.setToOrtho(false, w / MundoFisico.PPM, h / MundoFisico.PPM);
    }
    @Override public void pause()  { GestorAudio.getInstance().pausar(); }
    @Override public void resume() { GestorAudio.getInstance().reanudar(); }
    @Override public void hide()   { GestorAudio.getInstance().pausar(); }

    @Override
    public void dispose() {
        mundo.dispose();
        if (debugRenderer  != null) debugRenderer.dispose();
        shapeRenderer.dispose();
        batch.dispose();
        if (texturaTubo     != null) texturaTubo.dispose();
        if (texturaCuchillo != null) texturaCuchillo.dispose();
        gestorEscenarios.dispose();
        animadorHeroe.dispose();
        if (font != null) font.dispose();
        gestorEnemigos.dispose();
        renderizadorHUD.dispose();
    }
}