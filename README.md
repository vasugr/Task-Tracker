# Task-Tracker
A lightweight tool which displays task graph and time taken by tasks in a gradle project

## Features
1. Shows execution times of all the tasks sorted in descending order and reports it at the end of the build in the console
2. For a given task, it shows the task dependencies and lets you visualize relatively complex task dependencies in the form of a tree. The report is stored as a text file in the root project folder which shows the tree for the whole project and it also stores task graph of every child project in its respective directory

## Installation
* The plugin can be configured in the [build script](https://gradle.org/docs/current/userguide/plugins.html).
  In `build.gradle` file, add the following plugin id and version no
```groovy
plugins {
    id "com.github.vasugr.tasktracker" version "0.0.1"
}
```

## Usage

* Add `task-graph` next to all the tasks for which you want the report
* `gradle <task name> task-graph`
* In a multi-project, apply the plugin configuration for the root project only, it will be automatically added  to all the child projects


### Example

`gradle build task-graph`
```
:build
+--- :assemble
|    \--- :jar
|         \--- :classes
|              +--- :compileJava
|              \--- :processResources
\--- :check
     \--- :test
          +--- :classes
          |    +--- :compileJava
          |    \--- :processResources
          \--- :testClasses
               +--- :compileTestJava
               |    \--- :classes
               |         +--- :compileJava
               |         \--- :processResources
               \--- :processTestResources