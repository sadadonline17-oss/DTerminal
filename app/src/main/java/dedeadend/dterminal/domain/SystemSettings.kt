package dedeadend.dterminal.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_settings")
data class SystemSettings(
    val isFirstBoot: Boolean = true,
    @PrimaryKey val id: Int = 1
)