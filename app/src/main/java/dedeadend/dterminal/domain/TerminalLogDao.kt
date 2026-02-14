package dedeadend.dterminal.domain

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalLogDao {

    @Query("SELECT * FROM terminal_log ORDER BY id DESC")
    fun getLogs(): Flow<List<TerminalLog>>

    @Insert(onConflict = REPLACE)
    suspend fun insertLog(log: TerminalLog)

    @Query("DELETE FROM terminal_log")
    suspend fun deleteLogs()
}