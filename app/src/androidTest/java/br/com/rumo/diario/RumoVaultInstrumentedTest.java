package br.com.rumo.diario;

import android.content.Context;
import android.content.ContextWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import static org.junit.Assert.*;

/** Uses a separate directory; never edits the installed user's real journal. */
@RunWith(AndroidJUnit4.class)
public final class RumoVaultInstrumentedTest {
    private Context context;
    private File folder;
    private static final String JOURNAL = "rumo_diario_v1";
    @Before public void setUp() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        folder = new File(target.getNoBackupFilesDir(), "vault-test-" + UUID.randomUUID());
        assertTrue(folder.mkdirs());
        context = new ContextWrapper(target) { @Override public File getNoBackupFilesDir() { return folder; } };
    }
    @Test public void migrationAndAtomicRoundTrip() throws Exception {
        RumoVault vault = new RumoVault(context);
        String journal = "{\"2026-10-26\":{\"status\":\"done\",\"note\":\"confidential-marker-123\"}}";
        vault.migrate(new JSONObject().put(JOURNAL, journal).toString());
        assertEquals(journal, new RumoVault(context).get(JOURNAL));
        vault.migrate(new JSONObject().put(JOURNAL, "stale legacy copy").toString());
        assertEquals(journal, vault.get(JOURNAL));
        try { vault.putBundle(new JSONObject().put(JOURNAL,"overwrite").put("invalid-key","x").toString()); fail("Invalid bundle accepted"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(journal, new RumoVault(context).get(JOURNAL));
        vault.putBundle(new JSONObject().put("rumo_preferencias_v1","{\"theme\":\"violeta\"}")
                .put("rumo_conquistas_v1","{\"version\":1,\"unlocked\":{}}").toString());
        assertTrue(new RumoVault(context).get("rumo_preferencias_v1").contains("violeta"));
        byte[] bytes=Files.readAllBytes(new File(folder,"rumo-vault.enc").toPath());
        assertFalse(new String(bytes,StandardCharsets.ISO_8859_1).contains("confidential-marker-123"));
    }
    @Test public void pinPersistsAndDisablingPreservesJournal() throws Exception {
        RumoVault vault=new RumoVault(context);vault.put(JOURNAL,"private-data");vault.setPin("004812");
        vault=new RumoVault(context);assertTrue(vault.pinEnabled());assertFalse(vault.verifyPin("004813"));assertTrue(vault.verifyPin("004812"));
        vault.setBiometric(true);assertTrue(new RumoVault(context).biometricEnabled());
        vault.disablePin();vault=new RumoVault(context);assertFalse(vault.pinEnabled());assertFalse(vault.biometricEnabled());
        assertEquals("private-data",vault.get(JOURNAL));
    }
    @Test public void attemptBackoffSurvivesReopen() throws Exception {
        RumoVault vault=new RumoVault(context);vault.setPin("004812");
        for(int i=0;i<5;i++)assertFalse(vault.verifyPin("004813"));
        assertTrue(vault.waitMillis()>0);vault=new RumoVault(context);assertTrue(vault.waitMillis()>0);
        assertFalse(vault.verifyPin("004812"));
    }
    @Test public void tamperedVaultIsRejectedWithoutOverwritingIt() throws Exception {
        RumoVault vault=new RumoVault(context);vault.put(JOURNAL,"private-data");
        File file=new File(folder,"rumo-vault.enc");byte[] bytes=Files.readAllBytes(file.toPath());bytes[bytes.length-1]^=1;
        Files.write(file.toPath(),bytes);
        try { new RumoVault(context);fail("Tampered vault was opened"); } catch(Exception expected) { }
        assertArrayEquals(bytes,Files.readAllBytes(file.toPath()));
    }
}
