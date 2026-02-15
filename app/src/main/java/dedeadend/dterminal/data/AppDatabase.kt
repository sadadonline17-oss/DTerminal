package dedeadend.dterminal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import dedeadend.dterminal.domain.CommandDao
import dedeadend.dterminal.domain.History
import dedeadend.dterminal.domain.Script
import dedeadend.dterminal.domain.SystemSettings
import dedeadend.dterminal.domain.SystemSettingsDao
import dedeadend.dterminal.domain.TerminalLog
import dedeadend.dterminal.domain.TerminalLogDao

@Database(
    entities = [TerminalLog::class, History::class, Script::class, SystemSettings::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao

    abstract fun terminalLogDao(): TerminalLogDao

    abstract fun systemSettingsDao(): SystemSettingsDao
}
