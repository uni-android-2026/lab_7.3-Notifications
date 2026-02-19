package su.ioplock.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    public static final int NOTIFICATION_ID = 1;
    public static final int NOTIFICATION_PROGRESS_ID = 2;

    private static final String CHANNEL_ID = "main_channel";
    private static final int REQ_POST_NOTIFICATIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        createNotificationChannelIfNeeded();
        requestNotificationPermissionIfNeeded();
    }

    public void onBellClick(View view) {
        if (!hasNotificationPermission()) {
            askNotificationPermission();
            return;
        }

        PendingIntent contentIntent = buildContentPendingIntent();

        // Большая иконка: лучше PNG/JPG (BitmapFactory для vector может не подойти)
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.bell_large);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle("Колокольчик")
                .setContentText("Колокольчик звенит, колокольчик зовет")
                .setContentIntent(contentIntent)

                // Из картинок:
                .setAutoCancel(true) // удалять после нажатия
                .setLargeIcon(largeIcon) // большая иконка
                .setPriority(NotificationCompat.PRIORITY_HIGH) // высокий приоритет (для < Android 8)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // звук/вибрация/свет
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // на экране блокировки видно полностью

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
    }

    public void onProgressClick(View view) {
        if (!hasNotificationPermission()) {
            askNotificationPermission();
            return;
        }

        PendingIntent contentIntent = buildContentPendingIntent();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle("Колокольчик")
                .setContentText("Подождите, идет загрузка...")
                .setContentIntent(contentIntent)

                // Уведомление с процессом (как в примере на картинке)
                .setProgress(0, 0, true) // indeterminate progress
                .setOngoing(true)        // «висит», пока не обновишь/удалишь
                .setOnlyAlertOnce(true)  // при повторном notify не будет каждый раз шуметь

                // Доп. параметры (можно оставить, чтобы выглядело одинаково)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Повторное нажатие обновит это же уведомление, а не создаст новое
        NotificationManagerCompat.from(this).notify(NOTIFICATION_PROGRESS_ID, builder.build());
    }

    private PendingIntent buildContentPendingIntent() {
        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return stackBuilder.getPendingIntent(0, flags);
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Main notifications";
            String description = "Notifications channel for demo app";
            int importance = NotificationManager.IMPORTANCE_HIGH; // чтобы «HIGH» работал и на Android 8+

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "Разрешите уведомления для приложения.", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Уведомления разрешены.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Без разрешения уведомления показываться не будут.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
