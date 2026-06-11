package com.example.bmu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;

public class PantallaMenu implements Screen {

    private BMUGame juego;

    private SpriteBatch   batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont    font;
    private OrthographicCamera camara;

    private Rectangle btnNuevaPartida;
    private Rectangle btnContinuar;

    private float   tiempoMenu     = 0f;
    private float   pulsoNueva     = 0f;
    private float   pulsoContinuar = 0f;
    private boolean tienePartidaGuardada = false;

    // ── Acción pendiente para ejecutar DESPUÉS del frame ─────────────────────
    private enum Accion { NINGUNA, NUEVA_PARTIDA, CONTINUAR }
    private Accion accionPendiente = Accion.NINGUNA;

    private static final Color COLOR_FONDO     = new Color(0.06f, 0.06f, 0.10f, 1f);
    private static final Color COLOR_BOTON     = new Color(0.15f, 0.08f, 0.08f, 1f);
    private static final Color COLOR_BOTON_HOV = new Color(0.55f, 0.08f, 0.08f, 1f);
    private static final Color COLOR_TEXTO     = new Color(1f,    0.85f, 0.85f, 1f);
    private static final Color COLOR_TITULO    = new Color(0.9f,  0.15f, 0.15f, 1f);
    private static final Color COLOR_DESACT    = new Color(0.35f, 0.25f, 0.25f, 1f);

    public PantallaMenu(BMUGame juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font          = new BitmapFont();
        font.getData().setScale(2.5f);

        camara = new OrthographicCamera();
        camara.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        tienePartidaGuardada = Gdx.app.getPreferences("bmu_save").getBoolean("tiene_guardado", false);

        float w  = Gdx.graphics.getWidth();
        float h  = Gdx.graphics.getHeight();
        float bW = w * 0.55f;
        float bH = h * 0.10f;
        float bX = (w - bW) / 2f;

        btnNuevaPartida = new Rectangle(bX, h * 0.30f, bW, bH);
        btnContinuar    = new Rectangle(bX, h * 0.16f, bW, bH);

        GestorAudio.getInstance().iniciar();
    }

