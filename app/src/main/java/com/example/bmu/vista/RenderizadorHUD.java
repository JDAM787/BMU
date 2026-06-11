package com.example.bmu.vista;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.example.bmu.modelos.Jugador;

public class RenderizadorHUD {

    private Texture texturaHUDFondo;
    private Texture texturaHUDBarra;
    private Texture texturaGameOver;
    private Texture texturaYouWin;

    public RenderizadorHUD() {
        texturaHUDFondo = new Texture("HUD/salud/salud_fondo.png");
        texturaHUDBarra = new Texture("HUD/salud/salud_barra.png");
        texturaGameOver = new Texture("HUD/gameover.png");
        texturaYouWin   = new Texture("HUD/youwin.png"); 
    }

    public void dibujarHUD(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font, com.badlogic.gdx.graphics.OrthographicCamera camara,
                           Jugador jugador, int vidas, boolean isGameOver, float tiempoGameOver, boolean isVictoria) {
        if (isVictoria) {
            Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(uiMatrix);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0, 0, 0, 0.7f));
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

            batch.setProjectionMatrix(uiMatrix);
            batch.begin();
            float targetWidth  = Gdx.graphics.getWidth()  * 0.8f;
            float targetHeight = Gdx.graphics.getHeight() * 0.8f;
            float w = targetWidth;
            float h = texturaYouWin.getHeight() * (w / texturaYouWin.getWidth());
            if (h > targetHeight) { h = targetHeight; w = texturaYouWin.getWidth() * (h / texturaYouWin.getHeight()); }
            batch.draw(texturaYouWin, (Gdx.graphics.getWidth() - w) / 2f, (Gdx.graphics.getHeight() - h) / 2f, w, h);
            batch.end();
            return;
        }
        if (isGameOver) {
            Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(uiMatrix);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0, 0, 0, 0.7f)); // Pantalla oscura
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

            batch.setProjectionMatrix(uiMatrix);
            batch.begin();
            
            float targetWidth = Gdx.graphics.getWidth() * 0.8f;
            float targetHeight = Gdx.graphics.getHeight() * 0.8f;

            float goWidth = targetWidth;
            float goHeight = texturaGameOver.getHeight() * (goWidth / texturaGameOver.getWidth());

            if (goHeight > targetHeight) {
                goHeight = targetHeight;
                goWidth = texturaGameOver.getWidth() * (goHeight / texturaGameOver.getHeight());
            }

            float goX = (Gdx.graphics.getWidth() - goWidth) / 2f;
            float goY = (Gdx.graphics.getHeight() - goHeight) / 2f;

            // Dibujar la textura original (que incluye el game over y el insert coin)
            batch.draw(texturaGameOver, goX, goY, goWidth, goHeight);

            batch.end();
        } else {
            batch.setProjectionMatrix(camara.combined);
            batch.begin();

            float hudAncho = 4.5f;
            float hudAlto  = hudAncho * ((float) texturaHUDFondo.getHeight() / texturaHUDFondo.getWidth());
            float hudX = camara.position.x - (Gdx.graphics.getWidth() / com.example.bmu.fisicas.MundoFisico.PPM) / 2f;
            float hudY = camara.position.y + (Gdx.graphics.getHeight() / com.example.bmu.fisicas.MundoFisico.PPM) / 2f - hudAlto;

            float barraX     = hudX   + hudAncho * (403f / 1536f);
            float barraY     = hudY   + hudAlto  * (503f / 1024f);
            float barraAncho = hudAncho * (874f / 1536f);
            float barraAlto  = hudAlto  * (98f / 1024f);

            float pct = Math.max(0, jugador.getVidaActual()) / (float) jugador.getVidaMaxima();

            if (pct > 0) {
                com.badlogic.gdx.graphics.g2d.TextureRegion regionBarra = new com.badlogic.gdx.graphics.g2d.TextureRegion(texturaHUDBarra,
                        0, 0,
                        (int)(texturaHUDBarra.getWidth() * pct),
                        texturaHUDBarra.getHeight());
                batch.draw(regionBarra, barraX, barraY, barraAncho * pct, barraAlto);
            }
            batch.draw(texturaHUDFondo, hudX, hudY, hudAncho, hudAlto);

            com.badlogic.gdx.graphics.g2d.TextureRegion regionCara = new com.badlogic.gdx.graphics.g2d.TextureRegion(texturaHUDFondo, 141, 350, 262, 259);
            float tamanoVida = 0.45f; // en metros
            float espaciado = 0.5f;
            float rightX = camara.position.x + (Gdx.graphics.getWidth() / com.example.bmu.fisicas.MundoFisico.PPM) / 2f;
            float topY   = camara.position.y + (Gdx.graphics.getHeight() / com.example.bmu.fisicas.MundoFisico.PPM) / 2f;
            float vidasX = rightX - 0.2f - tamanoVida;
            float vidasY = topY - 0.2f - tamanoVida;

            for (int i = 0; i < vidas; i++) {
                batch.draw(regionCara, vidasX - (i * espaciado), vidasY, tamanoVida, tamanoVida);
            }

            batch.end();
        }
    }

    public void dispose() {
        if (texturaHUDFondo != null) texturaHUDFondo.dispose();
        if (texturaHUDBarra != null) texturaHUDBarra.dispose();
        if (texturaYouWin != null) texturaYouWin.dispose();
        if (texturaGameOver != null) texturaGameOver.dispose();
    }
}
