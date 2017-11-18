package com.teo.ttasks.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.teo.ttasks.data.remote.TasksHelper;

import javax.inject.Inject;

import dagger.android.DaggerBroadcastReceiver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.realm.Realm;
import timber.log.Timber;

public class TaskNotificationReceiver extends DaggerBroadcastReceiver {

    public static final String NOTIFICATION_ID = "notification-id";
    public static final String NOTIFICATION = "notification";

    public static final String TASK_ID = "taskId";

    public static final String ACTION_PUBLISH = "publish";
    public static final String ACTION_COMPLETE = "complete";
    public static final String ACTION_DELETE = "delete";

    @Inject TasksHelper tasksHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final String action = intent.getAction();
        if (action != null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            String taskId = intent.getStringExtra(TASK_ID);
            int id = intent.getIntExtra(NOTIFICATION_ID, 0);
            Realm realm;
            switch (action) {
                case ACTION_PUBLISH:
                    // Display the notification
                    Notification notification = intent.getParcelableExtra(NOTIFICATION);
                    notificationManager.notify(id, notification);
                    break;
                case ACTION_COMPLETE:
                    realm = Realm.getDefaultInstance();
                    // Mark the task as completed
                    tasksHelper.updateCompletionStatus(taskId, realm)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    tTask -> {
                                        Toast.makeText(context, "Task completed", Toast.LENGTH_SHORT).show();
                                        // Update successful, update sync status
                                        realm.executeTransaction(realm1 -> tTask.setSynced(true));
                                        realm.close();
                                        notificationManager.cancel(id);
                                    },
                                    throwable -> {
                                        Timber.e(throwable.toString());
                                        Toast.makeText(context, "Task not found. This is the case if the task was deleted.", Toast.LENGTH_SHORT).show();
                                        realm.close();
                                        notificationManager.cancel(id);
                                    }
                            );
                    break;
                case ACTION_DELETE:
                    // Mark this task's reminder as dismissed
                    realm = Realm.getDefaultInstance();
                    tasksHelper.getTaskAsFlowable(taskId, realm)
                            .firstOrError()
                            .subscribe(
                                    tTask -> {
                                        realm.executeTransaction(realm1 -> tTask.setNotificationDismissed(true));
                                        realm.close();
                                        Timber.d("task marked as dismissed");
                                    },
                                    throwable -> {
                                        Timber.e(throwable.toString());
                                        realm.close();
                                    }
                            );
                    break;
            }
        }
    }
}
