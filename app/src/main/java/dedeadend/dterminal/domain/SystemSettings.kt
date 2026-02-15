package dedeadend.dterminal.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_settings")
data class SystemSettings(
    val isFirstBoot: Boolean = true,
    val logFontColor: Int = -1,
    val logFontSize: Int = 12,
    @PrimaryKey val id: Int = 1
)