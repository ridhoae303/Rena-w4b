package com.rena.w4b;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.ArrayList;

public class DownloadService extends Service {
    private static final String CHANNEL_PROGRESS = "rena_downloads_progress";
    private static final String CHANNEL_DONE = "rena_downloads_done";
    private static final int NOTIFICATION_ID = 94021;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean foreground;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(RenaLocale.wrap(base));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RenaDownloadManager.initialize(this);
        ensureChannels(this);
        Notification notification = buildProgress();
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            foreground = true;
        } catch (Throwable ignored) {
            foreground = false;
        }
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 700L);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    private void update() {
        if (RenaDownloadManager.activeCount() == 0) {
            try {
                if (Build.VERSION.SDK_INT >= 24) {
                    stopForeground(STOP_FOREGROUND_REMOVE);
                } else {
                    stopForeground(true);
                }
            } catch (Throwable ignored) {
            }
            foreground = false;
            stopSelf();
            return;
        }
        try {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, buildProgress());
            }
        } catch (Throwable ignored) {
        }
        handler.postDelayed(tick, 800L);
    }

    private Notification buildProgress() {
        ArrayList<RenaDownloadManager.DownloadItem> items = RenaDownloadManager.getItems();
        int active = 0;
        long downloaded = 0L;
        long total = 0L;
        boolean indeterminate = false;
        long speed = 0L;
        String firstName = null;
        for (RenaDownloadManager.DownloadItem item : items) {
            if (item.status == RenaDownloadManager.RUNNING || item.status == RenaDownloadManager.QUEUED) {
                active++;
                downloaded += item.downloaded;
                speed += item.speed;
                if (item.total > 0) {
                    total += item.total;
                } else {
                    indeterminate = true;
                }
                if (firstName == null) {
                    firstName = item.fileName;
                }
            }
        }
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL_PROGRESS);
        } else {
            builder = new Notification.Builder(this);
        }
        String title = active <= 1
                ? (firstName == null ? getString(R.string.download_running_text) : firstName)
                : getString(R.string.download_notification_multiple, active);
        StringBuilder text = new StringBuilder();
        if (total > 0 && !indeterminate) {
            text.append(Math.min(100, (int) (downloaded * 100L / total))).append("% \u00B7 ");
        }
        text.append(DownloadFormat.size(downloaded));
        if (speed > 0) {
            text.append(" \u00B7 ").append(DownloadFormat.size(speed)).append("/s");
        }
        builder.setContentTitle(title);
        builder.setContentText(text.toString());
        builder.setSmallIcon(android.R.drawable.stat_sys_download);
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);
        builder.setContentIntent(openDownloads(this));
        if (total > 0 && !indeterminate) {
            builder.setProgress(1000, (int) Math.min(1000L, downloaded * 1000L / total), false);
        } else {
            builder.setProgress(0, 0, true);
        }
        return builder.build();
    }

    private static PendingIntent openDownloads(Context context) {
        Intent intent = new Intent(context, DownloadActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 7, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            if (manager == null) {
                return;
            }
            Context localized = RenaLocale.wrap(context);
            NotificationChannel progress = new NotificationChannel(CHANNEL_PROGRESS,
                    localized.getString(R.string.download_channel_progress), NotificationManager.IMPORTANCE_LOW);
            progress.setSound(null, null);
            progress.enableVibration(false);
            manager.createNotificationChannel(progress);
            NotificationChannel done = new NotificationChannel(CHANNEL_DONE,
                    localized.getString(R.string.download_channel_done), NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(done);
        } catch (Throwable ignored) {
        }
    }

    static void notifyCompleted(Context context, RenaDownloadManager.DownloadItem item) {
        if (context == null || item == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
            ensureChannels(context);
            Context localized = RenaLocale.wrap(context);
            NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            if (manager == null) {
                return;
            }
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= 26) {
                builder = new Notification.Builder(context, CHANNEL_DONE);
            } else {
                builder = new Notification.Builder(context);
            }
            builder.setContentTitle(item.fileName);
            builder.setContentText(localized.getString(R.string.download_completed_text) + " \u00B7 " +
                    DownloadFormat.size(item.downloaded));
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
            builder.setAutoCancel(true);
            builder.setContentIntent(openDownloads(context));
            manager.notify(item.id.hashCode(), builder.build());
        } catch (Throwable ignored) {
        }
    }
}
