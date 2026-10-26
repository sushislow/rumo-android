package br.com.rumo.diario;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import android.view.WindowManager;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int CREATE_DOCUMENT = 1001;
    private static final int OPEN_DOCUMENT = 1002;
    private static final String APP_URL = "https://appassets.androidplatform.net/assets/index.html";
    private WebView webView;
    private ValueCallback<Uri[]> importFileCallback;
    private PendingExport pendingExport;

    private static final class PendingExport {
        final String name, mime, data;
        PendingExport(String name, String mime, String data) {
            this.name = name;
            this.mime = mime;
            this.data = data;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Prevent accidental screenshots and recent-app previews of sensitive diary entries.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Color.rgb(11, 20, 33));
        getWindow().setNavigationBarColor(Color.rgb(16, 30, 44));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 20, 33));
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true); // Only imported content:// files from system picker; needed for JSON backup import.
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                // This journal works entirely offline; refuse external URLs.
                return !"appassets.androidplatform.net".equals(uri.getHost())
                        || !"https".equals(uri.getScheme());
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (importFileCallback != null) importFileCallback.onReceiveValue(null);
                importFileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
                try {
                    startActivityForResult(intent, OPEN_DOCUMENT);
                    return true;
                } catch (Exception e) {
                    importFileCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "Não foi possível abrir os arquivos.", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        });
        // Only first-party static HTML is loaded. This bridge is never exposed to internet content.
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void copyText(String value) {
                runOnUiThread(() -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Check-in Rumo", value));
                });
            }
            @JavascriptInterface
            public void saveFile(String filename, String mime, String contents) {
                runOnUiThread(() -> {
                    // Preserve journal exports with Android's secure system file picker.
                    pendingExport = new PendingExport(filename, mime, contents);
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(mime);
                    intent.putExtra(Intent.EXTRA_TITLE, filename);
                    try {
                        startActivityForResult(intent, CREATE_DOCUMENT);
                    } catch (Exception e) {
                        pendingExport = null;
                        Toast.makeText(MainActivity.this, "Não foi possível escolher o destino.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, "RumoAndroid");
        webView.loadUrl(APP_URL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_DOCUMENT) {
            ValueCallback<Uri[]> callback = importFileCallback;
            importFileCallback = null;
            if (callback != null) {
                Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
                callback.onReceiveValue(uri == null ? null : new Uri[]{uri});
            }
        } else if (requestCode == CREATE_DOCUMENT) {
            PendingExport export = pendingExport;
            pendingExport = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && export != null) {
                try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
                    if (output == null) throw new Exception("Erro ao abrir o arquivo");
                    output.write(export.data.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, "Backup salvo com sucesso.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Não foi possível salvar o arquivo.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        // In-app navigation is tab based; Android Back returns to the calendar.
        webView.evaluateJavascript("(function(){var b=document.querySelector('[data-tab=calendar]');if(b)b.click();})()", null);
    }

    @Override
    protected void onDestroy() {
        if (importFileCallback != null) {
            importFileCallback.onReceiveValue(null);
            importFileCallback = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
