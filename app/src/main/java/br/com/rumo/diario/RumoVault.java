package br.com.rumo.diario;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

/** Device-local encrypted store. PIN is a native UI gate, never sent to JavaScript. */
public final class RumoVault {
    private static final String ALIAS = "br.com.rumo.diario.vault.v1";
    private static RumoVault instance;
    private final AtomicFile file;
    private final SecretKey key;
    private JSONObject state;

    public static synchronized RumoVault getInstance(Context context) throws Exception {
        if (instance == null) instance = new RumoVault(context.getApplicationContext());
        return instance;
    }

    public RumoVault(Context context) throws Exception {
        File base = new File(context.getNoBackupFilesDir(), "rumo-vault.enc");
        file = new AtomicFile(base);
        KeyStore keys = KeyStore.getInstance("AndroidKeyStore");
        keys.load(null);
        boolean exists = base.exists() || new File(base.getPath() + ".bak").exists();
        if (!keys.containsAlias(ALIAS)) {
            if (exists) throw new IllegalStateException("Chave do cofre indisponível; restaure seu backup externo.");
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).setRandomizedEncryptionRequired(true).build());
            generator.generateKey();
        }
        key = (SecretKey) keys.getKey(ALIAS, null);
        if (exists) {
            try (FileInputStream input = file.openRead(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                    if (bytes.size() > 16000000) throw new IllegalStateException("Cofre grande demais");
                }
                state = new JSONObject(new String(VaultCrypto.decrypt(bytes.toByteArray(), key), StandardCharsets.UTF_8));
                if (state.optInt("format") != 1 || state.optJSONObject("items") == null)
                    throw new IllegalStateException("Formato do cofre inválido");
                if (pinEnabled() && (state.optString("pinHash").isEmpty() || state.optString("pinSalt").isEmpty()))
                    throw new IllegalStateException("Configuração de bloqueio inválida");
            }
        } else {
            state = new JSONObject().put("format", 1).put("items", new JSONObject());
        }
    }

    private boolean allowed(String name) {
        return "rumo_diario_v1".equals(name) || "rumo_preferencias_v1".equals(name)
                || "rumo_conquistas_v1".equals(name);
    }

    public synchronized String get(String name) {
        if (!allowed(name)) throw new IllegalArgumentException("Chave não permitida");
        JSONObject items = state.optJSONObject("items");
        return items.has(name) ? items.optString(name, null) : null;
    }

    private void commit(JSONObject next) throws Exception {
        byte[] packed = VaultCrypto.encrypt(next.toString().getBytes(StandardCharsets.UTF_8), key);
        if (packed.length > 16000000) throw new IllegalArgumentException("O cofre atingiu o limite de tamanho");
        FileOutputStream stream = null;
        try {
            stream = file.startWrite(); stream.write(packed); file.finishWrite(stream);
            state = next;
        } catch (Exception error) {
            if (stream != null) file.failWrite(stream);
            throw error;
        }
    }

    public synchronized void put(String name, String value) throws Exception {
        if (!allowed(name) || value == null || value.length() > 8000000)
            throw new IllegalArgumentException("Dados inválidos");
        JSONObject next = new JSONObject(state.toString());
        next.getJSONObject("items").put(name, value); commit(next);
    }

    public synchronized void putBundle(String json) throws Exception {
        if (json == null || json.length() > 12000000) throw new IllegalArgumentException("Dados inválidos");
        JSONObject changes = new JSONObject(json), next = new JSONObject(state.toString());
        java.util.Iterator<String> names = changes.keys();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowed(name) || !(changes.get(name) instanceof String) || changes.getString(name).length() > 8000000) throw new IllegalArgumentException("Chave inválida");
            next.getJSONObject("items").put(name, changes.getString(name));
        }
        commit(next);
    }

    /** One atomic migration. Existing protected keys always win. */
    public synchronized void migrate(String json) throws Exception {
        if (json == null || json.length() > 12000000) throw new IllegalArgumentException("Migração inválida");
        JSONObject legacy = new JSONObject(json), next = new JSONObject(state.toString());
        JSONObject items = next.getJSONObject("items");
        for (String name : new String[]{"rumo_diario_v1", "rumo_preferencias_v1", "rumo_conquistas_v1"}) {
            if (!items.has(name) && legacy.has(name) && legacy.get(name) instanceof String) {
                if (legacy.getString(name).length() > 8000000) throw new IllegalArgumentException("Cópia anterior grande demais");
                items.put(name, legacy.getString(name));
            }
        }
        commit(next);
    }

    public synchronized boolean pinEnabled() { return state.optBoolean("pinEnabled", false); }
    public synchronized boolean biometricEnabled() { return pinEnabled() && state.optBoolean("biometricEnabled", false); }
    public synchronized long waitMillis() { return Math.max(0, state.optLong("lockedUntil", 0) - System.currentTimeMillis()); }

    public synchronized boolean verifyPin(String pin) throws Exception {
        if (!pinEnabled() || waitMillis() > 0) return false;
        byte[] actual = VaultCrypto.pinHash((pin == null ? "" : pin).toCharArray(),
                Base64.decode(state.getString("pinSalt"), Base64.NO_WRAP), state.getString("pinAlgorithm"));
        boolean correct = MessageDigest.isEqual(actual, Base64.decode(state.getString("pinHash"), Base64.NO_WRAP));
        java.util.Arrays.fill(actual, (byte) 0);
        JSONObject next = new JSONObject(state.toString());
        int failures = correct ? 0 : Math.min(1000, next.optInt("pinFailures", 0) + 1);
        next.put("pinFailures", failures).put("lockedUntil", correct ? 0 : System.currentTimeMillis() + VaultCrypto.delayForFailures(failures));
        commit(next); return correct;
    }

    public synchronized void setPin(String pin) throws Exception {
        if (!VaultCrypto.validPin(pin)) throw new IllegalArgumentException("Use de 6 a 12 números");
        String algorithm = "PBKDF2WithHmacSHA256";
        try { SecretKeyFactory.getInstance(algorithm); }
        catch (java.security.NoSuchAlgorithmException error) { algorithm = "PBKDF2WithHmacSHA1"; }
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        byte[] hash = VaultCrypto.pinHash(pin.toCharArray(), salt, algorithm);
        JSONObject next = new JSONObject(state.toString());
        next.put("pinEnabled", true).put("pinAlgorithm", algorithm)
                .put("pinSalt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .put("pinHash", Base64.encodeToString(hash, Base64.NO_WRAP))
                .put("pinFailures", 0).put("lockedUntil", 0);
        java.util.Arrays.fill(hash, (byte) 0); commit(next);
    }

    public synchronized void disablePin() throws Exception {
        JSONObject next = new JSONObject(state.toString());
        next.put("pinEnabled", false).put("biometricEnabled", false).put("pinFailures", 0).put("lockedUntil", 0);
        next.remove("pinHash"); next.remove("pinSalt"); next.remove("pinAlgorithm"); commit(next);
    }

    public synchronized void setBiometric(boolean enabled) throws Exception {
        if (enabled && !pinEnabled()) throw new IllegalStateException("Crie um PIN primeiro");
        JSONObject next = new JSONObject(state.toString()); next.put("biometricEnabled", enabled); commit(next);
    }
}
