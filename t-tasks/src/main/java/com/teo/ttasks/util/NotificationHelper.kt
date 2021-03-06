package com.teo.ttasks.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.res.ResourcesCompat
import com.teo.ttasks.R
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.receivers.TaskNotificationReceiver
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper(private val context: Context) {

    companion object {
        /** Notification channel ID for reminders */
        const val CHANNEL_REMINDERS = "reminders"
    }

    /**
     * Schedule a notification to show up at the task's reminder date and time.
     * Does nothing if the reminder date doesn't exist or if the task is already completed.
     *
     * @param task task
     * @param id   notification ID
     */
    @JvmOverloads
    fun scheduleTaskNotification(task: Task, id: Int = task.notificationId) {
        if (task.isCompleted || task.reminder == null) {
            return
        }

        val notificationId = if (id != 0) id else task.notificationId

        Timber.d("scheduling notification for %s with id %d", task.id, notificationId)

        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        val resultPendingIntent = TaskStackBuilder.create(context)
                .apply {
                    // Creates an explicit intent for an Activity in your app
                    val resultIntent = TaskDetailActivity.getStartIntent(context, task.id, task.taskListId, true)
                    // Adds the back stack for the Intent (but not the Intent itself)
                    addParentStack(TaskDetailActivity::class.java)
                    // Adds the Intent that starts the Activity to the top of the stack
                    addNextIntent(resultIntent)
                }
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        // Set the "mark as completed" action
        val completedIntent = Intent(context, TaskNotificationReceiver::class.java)
                .apply {
                    putExtra(TaskNotificationReceiver.NOTIFICATION_ID, notificationId)
                    putExtra(TaskNotificationReceiver.TASK_ID, task.id)
                    action = TaskNotificationReceiver.ACTION_COMPLETE
                }
        val completedPendingIntent = PendingIntent.getBroadcast(context, 0, completedIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val completedAction = NotificationCompat.Action(R.drawable.ic_done_white_24dp, "Mark as completed", completedPendingIntent)

        // Create the delete intent
        val deleteIntent = Intent(context, TaskNotificationReceiver::class.java)
                .apply {
                    putExtra(TaskNotificationReceiver.TASK_ID, task.id)
                    action = TaskNotificationReceiver.ACTION_DELETE
                }
        val deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Check if user enabled reminder vibration
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val vibrate = preferences.getBoolean("reminder_vibrate", true)
        val sound = Uri.parse(preferences.getString("reminder_sound", context.getString(R.string.default_reminder_sound)))
        val colorString = preferences.getString("reminder_color", context.getString(R.string.default_led_color))
        val color = if (colorString!!.isEmpty()) NotificationCompat.COLOR_DEFAULT else Color.parseColor(colorString)

        // Create the notification
        val notification = NotificationCompat.Builder(context, "default")
                .setContentTitle(String.format(context.getString(R.string.task_due), task.title))
                .setContentText(context.getString(R.string.notification_more_info))
                .setSmallIcon(R.drawable.ic_assignment_turned_in_24dp)
                .setDefaults(if (vibrate) Notification.DEFAULT_VIBRATE else 0) // enable vibration only if requested
                .setSound(sound)
                .setOnlyAlertOnce(true)
                .setLights(color, 500, 2000)
                .setColor(ResourcesCompat.getColor(context.resources, R.color.colorPrimary, null))
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .addAction(completedAction)
                .setAutoCancel(true)
                .setChannelId(CHANNEL_REMINDERS)
                .build()

        val notificationIntent = Intent(context, TaskNotificationReceiver::class.java)
        notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, notificationId)
        notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION, notification)
        notificationIntent.action = TaskNotificationReceiver.ACTION_PUBLISH
        val pendingIntent = PendingIntent.getBroadcast(context, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Schedule an alarm for the reminder time on the due date
        val alarmDate = Calendar.getInstance()
                .apply {
                    val reminderCal = Calendar.getInstance().apply { time = task.reminder }
                    time = task.due
                    set(Calendar.HOUR_OF_DAY, reminderCal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, reminderCal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, reminderCal.get(Calendar.SECOND))
                }
                .time
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .set(AlarmManager.RTC_WAKEUP, alarmDate.time, pendingIntent)

        val alarmDateString = SimpleDateFormat("dd-MM-yyy HH:mm:ss z", Locale.getDefault()).format(alarmDate)
        Timber.d("Scheduled alarm for task %s at %s", task.id, alarmDateString)
    }

    /**
     * Cancel a notification. Used when deleting a task.
     *
     * @param id notification ID
     */
    fun cancelTaskNotification(id: Int) {
        // Cancel scheduled notification
        val notificationIntent = Intent(context, TaskNotificationReceiver::class.java)
        notificationIntent.action = TaskNotificationReceiver.ACTION_PUBLISH
        val pendingIntent = PendingIntent.getBroadcast(context, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        // Cancel active notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }
}
