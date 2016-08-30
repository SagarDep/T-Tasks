package com.teo.ttasks.ui.fragments.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.base.Presenter;
import com.teo.ttasks.util.RxUtils;

import java.util.concurrent.atomic.AtomicInteger;

import io.realm.Realm;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class TasksPresenter extends Presenter<TasksView> {

    private final TasksHelper tasksHelper;
    private final PrefHelper prefHelper;

    private Subscription tasksSubscription;

    private Realm realm;

    public TasksPresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        this.tasksHelper = tasksHelper;
        this.prefHelper = prefHelper;
    }

    /**
     * Load the tasks associated with the provided task list from the local database.
     *
     * @param taskListId task list identifier
     */
    void getTasks(@Nullable String taskListId) {
        if (taskListId == null)
            return;
        // Since Realm observables do not complete, this subscription must be recreated every time
        if (tasksSubscription != null && !tasksSubscription.isUnsubscribed())
            tasksSubscription.unsubscribe();
        {
            final TasksView view = view();
            if (view != null) view.onTasksLoading();
        }
        tasksSubscription = tasksHelper.getTasks(taskListId, realm)
                .compose(RxUtils.getTaskItems(prefHelper.getHideCompleted()))
                .subscribe(
                        // The Realm observable will not throw errors
                        tasks -> {
                            Timber.d("loaded %d tasks", tasks.size());
                            final TasksView view = view();
                            if (view != null) {
                                if (tasks.isEmpty()) view.showEmptyUi();
                                else view.showContentUi(tasks);
                            }
                        });
        unsubscribeOnUnbindView(tasksSubscription);
    }

    void refreshTasks(@Nullable String taskListId) {
        if (taskListId == null)
            return;
        final Subscription subscription = tasksHelper.refreshTasks(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasksResponse -> { /* ignored since onCompleted does the job, even when the tasks have not been updated */ },
                        throwable -> {
                            Timber.e(throwable.toString());
                            final TasksView view = view();
                            if (view != null) {
                                if (throwable.getCause() instanceof UserRecoverableAuthException)
                                    view.onTasksLoadError(((UserRecoverableAuthException) throwable.getCause()).getIntent());
                                else
                                    view.onTasksLoadError(null);
                            }
                        },
                        () -> {
                            final TasksView view = view();
                            if (view != null) view.onRefreshDone();
                        }
                );
        unsubscribeOnUnbindView(subscription);
    }

    /**
     * Synchronize the local tasks from the specified task list.
     *
     * @param taskListId task list identifier
     */
    void syncTasks(@Nullable String taskListId) {
        if (taskListId == null)
            return;
        // Keep track of the number of synced tasks
        AtomicInteger taskSyncCount = new AtomicInteger(0);
        final Subscription subscription = tasksHelper.syncTasks(taskListId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        syncedTask -> {
                            // Sync successful for this task
                            realm.executeTransaction(realm -> {
                                syncedTask.setSynced(true);
                                // This task is not managed by Realm so it needs to be updated manually
                                realm.insertOrUpdate(syncedTask);
                            });
                            taskSyncCount.incrementAndGet();
                        },
                        throwable -> {
                            // Sync failed for some or all tasks
                            Timber.e(throwable.toString());
                            final TasksView view = view();
                            if (view != null) view.onSyncDone(taskSyncCount.get());
                        },
                        () -> {
                            // Syncing done
                            final TasksView view = view();
                            if (view != null) view.onSyncDone(taskSyncCount.get());
                        }
                );
        unsubscribeOnUnbindView(subscription);
    }

    void setHideCompleted(boolean hideCompleted) {
        prefHelper.setHideCompleted(hideCompleted);
    }

    boolean getHideCompleted() {
        return prefHelper.getHideCompleted();
    }

    @Override
    public void bindView(@NonNull TasksView view) {
        super.bindView(view);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void unbindView(@NonNull TasksView view) {
        super.unbindView(view);
        realm.close();
    }
}
