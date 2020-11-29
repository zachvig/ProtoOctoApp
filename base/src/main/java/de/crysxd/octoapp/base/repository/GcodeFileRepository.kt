package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.datasource.GcodeFileDataSourceGroup
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.flow

class GcodeFileRepository(
    private val dataSources: GcodeFileDataSourceGroup
) {
    fun loadFile(file: FileObject.File) = dataSources.dataSources.firstOrNull {
        it.canLoadFile(file)
    }?.loadFile(file) ?: flow {
        emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("No data source can load file")))
    }
}