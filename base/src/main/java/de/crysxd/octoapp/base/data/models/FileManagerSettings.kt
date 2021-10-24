package de.crysxd.octoapp.base.data.models

data class FileManagerSettings(
    val hidePrintedFiles: Boolean = false,
    val sortBy: SortBy = SortBy.UploadTime,
    val sortDirection: SortDirection = SortDirection.Descending,
) {
    enum class SortBy {
        UploadTime, PrintTime, FileSize, Name;
    }

    enum class SortDirection {
        Ascending, Descending;
    }
}