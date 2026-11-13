package com.example.domain.usecase

import com.example.data.repository.ProjectRepository
import com.example.domain.model.AppProject
import com.example.domain.model.BuildStatus
import com.example.engine.TermuxEngine
import kotlinx.coroutines.flow.StateFlow

class GetProjectsUseCase(private val repository: ProjectRepository) {
    operator fun invoke(): StateFlow<List<AppProject>> = repository.projects
}

class SaveProjectUseCase(private val repository: ProjectRepository) {
    operator fun invoke(project: AppProject) {
        repository.saveProject(project)
    }
}

class DeleteProjectUseCase(private val repository: ProjectRepository) {
    operator fun invoke(projectId: String) {
        repository.deleteProject(projectId)
    }
}

class GetProjectByIdUseCase(private val repository: ProjectRepository) {
    operator fun invoke(id: String): AppProject? = repository.getProjectById(id)
}

class BuildProjectUseCase(
    private val repository: ProjectRepository,
    private val termuxEngine: TermuxEngine
) {
    suspend fun execute(project: AppProject, buildType: String): Boolean {
        repository.updateBuildStatus(project.id, BuildStatus.BUILDING, null)
        val success = termuxEngine.executeBuildWithProgress(project, buildType)
        val finalStatus = if (success) BuildStatus.SUCCESS else BuildStatus.FAILED
        val tele = termuxEngine.telemetry.value
        val apkPath = if (success) tele.lastOutputPath else null
        repository.updateBuildStatus(
            projectId = project.id,
            status = finalStatus,
            apkPath = apkPath,
            customFileName = tele.lastOutputFileName,
            sizeText = tele.estimatedSizeMb
        )
        return success
    }
}
