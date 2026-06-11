package com.example.bmu;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import java.io.File;
import java.io.FileOutputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidGraphics;

public class AndroidLauncher extends AndroidApplication implements ScreenshotHandler {

    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;

        // Solicitar permiso en tiempo de ejecución (Android 6+)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_STORAGE);
        }

        initialize(new BMUGame(this), config);
    }

    @Override
    public void tomarYCompartirScreenshot() {
        ((android.opengl.GLSurfaceView)
            ((AndroidGraphics) Gdx.graphics).getView()).queueEvent(() -> {
            try {
                int w = Gdx.graphics.getBackBufferWidth();
                int h = Gdx.graphics.getBackBufferHeight();

                java.nio.ByteBuffer pixels = java.nio.ByteBuffer.allocateDirect(w * h * 4);
                pixels.order(java.nio.ByteOrder.nativeOrder());
                android.opengl.GLES20.glReadPixels(0, 0, w, h,
                    android.opengl.GLES20.GL_RGBA,
                    android.opengl.GLES20.GL_UNSIGNED_BYTE, pixels);

                pixels.position(0);
                int[] argb = new int[w * h];
                for (int y = 0; y < h; y++) {
                    int offset = (h - 1 - y) * w;
                    for (int x = 0; x < w; x++) {
                        int r = pixels.get() & 0xFF;
                        int g = pixels.get() & 0xFF;
                        int b = pixels.get() & 0xFF;
                        int a = pixels.get() & 0xFF;
                        argb[offset + x] = android.graphics.Color.argb(a, r, g, b);
                    }
                }
                android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(argb, w, h, android.graphics.Bitmap.Config.ARGB_8888);
                compartirBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void compartirBitmap(android.graphics.Bitmap bitmap) {
        runOnUiThread(() -> {
            try {
                String nombreArchivo = "bmu_screenshot_" + System.currentTimeMillis() + ".png";
                java.io.File directorio = new java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES), "BMU");
                if (!directorio.exists()) directorio.mkdirs();

                java.io.File archivo = new java.io.File(directorio, nombreArchivo);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(archivo);
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", archivo);

                android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_SEND);
                intent.setType("image/png");
                intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                intent.putExtra(android.content.Intent.EXTRA_TEXT, "¡Mirá mi partida en Beat 'Em Up!");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(intent, "Compartir screenshot"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}