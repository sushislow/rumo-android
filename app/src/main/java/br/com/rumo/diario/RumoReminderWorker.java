package br.com.rumo.diario;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Lembretes estritamente locais. Nenhum texto do diário é lido ou transmitido. */
public final class RumoReminderWorker extends Worker {
    private static final String PREFS = "rumo_lembretes_android";
    private static final String CHANNEL = "rumo_pausas_v1";
    private static final String JOB_PREFIX = "rumo_daily_pause_";
    private static final String[] DEFAULT_TIMES = {"08:30", "14:00", "20:30"};
    private static final Pattern TIME_PATTERN = Pattern.compile("(?:[01]\\d|2[0-3]):[0-5]\\d");
    private static final String[] TEXTS = {
        "Seu próximo passo pode ser pequeno. Como você está agora?",
        "Uma pausa pode ajudar. Escolha o que você precisa neste momento.",
        "Cuidar de si também é perceber o que funciona."
    };

    public RumoReminderWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    public static boolean timesValid(String[] times, int count) {
        if (times == null || times.length != 3) return false;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < times.length; i++) {
            String time = times[i];
            if (time == null || !TIME_PATTERN.matcher(time).matches()) return false;
            if (i < count && !seen.add(time)) return false;
        }
        return true;
    }

    public static void configure(Context context, int count, String[] times) {
        if (count < 0 || count > 3 || !timesValid(times, count)) return;
        Context app = context.getApplicationContext();
        SharedPreferences.Editor edit = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        edit.putInt("count", count);
        for (int i = 0; i < 3; i++) edit.putString("time" + i, times[i]);
        edit.apply();
        WorkManager manager = WorkManager.getInstance(app);
        for (int i = 0; i < 3; i++) {
            if (i < count) schedule(app, manager, i, times[i]);
            else manager.cancelUniqueWork(JOB_PREFIX + i);
        }
    }

    public static void refreshIfEnabled(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int count = pref.getInt("count", 0);
        if (count < 1 || count > 3) return;
        String[] times = new String[3];
        for (int i = 0; i < 3; i++) times[i] = pref.getString("time" + i, DEFAULT_TIMES[i]);
        if (timesValid(times, count)) configure(context, count, times);
    }

    private static void schedule(Context context, WorkManager manager, int slot, String time) {
        String[] components = time.split(":");
        int hh = Integer.parseInt(components[0]), mm = Integer.parseInt(components[1]);
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hh);
        target.set(Calendar.MINUTE, mm);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (target.getTimeInMillis() <= System.currentTimeMillis()) target.add(Calendar.DAY_OF_YEAR, 1);
        long delay = Math.max(0, target.getTimeInMillis() - System.currentTimeMillis());
        Data input = new Data.Builder().putInt("slot", slot).build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(RumoReminderWorker.class, 24, TimeUnit.HOURS)
                .setInputData(input)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        manager.enqueueUniquePeriodicWork(JOB_PREFIX + slot, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        int slot = getInputData().getInt("slot", -1);
        int count = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("count", 0);
        if (slot < 0 || slot >= count || slot >= 3) return Result.success();
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return Result.success();
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(CHANNEL, "Pausas do Rumo", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Lembretes discretos escolhidos por você");
                channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                channel.enableVibration(false);
                manager.createNotificationChannel(channel);
            }
            if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) return Result.success();
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("rumo_open_tools", true);
            PendingIntent open = PendingIntent.getActivity(context, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= 26) builder = new Notification.Builder(context, CHANNEL);
            else builder = new Notification.Builder(context).setPriority(Notification.PRIORITY_LOW);
            Notification notice = builder
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Um momento para você")
                    .setContentText(TEXTS[(slot + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) % TEXTS.length])
                    .setAutoCancel(true)
                    .setContentIntent(open)
                    .setVisibility(Notification.VISIBILITY_SECRET)
                    .build();
            manager.notify(4100 + slot, notice);
        } catch (SecurityException ignored) {
            // Permissão alterada enquanto o Worker estava em execução.
        }
        return Result.success();
    }
}
