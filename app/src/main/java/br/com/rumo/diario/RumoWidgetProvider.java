package br.com.rumo.diario;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Only explicitly enabled aggregates are stored here; never diary entries or notes. */
public final class RumoWidgetProvider extends AppWidgetProvider {
    private static final String PREFS = "rumo_widget_v1";

    public static void sync(Context context, String summary) throws Exception {
        JSONObject json = new JSONObject(summary);
        boolean enabled = json.optBoolean("enabled", false);
        String mode = json.optString("mode", "total");
        if (!mode.equals("total") && !mode.equals("registered") && !mode.equals("sequence")) mode = "total";
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear();
        editor.putBoolean("enabled", enabled);
        if (enabled) {
            editor.putString("mode", mode).putInt("count", Math.max(0, Math.min(365000, json.optInt("count", 0))));
            String date = json.optString("asOf", "");
            if (date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) editor.putString("asOf", date);
            editor.putBoolean("pending", json.optBoolean("pending", false));
        }
        if (!editor.commit()) throw new IllegalStateException("Falha ao atualizar widget");
        updateAll(context);
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit(); updateAll(context);
    }
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int id : manager.getAppWidgetIds(new ComponentName(context, RumoWidgetProvider.class))) update(context, manager, id);
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) updateAll(context);
    }
    public static boolean requestAdd(Context context) {
        if (Build.VERSION.SDK_INT < 26) return false;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        return manager.isRequestPinAppWidgetSupported()
                && manager.requestPinAppWidget(new ComponentName(context, RumoWidgetProvider.class), null, null);
    }
    private static void update(Context context, AppWidgetManager manager, int id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("enabled", false);
        String mode = prefs.getString("mode", "total"), asOf = prefs.getString("asOf", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        boolean staleSequence = mode.equals("sequence") && !today.equals(asOf);
        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.rumo_widget);
        view.setTextViewText(R.id.widget_number, enabled && !staleSequence ? String.valueOf(prefs.getInt("count", 0)) : "—");
        String label = !enabled ? "Contagem oculta" : staleSequence ? "Abra para atualizar" : mode.equals("registered") ? "dias registrados"
                : mode.equals("sequence") ? "dias em sequência" : "dias de progresso";
        view.setTextViewText(R.id.widget_label, label);
        String detail = !enabled ? "Ative em Dados > Widget" : prefs.getBoolean("pending", false) ? "Há registro pendente" : asOf.isEmpty() ? "Um passo de cada vez" : "Atualizado: " + asOf.substring(8) + "/" + asOf.substring(5, 7);
        view.setTextViewText(R.id.widget_detail, detail);
        Intent intent = new Intent(context, MainActivity.class).setAction("br.com.rumo.diario.CHECKIN_WIDGET")
                .putExtra("rumo_open_checkin", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        view.setOnClickPendingIntent(R.id.widget_root, open);
        view.setOnClickPendingIntent(R.id.widget_button, open);
        manager.updateAppWidget(id, view);
    }
}
