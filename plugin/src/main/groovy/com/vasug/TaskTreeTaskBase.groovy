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
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Normal

abstract class TaskTreeTaskBase extends ProjectBasedReportTask {
    @Internal
    String outFilepath = project.projectDir.toString() + "/task_graph.txt"
    @Internal
    File outputGraphFile = new File(outFilepath)

    @Internal
    String rootFilepath = project.rootDir.toString() + "/final_task_graph.txt"
    @Internal
    File rootGraphFile = new File(rootFilepath)

    public TextReportRenderer renderer = new TextReportRenderer()

    @Internal
    GraphRenderer graphRenderer

    @Override
    protected ReportRenderer getRenderer() {
        return renderer
    }
    void saveToRoot(String projName,File inputFile)
    {
        String val="";
        val += "\n------------------------------------------------------\n"
        val += "\t"+projName
        val += "\n------------------------------------------------------\n"
        String fileContent = inputFile.text
        val += fileContent + "\n"
        rootGraphFile.append(val)
    }

    @Override
    protected void generate(final Project project) throws IOException {
        TextReportRenderer textRenderer = (getRenderer() as TextReportRenderer)
        textRenderer.setOutputFile(outputGraphFile)
        StyledTextOutput textOutput = textRenderer.textOutput
        graphRenderer = new GraphRenderer(textOutput)
        //println(" filepath == "+outputGraphFile)

        TaskExecutionGraph executionGraph = project.gradle.taskGraph
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

           /*println("\n-->")
           textOutput.println()
            println("<--\n")*/
        }
        /*println("\n==>")
        textOutput.println()
        println("<==\n")*/
        saveToRoot(project.name, outputGraphFile)
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

}