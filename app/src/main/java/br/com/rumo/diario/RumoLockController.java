package br.com.rumo.diario;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native lock screen: the diary WebView cannot dismiss it or validate a PIN. */
public final class RumoLockController {
    private final FragmentActivity activity;
    private final RumoVault vault;
    private final FrameLayout container;
    private final Runnable afterUnlock, afterChange;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean unlocked;
    private boolean busy;
    private long lockEpoch;
    private final java.util.List<AlertDialog> dialogs = new java.util.ArrayList<>();
    private LinearLayout screen;
    private EditText input;
    private TextView message;
    private Button unlock;
    private BiometricPrompt biometric;
    private final Runnable countdown = new Runnable() {
        @Override public void run() {
            if (screen == null || unlocked || activity.isFinishing()) return;
            long wait = vault.waitMillis();
            if (wait > 0) message.setText("Aguarde " + ((wait + 999) / 1000) + " s antes de tentar novamente.");
            else if (!busy) message.setText("Digite seu PIN para abrir o diário.");
            unlock.setEnabled(wait == 0 && !busy);
            handler.postDelayed(this, 1000);
        }
    };

    public RumoLockController(FragmentActivity activity, RumoVault vault, FrameLayout container,
                              Runnable afterUnlock, Runnable afterChange) {
        this.activity = activity; this.vault = vault; this.container = container;
        this.afterUnlock = afterUnlock; this.afterChange = afterChange;
        unlocked = !vault.pinEnabled();
    }
    public boolean isUnlocked() { return unlocked; }
    public boolean biometricsAvailable() {
        return BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }
    private RumoStyle style() { return new RumoStyle(activity, vault); }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) { return style().label(value, size); }
    private EditText pinInput(String hint) {
        EditText edit = style().input(hint, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(12)});
        edit.setLongClickable(false); return edit;
    }
    private Button button(String label, Runnable action) { return style().button(label, action); }
    private void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    public void lock() {
        lockEpoch++;
        if (!vault.pinEnabled()) { unlocked = true; return; }
        unlocked = false;
        for (AlertDialog dialog : new java.util.ArrayList<>(dialogs)) dialog.dismiss();
        if (screen != null) { container.bringChildToFront(screen); return; }
        for (int i = 0; i < container.getChildCount(); i++) container.getChildAt(i).setVisibility(View.INVISIBLE);
        screen = new LinearLayout(activity); screen.setOrientation(LinearLayout.VERTICAL); screen.setGravity(Gravity.CENTER_VERTICAL);
        screen.setPadding(dp(28), dp(24), dp(28), dp(24)); screen.setBackgroundColor(style().bg);
        screen.addView(text("Rumo", 34)); screen.addView(text("Seu espaço pessoal", 18));
        message = text("Digite seu PIN para abrir o diário.", 15); screen.addView(message);
        input = pinInput("PIN de 6 a 12 números"); screen.addView(input);
        unlock = button("Desbloquear", this::tryPin); screen.addView(unlock);
        if (vault.biometricEnabled() && biometricsAvailable()) screen.addView(button("Usar biometria", () -> authenticate(this::unlockSession)));
        screen.addView(button("Esqueci meu PIN", () -> trackDialog(style().dialog()
                .setTitle("Acesso ao seu diário")
                .setMessage("O PIN não é enviado a um servidor e não pode ser recuperado. Se você ativou biometria, use-a. Caso contrário, mantenha seu backup externo protegido. Limpar dados ou reinstalar apaga o cofre local; só faça isso se tiver o backup e sua senha para restaurar.")
                .setPositiveButton("Entendi", null).show())));
        container.addView(screen, new FrameLayout.LayoutParams(-1, -1)); handler.removeCallbacks(countdown); countdown.run();
    }
    private void tryPin() {
        if (busy || vault.waitMillis() > 0) return;
        final String pin = input.getText().toString(); input.setText("");
        final long requestEpoch = lockEpoch;
        busy = true; unlock.setEnabled(false); message.setText("Verificando…");
        worker.execute(() -> {
            try {
                boolean ok = vault.verifyPin(pin);
                activity.runOnUiThread(() -> {
                    busy = false;
                    if (ok && requestEpoch == lockEpoch && activity.getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) unlockSession(); else {
                        Toast.makeText(activity, "PIN não confere.", Toast.LENGTH_SHORT).show();
                        if (unlock != null) unlock.setEnabled(vault.waitMillis() == 0);
                    }
                });
            } catch (Exception error) { activity.runOnUiThread(() -> { busy = false; message.setText("Não foi possível verificar o cofre. Seus dados não foram apagados."); }); }
        });
    }
    public void unlockSession() {
        unlocked = true; handler.removeCallbacks(countdown);
        if (input != null) { input.setText(""); hideKeyboard(input); }
        if (screen != null) { container.removeView(screen); screen = null; }
        for (int i = 0; i < container.getChildCount(); i++) container.getChildAt(i).setVisibility(View.VISIBLE);
        afterUnlock.run();
    }
    private void authenticate(Runnable success) {
        if (!biometricsAvailable()) { Toast.makeText(activity, "Biometria forte indisponível. Use seu PIN.", Toast.LENGTH_LONG).show(); return; }
        final long requestEpoch = lockEpoch;
        biometric = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                if (requestEpoch == lockEpoch && activity.getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) success.run();
            }
            @Override public void onAuthenticationError(int code, CharSequence error) {
                if (code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                    Toast.makeText(activity, "Use seu PIN para continuar.", Toast.LENGTH_SHORT).show();
            }
        });
        biometric.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("Desbloquear Rumo")
                .setSubtitle("Confirme com a biometria deste aparelho")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Usar PIN").build());
    }
    public void showSettings() {
        if (!unlocked) return;
        String[] choices = {vault.pinEnabled() ? "Alterar PIN" : "Criar PIN",
                vault.biometricEnabled() ? "Desativar biometria" : "Ativar biometria", "Desativar PIN"};
        track(style().dialog().setTitle("Bloqueio do Rumo").setItems(choices, (dialog, item) -> {
            if (!unlocked) return;
            if (item == 0) changePin();
            else if (item == 1) {
                if (!vault.pinEnabled()) { Toast.makeText(activity, "Crie um PIN primeiro.", Toast.LENGTH_LONG).show(); return; }
                if (vault.biometricEnabled()) confirmPin(() -> changeBiometric(false));
                else confirmPin(() -> authenticate(() -> changeBiometric(true)));
            } else if (vault.pinEnabled()) confirmPin(() -> {
                try { vault.disablePin(); afterChange.run(); Toast.makeText(activity, "PIN desativado. A criptografia local continua ativa.", Toast.LENGTH_LONG).show(); }
                catch (Exception error) { Toast.makeText(activity, "Não foi possível alterar o bloqueio.", Toast.LENGTH_LONG).show(); }
            });
        }).setNegativeButton("Fechar", null).show());
    }
    private void changeBiometric(boolean enabled) {
        try { vault.setBiometric(enabled); afterChange.run(); Toast.makeText(activity, enabled ? "Biometria ativada." : "Biometria desativada.", Toast.LENGTH_SHORT).show(); }
        catch (Exception error) { Toast.makeText(activity, "Não foi possível salvar a preferência.", Toast.LENGTH_LONG).show(); }
    }
    private void changePin() {
        if (vault.pinEnabled()) { confirmPin(this::newPinDialog); return; }
        newPinDialog();
    }
    private void newPinDialog() {
        LinearLayout fields = new LinearLayout(activity); fields.setOrientation(LinearLayout.VERTICAL); fields.setPadding(dp(20), dp(12), dp(20), dp(12));
        fields.setBackgroundColor(style().panel);
        fields.addView(text("Guarde seu PIN. Sem PIN ou biometria, o acesso depende de reinstalar e restaurar um backup externo.", 14));
        EditText first = pinInput("Novo PIN (6 a 12 números)"), second = pinInput("Repita o PIN"); fields.addView(first); fields.addView(second);
        AlertDialog dialog = style().dialog().setTitle("Criar ou alterar PIN").setView(fields)
                .setPositiveButton("Salvar PIN", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(-1).setOnClickListener(button -> {
            if (!unlocked) { dialog.dismiss(); return; }
            String pin = first.getText().toString(), confirmation = second.getText().toString();
            if (!VaultCrypto.validPin(pin) || !pin.equals(confirmation)) { first.setError("Use de 6 a 12 números e repita o mesmo PIN."); return; }
            dialog.getButton(-1).setEnabled(false); first.setText(""); second.setText("");
            final long requestEpoch = lockEpoch;
            worker.execute(() -> {
                try { vault.setPin(pin); activity.runOnUiThread(() -> { dialog.dismiss(); if (requestEpoch != lockEpoch) lock(); if (unlocked) afterChange.run(); Toast.makeText(activity, "PIN ativado. O Rumo será bloqueado ao sair.", Toast.LENGTH_LONG).show(); }); }
                catch (Exception error) { activity.runOnUiThread(() -> { dialog.getButton(-1).setEnabled(true); Toast.makeText(activity, "Falha ao salvar PIN; configuração anterior preservada.", Toast.LENGTH_LONG).show(); }); }
            });
        })); dialog.show(); style().style(dialog); track(dialog);
    }
    private void confirmPin(Runnable success) {
        if (!unlocked || vault.waitMillis() > 0) { Toast.makeText(activity, "Aguarde antes de tentar novamente.", Toast.LENGTH_LONG).show(); return; }
        EditText field = pinInput("PIN atual"); 
        AlertDialog dialog = style().dialog().setTitle("Confirme seu PIN").setView(field)
                .setPositiveButton("Continuar", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(-1).setOnClickListener(button -> {
            String pin = field.getText().toString(); field.setText(""); dialog.getButton(-1).setEnabled(false);
            worker.execute(() -> {
                try { boolean ok = vault.verifyPin(pin); activity.runOnUiThread(() -> {
                    dialog.dismiss(); if (ok && unlocked) success.run(); else Toast.makeText(activity, "PIN não confere. Aguarde se houver bloqueio de tentativas.", Toast.LENGTH_LONG).show();
                }); } catch (Exception error) { activity.runOnUiThread(() -> { dialog.dismiss(); Toast.makeText(activity, "Falha ao confirmar o PIN.", Toast.LENGTH_LONG).show(); }); }
            });
        })); dialog.show(); style().style(dialog); track(dialog);
    }
    private void trackDialog(AlertDialog dialog) { track(dialog); }
    private void track(AlertDialog dialog) { dialogs.add(dialog); dialog.setOnDismissListener(value -> dialogs.remove(dialog)); }
    public void destroy() { handler.removeCallbacksAndMessages(null); for (AlertDialog dialog : new java.util.ArrayList<>(dialogs)) dialog.dismiss(); if (biometric != null) biometric.cancelAuthentication(); worker.shutdownNow(); }
}
