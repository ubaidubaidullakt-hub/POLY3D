package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects_3d")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val format: String = "GLB",
    val polyCount: Int = 0,
    val vertexCount: Int = 0,
    val molecularParts: Int = 1,
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val sceneJsonData: String = ""
)
