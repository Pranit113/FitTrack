package com.fittrack.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class ActivityWithRoutePoints(
    @Embedded val activity: ActivityEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "activityId"
    )
    val routePoints: List<RoutePointEntity>
)

@Dao
interface ActivityDao {
    @Insert
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Insert
    suspend fun insertRoutePoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): ActivityEntity?

    @Query("SELECT * FROM route_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    suspend fun getRoutePoints(activityId: Long): List<RoutePointEntity>

    @Query("SELECT * FROM activities WHERE startTime >= :fromTime ORDER BY startTime DESC")
    fun getActivitiesSince(fromTime: Long): Flow<List<ActivityEntity>>

    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()

    @Query("DELETE FROM route_points")
    suspend fun deleteAllRoutePoints()

    @Transaction
    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityWithRoutePoints(id: Long): ActivityWithRoutePoints?
}
