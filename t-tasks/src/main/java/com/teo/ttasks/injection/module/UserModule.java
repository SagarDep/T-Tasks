package com.teo.ttasks.injection.module;

import com.teo.ttasks.api.PeopleApi;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.activities.edit_task.EditTaskPresenter;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailPresenter;
import com.teo.ttasks.ui.fragments.task_lists.TaskListsPresenter;
import com.teo.ttasks.ui.fragments.tasks.TasksPresenter;
import com.teo.ttasks.widget.configure.TasksWidgetConfigurePresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class UserModule {

    @Provides
    TasksPresenter provideTasksPresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new TasksPresenter(tasksHelper, prefHelper);
    }

    @Provides
    TaskListsPresenter provideTaskListsPresenter(TasksHelper tasksHelper) {
        return new TaskListsPresenter(tasksHelper);
    }

    @Provides
    MainActivityPresenter provideMainActivityPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, PeopleApi peopleApi) {
        return new MainActivityPresenter(tasksHelper, prefHelper, peopleApi);
    }

    @Provides
    EditTaskPresenter provideEditTaskPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, WidgetHelper widgetHelper) {
        return new EditTaskPresenter(tasksHelper, prefHelper, widgetHelper);
    }

    @Provides
    TaskDetailPresenter provideTaskDetailPresenter(TasksHelper tasksHelper, WidgetHelper widgetHelper) {
        return new TaskDetailPresenter(tasksHelper, widgetHelper);
    }

    // TODO: 2016-07-27 maybe this belongs to another component
    @Provides
    TasksWidgetConfigurePresenter provideTasksWidgetConfigurePresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new TasksWidgetConfigurePresenter(tasksHelper, prefHelper);
    }
}
