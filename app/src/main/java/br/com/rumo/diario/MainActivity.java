package br.com.rumo.diario;

import android.Manifest;
import androidx.fragment.app.FragmentActivity;
import android.content.pm.PackageManager;
import android.os.Build;
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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.WindowManager;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends FragmentActivity {
    private static final int CREATE_DOCUMENT = 1001;
    private static final int OPEN_DOCUMENT = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1003;
    private static final String APP_URL = "https://appassets.androidplatform.net/assets/index.html";
    private WebView webView;
    private FrameLayout container;
    private RumoAccessController accessController;
    private RumoVault vault;
    private RumoAccountController accountController;
    private RumoLockController lockController;
    private boolean pageStarted;
    private Uri queuedImport;
    private Boolean queuedNotificationEnabled;
    private String queuedNotificationMessage;
    private ValueCallback<Uri[]> importFileCallback;
    private PendingExport pendingExport;
    private int pendingReminderCount = 0;
    private String[] pendingReminderTimes = new String[]{"08:30", "14:00", "20:30"};

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
        try { vault = RumoVault.getInstance(this); }
        catch (Exception error) {
            TextView failure = new TextView(this);
            failure.setText("Não foi possível abrir o cofre do Rumo. Os dados não foram apagados. Preserve seu backup externo e procure suporte antes de limpar os dados ou reinstalar.");
            failure.setTextColor(Color.WHITE); failure.setBackgroundColor(Color.rgb(11, 20, 33));
            failure.setPadding(32, 60, 32, 32); setContentView(failure); return;
        }
        webView = new WebView(this);
        webView.setSaveEnabled(false);
        if (Build.VERSION.SDK_INT >= 26) webView.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        webView.setBackgroundColor(Color.rgb(11, 20, 33));
        container = new FrameLayout(this);
        container.addView(webView, new FrameLayout.LayoutParams(-1, -1)); setContentView(container);
        lockController = new RumoLockController(this, vault, container, this::showUnlockedApp,
                () -> webView.evaluateJavascript("window.RumoNativeSettingsChanged && window.RumoNativeSettingsChanged()", null));
        // Keep content below the status/navigation bars when targeting Android 16.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(container,(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        accountController=new RumoAccountController(this,vault,()->{if(accessController!=null)accessController.accountChanged();});
        accessController=new RumoAccessController(this,vault,container,accountController,this::showUnlockedApp);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSaveFormData(false);
        android.webkit.WebViewDatabase.getInstance(this).clearFormData();
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
                WebResourceResponse local = assetLoader.shouldInterceptRequest(request.getUrl());
                if (local != null) return local;
                // INTERNET is only for native authentication. The diary cannot reach any server.
                return new WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", java.util.Collections.emptyMap(),
                        new java.io.ByteArrayInputStream(new byte[0]));
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showToolsIfRequested();
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
                if (!canUseDiary() || value == null || value.length() > 100000) return;
                runOnUiThread(() -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Check-in Rumo", value));
                });
            }
            @JavascriptInterface
            public void configureReminders(int count, String timesCsv) {
                if (!canUseDiary()) return;
                runOnUiThread(() -> {
                    if (count < 0 || count > 3 || timesCsv == null) {
                        notifyReminderResult(false, "Escolha inválida de lembretes.");
                        return;
                    }
                    String[] times = timesCsv.split(",", -1);
                    if (times.length != 3 || !RumoReminderWorker.timesValid(times, count)) {
                        notifyReminderResult(false, "Informe horários válidos e diferentes.");
                        return;
                    }
                    if (count == 0) {
                        RumoReminderWorker.configure(MainActivity.this, 0, times);
                        notifyReminderResult(true, "Lembretes desativados.");
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        pendingReminderCount = count;
                        pendingReminderTimes = times;
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
                        return;
                    }
                    RumoReminderWorker.configure(MainActivity.this, count, times);
                    notifyReminderResult(true, "Lembretes ativados. O Android pode atrasar o envio.");
                });
            }
            @JavascriptInterface
            public void saveFile(String filename, String mime, String contents) {
                if (!canUseDiary() || contents == null || contents.length() > 16000000) return;
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
            @JavascriptInterface public String getProtectedItem(String name) {
                if (!canUseDiary()) throw new IllegalStateException("Diário bloqueado");
                return vault.get(name);
            }
            @JavascriptInterface public boolean setProtectedItem(String name, String value) {
                if (!canUseDiary()) return false;
                try { vault.put(name, value); return true; } catch (Exception error) { return false; }
            }
            @JavascriptInterface public boolean setProtectedBundle(String json) {
                if (!canUseDiary()) return false;
                try { vault.putBundle(json); return true; } catch (Exception error) { return false; }
            }
            @JavascriptInterface public boolean migrateStorage(String json) {
                if (!canUseDiary()) return false;
                try { vault.migrate(json); return true; } catch (Exception error) { return false; }
            }
            @JavascriptInterface public String getAppInfo() {
                if (!canUseDiary()) return "{}";
                org.json.JSONObject info = new org.json.JSONObject();
                try {
                    info.put("version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
                        .put("android", Build.VERSION.RELEASE).put("encryptedStorage", true)
                        .put("pinEnabled", vault.pinEnabled()).put("biometricEnabled", vault.biometricEnabled())
                        .put("biometricAvailable", lockController.biometricsAvailable())
                        .put("accountConfigured", RumoAccountController.configured(MainActivity.this))
                        .put("supportEmail", getString(R.string.support_email).trim());
                } catch (Exception ignored) { }
                return info.toString();
            }
            @JavascriptInterface public void accountSettings() {
                runOnUiThread(() -> {
                    if (!canUseDiary()) return;
                    accountController.show();
                });
            }
            @JavascriptInterface public void securitySettings() {
                runOnUiThread(() -> { if (canUseDiary()) lockController.showSettings(); });
            }
            @JavascriptInterface public void lockNow() {
                runOnUiThread(() -> { if (vault.pinEnabled()) lockController.lock(); });
            }
            @JavascriptInterface public boolean syncWidget(String json) {
                if (!canUseDiary() || json == null || json.length() > 2000) return false;
                try { RumoWidgetProvider.sync(MainActivity.this, json); return true; } catch (Exception error) { return false; }
            }
            @JavascriptInterface public void addWidget() {
                runOnUiThread(() -> {
                    if (!canUseDiary()) return;
                    if (!RumoWidgetProvider.requestAdd(MainActivity.this)) Toast.makeText(MainActivity.this,
                            "Na tela inicial, mantenha um espaço vazio pressionado > Widgets > Rumo.", Toast.LENGTH_LONG).show();
                });
            }
            @JavascriptInterface public void composeFeedback(String category, String description) {
                if (!canUseDiary() || description == null || description.length() > 8000) return;
                runOnUiThread(() -> {
                    if (!canUseDiary()) return;
                    String address = getString(R.string.support_email).trim();
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
                        Toast.makeText(MainActivity.this, "O contato ainda será configurado. Use Copiar mensagem.", Toast.LENGTH_LONG).show(); return;
                    }
                    String kind = "bug".equals(category) ? "Problema" : "Sugestão";
                    String version = "1.5.0-beta";
                    try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
                    String subject = "Rumo — " + kind + " — v" + version;
                    String body = description + "\n\nRumo " + version + " | Android " + Build.VERSION.RELEASE
                            + "\nNenhum registro do diário foi anexado automaticamente.";
                    Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(address, "@.+-")
                            + "?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body)));
                    email.putExtra(Intent.EXTRA_EMAIL, new String[]{address}); email.putExtra(Intent.EXTRA_SUBJECT, subject); email.putExtra(Intent.EXTRA_TEXT, body);
                    try { startActivity(email); }
                    catch (android.content.ActivityNotFoundException error) { Toast.makeText(MainActivity.this, "Nenhum aplicativo de e-mail disponível. Use Copiar mensagem.", Toast.LENGTH_LONG).show(); }
                });
            }
        }, "RumoAndroid");
        // Os trabalhos persistem após reiniciar; ao abrir, alinhamos horários ao fuso atual.
        RumoReminderWorker.refreshIfEnabled(this);
        if (lockController.isUnlocked()) showUnlockedApp(); else lockController.lock();
    }

    private boolean canUseDiary(){return lockController!=null&&lockController.isUnlocked()&&accessController!=null&&accessController.isAllowed();}

    private void showUnlockedApp() {
        if (isFinishing() || isDestroyed() || webView == null || lockController == null || !lockController.isUnlocked()) return;
        if (accessController != null && !accessController.enter()) { webView.setVisibility(android.view.View.INVISIBLE); return; }
        webView.setVisibility(android.view.View.VISIBLE);
        webView.onResume(); webView.resumeTimers();
        if (!pageStarted) { pageStarted = true; webView.loadUrl(APP_URL); }
        else showToolsIfRequested();
        if (queuedImport != null && importFileCallback != null) {
            Uri uri = queuedImport; queuedImport = null; ValueCallback<Uri[]> callback = importFileCallback; importFileCallback = null; callback.onReceiveValue(new Uri[]{uri});
        }
        if (queuedNotificationEnabled != null) {
            boolean enabled = queuedNotificationEnabled; String message = queuedNotificationMessage;
            queuedNotificationEnabled = null; queuedNotificationMessage = null; notifyReminderResult(enabled, message);
        }
    }

    private void notifyReminderResult(boolean enabled, String message) {
        if (webView == null) return;
        if (lockController != null && !lockController.isUnlocked()) { queuedNotificationEnabled = enabled; queuedNotificationMessage = message; return; }
        String safe = org.json.JSONObject.quote(message);
        webView.evaluateJavascript("window.RumoNativeNotificationsResult && window.RumoNativeNotificationsResult(" + (enabled ? "true" : "false") + "," + safe + ")", null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                RumoReminderWorker.configure(this, pendingReminderCount, pendingReminderTimes);
                notifyReminderResult(true, "Lembretes ativados. O Android pode atrasar o envio.");
            } else {
                RumoReminderWorker.configure(this, 0, pendingReminderTimes);
                notifyReminderResult(false, "Permissão negada. Lembretes continuam desligados.");
            }
            pendingReminderCount = 0;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_DOCUMENT) {
            ValueCallback<Uri[]> callback = importFileCallback;
            if (callback != null) {
                Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
                if (uri != null && lockController != null && !lockController.isUnlocked()) { queuedImport = uri; return; }
                importFileCallback = null;
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
        if (webView == null || !canUseDiary()) { super.onBackPressed(); return; }
        // In-app navigation is tab based; Android Back returns to the calendar.
        webView.evaluateJavascript("(function(){var b=document.querySelector('[data-tab=calendar]');if(b)b.click();})()", null);
    }

    private void showToolsIfRequested() {
        if (!canUseDiary()) return;
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("rumo_open_checkin", false) && webView != null) {
            intent.removeExtra("rumo_open_checkin");
            webView.evaluateJavascript("(function(){var b=document.querySelector('[data-action=\\\"today-checkin\\\"]');if(b)b.click();})()", null);
        }
        if (intent != null && intent.getBooleanExtra("rumo_open_tools", false) && webView != null) {
            intent.removeExtra("rumo_open_tools");
            webView.evaluateJavascript("(function(){var b=document.querySelector('[data-tab=tools]');if(b)b.click();})()", null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showToolsIfRequested();
    }

    @Override protected void onStart() {
        super.onStart();
        if (lockController != null && !lockController.isUnlocked()) lockController.lock();
    }
    @Override protected void onResume() {
        super.onResume();
        if (accessController != null) accessController.onResume();
        if (webView != null && lockController != null && lockController.isUnlocked()) showUnlockedApp();
    }
    @Override protected void onStop() {
        if (webView != null) webView.evaluateJavascript("window.RumoDismissSensitiveDialogs && window.RumoDismissSensitiveDialogs()", null);
        if (accountController != null) accountController.dismiss();
        if (accessController != null) accessController.onStop();
        if (lockController != null) lockController.lock();
        if (webView != null) { webView.onPause(); webView.pauseTimers(); }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (accessController != null) accessController.destroy();
        if (accountController != null) accountController.destroy();
        if (lockController != null) lockController.destroy();
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
