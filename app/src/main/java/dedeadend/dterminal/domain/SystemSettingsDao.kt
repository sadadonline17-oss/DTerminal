package dedeadend.dterminal.domain

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemSettingsDao {
    @Query("SELECT * FROM system_settings")
    fun getSettings(): Flow<SystemSettings>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: SystemSettings)
}