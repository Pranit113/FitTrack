package com.fittrack.app.data.repository

import com.fittrack.app.data.db.ActivityDao
import com.fittrack.app.data.db.ActivityEntity
import com.fittrack.app.data.db.RoutePointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class PersonalBests(
    val longestDistanceActivity: ActivityEntity?,
    val fastestPaceActivity: ActivityEntity?,
    val longestDurationActivity: ActivityEntity?
)

@Singleton
class ActivityRepository @Inject constructor(private val dao: ActivityDao) {
    fun getAllActivities(): Flow<List<ActivityEntity>> = dao.getAllActivities()
    
    fun getActivitiesSince(fromTime: Long) = dao.getActivitiesSince(fromTime)
    
    suspend fun saveActivity(activity: ActivityEntity, routePoints: List<RoutePointEntity>): Long {
        val id = dao.insertActivity(activity)
        val routePointsWithId = routePoints.map { it.copy(activityId = id) }
        dao.insertRoutePoints(routePointsWithId)
        return id
    }
    
    suspend fun getActivityById(id: Long) = dao.getActivityById(id)
    
    suspend fun getActivityWithRoute(id: Long) = dao.getActivityWithRoutePoints(id)
    
    suspend fun deleteActivity(activity: ActivityEntity) = dao.deleteActivity(activity)
    
    suspend fun deleteAllData() {
        dao.deleteAllRoutePoints()
        dao.deleteAllActivities()
    }
    
    suspend fun getPersonalBests(): PersonalBests {
        val activities = dao.getAllActivities().first()
        return PersonalBests(
            longestDistanceActivity = activities.maxByOrNull { it.distanceMeters },
            fastestPaceActivity = activities.filter { it.avgPaceSecPerKm > 0.0 }.minByOrNull { it.avgPaceSecPerKm },
            longestDurationActivity = activities.maxByOrNull { it.durationSeconds }
        )
    }
}
