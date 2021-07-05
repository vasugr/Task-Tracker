package com.vasug

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import java.util.concurrent.ConcurrentHashMap

class TimeRecorder implements TaskExecutionListener, BuildListener{

    private List<TaskInfo> taskInfoList = []
    private BuildTrackerPlugin gradleTaskRecorder
    private float sum = 0
    private float max = 0
    final Map<String, Long> taskTimingMap = new ConcurrentHashMap<>()

    TimeRecorder(BuildTrackerPlugin gradleTaskRecorder) {
        this.gradleTaskRecorder = gradleTaskRecorder
    }


    @Override
    void settingsEvaluated(Settings settings) {

    }

    @Override
    void projectsLoaded(Gradle gradle) {

    }

    @Override
    void projectsEvaluated(Gradle gradle) {

    }

    @Override
    void buildFinished(BuildResult result) {
        taskInfoList.sort{-it.time}
        println "\n===================TIME TAKEN BY TASKS=====================\n"

        for (TaskInfo taskInfo : taskInfoList){
            def finalStr = "\t$taskInfo.time ms\t\t| $taskInfo.path "

            if (taskInfo.isSuccess){
                finalStr = finalStr + " " + '\033[32m' + "[ Success ]" + '\033[0m'
            }else{
                finalStr = finalStr + " " + '\033[31m' + "[ Failure ]" + '\033[0m'
            }
            println finalStr
        }
    }

    @Override
    void beforeExecute(Task task) {
        taskTimingMap.put(task.getPath(),System.currentTimeMillis())
    }

    @Override
    void afterExecute(Task task, TaskState state) {
        long taskTime = System.currentTimeMillis() - taskTimingMap.get(task.getPath())

        taskInfoList << new TaskInfo(taskTime,
                                task.getPath(),
                                state.getSkipped(),
                                state.getFailure() == null
                             )
        if (max < taskTime){
            max = taskTime
        }
        sum += taskTime
    }
}