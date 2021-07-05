package com.vasug

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.diagnostics.ProjectBasedReportTask
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer
import org.gradle.api.tasks.diagnostics.internal.TextReportRenderer
import org.gradle.execution.plan.DefaultExecutionPlan
import org.gradle.execution.plan.Node
import org.gradle.internal.graph.GraphRenderer
import org.gradle.internal.logging.text.StyledTextOutput

import static org.gradle.internal.logging.text.StyledTextOutput.Style.Description
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Identifier
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Info
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Normal

abstract class TaskTreeTaskBase extends ProjectBasedReportTask {
    public TextReportRenderer renderer = new TextReportRenderer()

    @Internal
    GraphRenderer graphRenderer

    @Override
    protected ReportRenderer getRenderer() {
        return renderer
    }

    @Override
    protected void generate(final Project project) throws IOException {
        // textOutput is injected and set into renderer by the parent abstract class before this method is called
        StyledTextOutput textOutput = (getRenderer() as TextReportRenderer).textOutput
        graphRenderer = new GraphRenderer(textOutput)

        TaskExecutionGraph executionGraph = project.gradle.taskGraph
        // Getting a private field is possible thanks to groovy not honoring the private modifier
        DefaultExecutionPlan executionPlan = executionGraph.executionPlan
        Set<Node> tasksOfCurrentProject = executionPlan.entryNodes.findAll {
            it.getTask().getProject() == project
        }

        tasksOfCurrentProject.findAll {
            !(it.task.class in TaskTreeTaskBase)
        }.findAll {
            it.hasProperty('task')
        }.each {
            renderTreeRecursive(it, true, textOutput, true, new HashSet<Node>(), 1)
            if (it.dependencySuccessors.isEmpty()) {
                printNoTaskDependencies(textOutput)
            }
            textOutput.println()
        }
        textOutput.println()
    }

    void renderTreeRecursive(Node taskNode, boolean lastChild,
                             final StyledTextOutput textOutput, boolean isFirst, Set<Node> rendered, int depth) {

        final boolean taskSubtreeAlreadyPrinted = !rendered.add(taskNode)
        final Set<Node> children = (taskNode.dependencySuccessors).findAll {
            it.hasProperty('task')
        }

        graphRenderer.visit({ StyledTextOutput styledTextOutput ->
            // print task name
            styledTextOutput.withStyle(isFirst ? Identifier : Normal)
                    .text(taskNode.task.path)

            Gradle currentGradleBuild = project.gradle
            Task refTask = taskNode.task
            if (refTask.project.gradle != currentGradleBuild) {
                styledTextOutput.withStyle(Description)
                        .text(" (included build '" + refTask.project.gradle.rootProject.name + "')")
            }

            if (taskSubtreeAlreadyPrinted) {
                styledTextOutput.text(" *")
            }
        }, lastChild)

        if (!taskSubtreeAlreadyPrinted) {
            // print children tasks
            graphRenderer.startChildren()
            children.eachWithIndex { Node child, int i ->
                this.renderTreeRecursive(child, i == children.size() - 1, textOutput, false, rendered, depth + 1)
            }
            graphRenderer.completeChildren()
        }
    }
    private static void printNoTaskDependencies(StyledTextOutput textOutput) {
        textOutput.withStyle(Info).text("No task dependencies")
        textOutput.println()
    }
}