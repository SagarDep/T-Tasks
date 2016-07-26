package com.teo.ttasks.ui.activities.task_detail;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.databinding.ActivityTaskDetailBinding;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.util.AnimUtils.TaskDetailAnim;

import javax.inject.Inject;

public class TaskDetailActivity extends AppCompatActivity implements TaskDetailView {

    private static final String EXTRA_TASK_ID = "taskId";
    private static final String EXTRA_TASK_LIST_ID = "taskListId";

    @Inject TaskDetailPresenter mTaskDetailPresenter;

    private ActivityTaskDetailBinding mBinding;

    private String taskId;
    private String taskListId;

    public static void start(Context context, String taskId, String taskListId, @Nullable Bundle bundle) {
        Intent starter = new Intent(context, TaskDetailActivity.class);
        starter.putExtra(EXTRA_TASK_ID, taskId);
        starter.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        context.startActivity(starter, bundle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(this).userComponent().inject(this);
        mTaskDetailPresenter.bindView(this);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_task_detail);

        enterAnimation();

        // Add a context menu to the task header
        registerForContextMenu(mBinding.taskHeader);

        // Get the task
        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        taskListId = getIntent().getStringExtra(EXTRA_TASK_LIST_ID);
        mTaskDetailPresenter.getTask(taskId);
        mTaskDetailPresenter.getTaskList(taskListId);
    }

    @Override
    public void onTaskLoaded(Task task) {
        mBinding.setTask(task);
    }

    @Override
    public void onTaskLoadError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onTaskListLoaded(TaskList taskList) {
        mBinding.setTaskList(taskList);
    }

    @Override
    public void onTaskListLoadError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onTaskUpdated() {
        onBackPressed();
    }

    @Override
    public void onTaskUpdateError() {
        // TODO: 2016-07-25 implement
    }

    @Override
    public void onTaskDeleted() {
        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
        onBackPressed();
    }

    public void onFabClicked(View v) {
        mTaskDetailPresenter.updateCompletionStatus(taskListId);
    }

    public void onBackClicked(View v) {
        onBackPressed();
    }

    public void onEditClicked(View v) {
        EditTaskActivity.startEdit(this, taskId, taskListId, null);
    }

    public void onOverflowClicked(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setGravity(Gravity.END);
        popup.inflate(R.menu.menu_task_detail);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.delete:
                    DialogInterface.OnClickListener dialogClickListener = (dialog, choice) -> {
                        switch (choice) {
                            case DialogInterface.BUTTON_POSITIVE:
                                mTaskDetailPresenter.deleteTask(taskListId, taskId);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };
                    new AlertDialog.Builder(this)
                            .setMessage("Delete this task?")
                            .setPositiveButton(android.R.string.yes, dialogClickListener)
                            .setNegativeButton(android.R.string.no, dialogClickListener)
                            .show();
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void enterAnimation() {
        ViewPropertyAnimatorCompat backAnimator = TaskDetailAnim.animate(mBinding.back);
        ViewPropertyAnimatorCompat editAnimator = TaskDetailAnim.animate(mBinding.edit);
        ViewPropertyAnimatorCompat moreAnimation = TaskDetailAnim.animate(mBinding.more);
        ViewPropertyAnimatorCompat titleAnimator = TaskDetailAnim.animate(mBinding.taskTitle);
        ViewPropertyAnimatorCompat taskListTitleAnimator = TaskDetailAnim.animate(mBinding.taskListTitle);
        ViewPropertyAnimatorCompat fabAnimator = ViewCompat.animate(mBinding.fab)
                .scaleX(1.0F)
                .scaleY(1.0F)
                .alpha(1.0F)
                .setStartDelay(400)
                .setInterpolator(new OvershootInterpolator())
                .withLayer();
        backAnimator.start();
        editAnimator.start();
        moreAnimation.start();
        titleAnimator.start();
        taskListTitleAnimator.start();
        fabAnimator.start();
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBinding.taskHeader.setTransitionName(null);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTaskDetailPresenter.unbindView(this);
    }
}
