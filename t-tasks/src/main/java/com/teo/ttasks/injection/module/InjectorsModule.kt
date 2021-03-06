package com.teo.ttasks.injection.module

import com.teo.ttasks.injection.module.activity.*
import com.teo.ttasks.injection.module.fragment.MainFragmentsModule
import com.teo.ttasks.receivers.TaskNotificationReceiver
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.activities.sign_in.SignInActivity
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity
import com.teo.ttasks.widget.TasksWidgetProvider
import com.teo.ttasks.widget.TasksWidgetService
import com.teo.ttasks.widget.configure.TasksWidgetConfigureActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class InjectorsModule {

    /********************************
     * Activities
     ********************************/

    @ContributesAndroidInjector(modules = [SignInActivityModule::class])
    internal abstract fun contributeSignInActivityInjector(): SignInActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class, MainFragmentsModule::class])
    internal abstract fun contributeMainActivityInjector(): MainActivity

    @ContributesAndroidInjector(modules = [TaskDetailActivityModule::class])
    internal abstract fun contributeTaskDetailActivityInjector(): TaskDetailActivity

    @ContributesAndroidInjector(modules = [EditTaskActivityModule::class])
    internal abstract fun contributeEditTaskActivityInjector(): EditTaskActivity

    @ContributesAndroidInjector(modules = [TasksWidgetConfigureActivityModule::class])
    internal abstract fun contributeTasksWidgetConfigureActivityInjector(): TasksWidgetConfigureActivity

    /********************************
     * Services
     ********************************/

    @ContributesAndroidInjector
    internal abstract fun contributeTasksWidgetServiceInjector(): TasksWidgetService

    @ContributesAndroidInjector
    internal abstract fun contributeTasksWidgetProviderInjector(): TasksWidgetProvider

    /********************************
     * BroadcastReceivers
     ********************************/

    @ContributesAndroidInjector
    internal abstract fun contributeTaskNotificationReceiverInjector(): TaskNotificationReceiver
}
