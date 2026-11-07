package fr.paniercommun.app;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 42;
    private static final int CAM_REQ = 7;
    // Adresse sécurisée (https) servie depuis l'application : nécessaire pour la caméra
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START = "https://" + HOST + "/assets/index.html";

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingPerm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // localStorage : les données restent sur l'appareil
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);

        web.addJavascriptInterface(new Bridge(this), "Android");

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (HOST.equals(u.getHost())) return false;
                // Liens externes : ouverts dans le navigateur du téléphone
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(() -> {
                    boolean cam = Arrays.asList(r.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
                    if (!cam) { r.deny(); return; }
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        r.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        if (pendingPerm != null) pendingPerm.deny();
                        pendingPerm = r;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAM_REQ);
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Choisir une image"), PICK_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(START);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        if (req == CAM_REQ && pendingPerm != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPerm.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            } else {
                pendingPerm.deny();
                Toast.makeText(this, "Caméra refusée : autorisez-la dans Paramètres → Applications → Boutique Solidaire", Toast.LENGTH_LONG).show();
            }
            pendingPerm = null;
            return;
        }
        super.onRequestPermissionsResult(req, perms, results);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK_FILE && fileCallback != null) {
            Uri[] r = null;
            if (res == RESULT_OK && data != null && data.getData() != null) r = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(r);
            fileCallback = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        // Ferme d'abord une fenêtre ouverte (et coupe la caméra), puis revient à l'écran précédent
        web.evaluateJavascript(
            "(function(){var b=document.querySelector('#modal-root [data-close]');if(b){b.click();return 'm';}return '';})()",
            v -> {
                if ("\"m\"".equals(v)) return;
                if (web.canGoBack()) web.goBack();
                else MainActivity.super.onBackPressed();
            });
    }

    /** Pont appelé depuis la page : enregistrement des PDF d'étiquettes, copie du CSV. */
    static class Bridge {
        private final Activity a;
        Bridge(Activity a) { this.a = a; }

        @JavascriptInterface
        public boolean saveFile(String base64, String filename, String mime) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Uri uri;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    uri = a.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) return false;
                    try (OutputStream os = a.getContentResolver().openOutputStream(uri)) { os.write(bytes); }
                } else {
                    File dir = a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    File f = new File(dir, filename);
                    try (FileOutputStream os = new FileOutputStream(f)) { os.write(bytes); }
                    a.runOnUiThread(() -> Toast.makeText(a, "Enregistré : " + f.getAbsolutePath(), Toast.LENGTH_LONG).show());
                    return true;
                }
                final Uri open = uri;
                a.runOnUiThread(() -> {
                    try {
                        Intent v = new Intent(Intent.ACTION_VIEW);
                        v.setDataAndType(open, mime);
                        v.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        a.startActivity(Intent.createChooser(v, "Ouvrir le PDF"));
                    } catch (Exception ignored) {}
                });
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /** Couleurs des barres système selon le thème de la page (clair ou sombre). */
        @JavascriptInterface
        public void setBars(String navHex, String statusHex, boolean dark) {
            a.runOnUiThread(() -> {
                try {
                    Window w = a.getWindow();
                    w.setStatusBarColor(Color.parseColor("#" + statusHex));
                    w.setNavigationBarColor(Color.parseColor("#" + navHex));
                    View d = w.getDecorView();
                    int f = d.getSystemUiVisibility();
                    int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    d.setSystemUiVisibility(dark ? (f & ~light) : (f | light));
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public boolean copy(String text) {
            a.runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Rapport", text));
            });
            return true;
        }
    }
}
