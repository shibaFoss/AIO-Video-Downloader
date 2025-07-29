package app.core.engines.services;

import static androidx.core.app.NotificationCompat.Builder;
import static app.core.AIOApp.INSTANCE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.aio.R;

import app.ui.main.MotherActivity;

/**
 * AIOForegroundService is a persistent foreground service that ensures
 * background operations can continue without being interrupted by the Android system.
 *
 * This service shows a silent notification and restarts automatically if destroyed.
 */
public class AIOForegroundService extends Service {

    private static final String CHANNEL_ID = "AIO System Service";
    private static final int NOTIFICATION_ID = 1430;

    private Notification notification;
    private boolean isServiceRunning = false;
    private static AIOForegroundService instance;

    /**
     * Returns the current instance of the service.
     * @return singleton instance of AIOForegroundService, or null if not yet initialized.
     */
    public static AIOForegroundService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotification();
        startForeground(NOTIFICATION_ID, notification);
        isServiceRunning = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Binding not used in this service.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Keeps service running until explicitly stopped.
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        isServiceRunning = false;

        // Restart service automatically if killed by the system.
        sendBroadcast(new Intent(this, RestartIdleServiceReceiver.class));
    }

    /**
     * Returns whether the foreground service is currently running.
     */
    public boolean isIsServiceRunning() {
        return this.isServiceRunning;
    }

    /**
     * Stops the service and removes the foreground notification.
     */
    public void stopService() {
        Intent serviceIntent = new Intent(INSTANCE, AIOForegroundService.class);
        INSTANCE.stopService(serviceIntent);
        isServiceRunning = false;
    }

    /**
     * Starts the service only if it is not already running.
     */
    public void updateService() {
        try {
            if (!isServiceRunning) {
                isServiceRunning = true;
                Intent service = new Intent(INSTANCE, AIOForegroundService.class);
                INSTANCE.startForegroundService(service);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /**
     * Forcefully starts the service even if it's already running.
     */
    public void forceStartService() {
        Intent serviceIntent = new Intent(INSTANCE, AIOForegroundService.class);
        INSTANCE.startForegroundService(serviceIntent);
        isServiceRunning = true;
    }

    /**
     * Creates a notification channel and prepares the foreground notification.
     */
    private void createNotification() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.text_aio_system_service),
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
        notification = getCustomNotification();
    }

    /**
     * Builds a custom persistent notification that links back to the main activity.
     * @return a Notification object to be shown in the foreground.
     */
    private Notification getCustomNotification() {
        Intent notificationIntent = new Intent(this, MotherActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        return new Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.title_aio_video_downloader))
                .setContentText(getString(R.string.text_app_running_in_the_background))
                .setSmallIcon(R.drawable.ic_launcher_logo_v4)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .build();
    }
}