    @Override
    public void render(float delta) {
        tiempoMenu     += delta;
        pulsoNueva      = Math.max(0, pulsoNueva     - delta * 4f);
        pulsoContinuar  = Math.max(0, pulsoContinuar - delta * 4f);

        float w  = Gdx.graphics.getWidth();
        float h  = Gdx.graphics.getHeight();
        float bW = w * 0.55f;
        float bH = h * 0.10f;
        float bX = (w - bW) / 2f;

        btnNuevaPartida.set(bX, h * 0.30f, bW, bH);
        btnContinuar.set(   bX, h * 0.16f, bW, bH);

        // ── Detectar toque: solo marcar acción, NO cambiar pantalla aquí ────
        if (Gdx.input.justTouched()) {
            float tx = Gdx.input.getX();
            float ty = h - Gdx.input.getY();
            if (btnNuevaPartida.contains(tx, ty)) {
                pulsoNueva = 1f;
                accionPendiente = Accion.NUEVA_PARTIDA;
            } else if (btnContinuar.contains(tx, ty) && tienePartidaGuardada) {
                pulsoContinuar = 1f;
                accionPendiente = Accion.CONTINUAR;
            }
        }

        // ── Limpiar ──────────────────────────────────────────────────────────
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1f); // Fondo más oscuro
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, w, h);
        shapeRenderer.setProjectionMatrix(uiMatrix);

        // ── Cuadrícula de fondo en movimiento ────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.2f, 0.05f, 0.05f, 0.3f);
        float spacing = 60f;
        float offsetX = (tiempoMenu * 15f) % spacing;
        float offsetY = (tiempoMenu * 15f) % spacing;
        for (float x = offsetX - spacing; x < w + spacing; x += spacing) shapeRenderer.line(x, 0, x, h);
        for (float y = offsetY - spacing; y < h + spacing; y += spacing) shapeRenderer.line(0, y, w, y);
        shapeRenderer.end();

        // ── Líneas decorativas ────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_TITULO.r, COLOR_TITULO.g, COLOR_TITULO.b, 0.8f);
        shapeRenderer.rect(0, h - 8, w, 8);
        shapeRenderer.rect(0, 0,     w, 8);
        shapeRenderer.end();

        // ── Sombra de los botones (efecto 3D) ─────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.02f, 0.02f, 0.8f);
        shapeRenderer.rect(btnNuevaPartida.x + 8, btnNuevaPartida.y - 8, btnNuevaPartida.width, btnNuevaPartida.height);
        if (tienePartidaGuardada) {
            shapeRenderer.rect(btnContinuar.x + 8, btnContinuar.y - 8, btnContinuar.width, btnContinuar.height);
        }
        shapeRenderer.end();

        // ── Relleno de botones ────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Color cNueva = pulsoNueva > 0 ? COLOR_BOTON_HOV : new Color(0.25f, 0.05f, 0.05f, 1f);
        shapeRenderer.setColor(cNueva);
        shapeRenderer.rect(btnNuevaPartida.x, btnNuevaPartida.y, btnNuevaPartida.width, btnNuevaPartida.height);
        Color cCont = tienePartidaGuardada
            ? (pulsoContinuar > 0 ? COLOR_BOTON_HOV : new Color(0.2f, 0.05f, 0.05f, 1f))
            : new Color(0.10f, 0.08f, 0.08f, 0.5f);
        shapeRenderer.setColor(cCont);
        shapeRenderer.rect(btnContinuar.x, btnContinuar.y, btnContinuar.width, btnContinuar.height);
        shapeRenderer.end();

        // ── Bordes de botones ─────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COLOR_TITULO.r, COLOR_TITULO.g, COLOR_TITULO.b, 0.8f);
        shapeRenderer.rect(btnNuevaPartida.x, btnNuevaPartida.y, btnNuevaPartida.width, btnNuevaPartida.height);
        if (tienePartidaGuardada) {
            shapeRenderer.rect(btnContinuar.x, btnContinuar.y, btnContinuar.width, btnContinuar.height);
        } else {
            shapeRenderer.setColor(0.3f, 0.2f, 0.2f, 0.4f);
            shapeRenderer.rect(btnContinuar.x, btnContinuar.y, btnContinuar.width, btnContinuar.height);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Texto ─────────────────────────────────────────────────────────────
        batch.setProjectionMatrix(uiMatrix);
        batch.begin();

        font.getData().setScale(4.5f); // Título más grande
        GlyphLayout glTitulo = new GlyphLayout(font, "BEAT 'EM UP");
        // Sombra del título
        font.setColor(0f, 0f, 0f, 0.8f);
        font.draw(batch, glTitulo, (w - glTitulo.width) / 2f + 8, h * 0.78f - 8);
        // Texto del título
        font.setColor(COLOR_TITULO);
        font.draw(batch, glTitulo, (w - glTitulo.width) / 2f, h * 0.78f);

        float alpha = 0.6f + 0.4f * (float) Math.sin(tiempoMenu * 3f);
        font.getData().setScale(1.5f);
        font.setColor(1f, 1f, 0.5f, alpha); // Letras amarillentas parpadeando
        GlyphLayout glSub = new GlyphLayout(font, "INSERT COIN");
        font.draw(batch, glSub, (w - glSub.width) / 2f, h * 0.58f);

        font.getData().setScale(1.2f);
        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        font.draw(batch, "v1.0.1", 20, 35);

        font.getData().setScale(2.5f);
        font.setColor(COLOR_TEXTO);
        GlyphLayout glNueva = new GlyphLayout(font, "NUEVA PARTIDA");
        // Sombra del texto
        font.setColor(0f, 0f, 0f, 0.6f);
        font.draw(batch, glNueva,
            btnNuevaPartida.x + (btnNuevaPartida.width  - glNueva.width)  / 2f + 3,
            btnNuevaPartida.y + (btnNuevaPartida.height + glNueva.height) / 2f - 3);
        font.setColor(COLOR_TEXTO);
        font.draw(batch, glNueva,
            btnNuevaPartida.x + (btnNuevaPartida.width  - glNueva.width)  / 2f,
            btnNuevaPartida.y + (btnNuevaPartida.height + glNueva.height) / 2f);

        GlyphLayout glCont = new GlyphLayout(font, "CONTINUAR");
        if (tienePartidaGuardada) {
            font.setColor(0f, 0f, 0f, 0.6f);
            font.draw(batch, glCont,
                btnContinuar.x + (btnContinuar.width  - glCont.width)  / 2f + 3,
                btnContinuar.y + (btnContinuar.height + glCont.height) / 2f - 3);
        }
        font.setColor(tienePartidaGuardada ? COLOR_TEXTO : COLOR_DESACT);
        font.draw(batch, glCont,
            btnContinuar.x + (btnContinuar.width  - glCont.width)  / 2f,
            btnContinuar.y + (btnContinuar.height + glCont.height) / 2f);

        if (!tienePartidaGuardada) {
            font.getData().setScale(1.2f);
            font.setColor(0.4f, 0.3f, 0.3f, 1f);
            GlyphLayout glSin = new GlyphLayout(font, "Sin partida guardada");
            font.draw(batch, glSin,
                btnContinuar.x + (btnContinuar.width - glSin.width) / 2f,
                btnContinuar.y - 10);
        }

        batch.end();

        // ── Ejecutar acción DESPUÉS de que todo el frame terminó de dibujar ──
        if (accionPendiente == Accion.NUEVA_PARTIDA) {
            accionPendiente = Accion.NINGUNA;
            Gdx.app.getPreferences("bmu_save").putBoolean("tiene_guardado", false).flush();
            juego.setScreen(new PantallaJuego(juego, 0, 5));
            dispose();
        } else if (accionPendiente == Accion.CONTINUAR) {
            accionPendiente = Accion.NINGUNA;
            int escenarioGuardado = Gdx.app.getPreferences("bmu_save").getInteger("escenario", 0);
            int vidasGuardadas    = Gdx.app.getPreferences("bmu_save").getInteger("vidas", 5);
            juego.setScreen(new PantallaJuego(juego, escenarioGuardado, vidasGuardadas));
            dispose();
        }
    }

    @Override public void resize(int width, int height) { camara.setToOrtho(false, width, height); }
    @Override public void pause()  { GestorAudio.getInstance().pausar(); }
    @Override public void resume() { GestorAudio.getInstance().reanudar(); }
    @Override public void hide()   {}

    @Override
    public void dispose() {
        if (batch != null)         batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null)          font.dispose();
    }
}