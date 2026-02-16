package dedeadend.dterminal.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dedeadend.dterminal.domain.CommandDao
import dedeadend.dterminal.domain.History
import dedeadend.dterminal.domain.Script
import dedeadend.dterminal.domain.SystemSettings
import dedeadend.dterminal.domain.SystemSettingsDao
import dedeadend.dterminal.domain.TerminalLog
import dedeadend.dterminal.domain.TerminalLogDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class Repository @Inject constructor(
    private val commandDao: CommandDao,
    private val terminalLogDao: TerminalLogDao,
    private val systemSettingsDao: SystemSettingsDao,
    private val ioDispatcher: CoroutineDispatcher
) {
    fun getHistory(): Flow<List<History>> = commandDao.getAllHistory()
    fun getScripts(): Flow<List<Script>> = commandDao.getAllScripts()
    fun getLogs(): Flow<List<TerminalLog>> = terminalLogDao.getLogs()
    fun getSystemSettings(): Flow<SystemSettings> = systemSettingsDao.getSettings()

    suspend fun addHistory(command: History) = withContext(ioDispatcher) {
        commandDao.insertHistory(command)
    }

    suspend fun addScript(command: Script) = withContext(ioDispatcher) {
        commandDao.insertScript(command)
    }

    suspend fun addLog(log: TerminalLog) = withContext(ioDispatcher) {
        terminalLogDao.insertLog(log)
    }

    suspend fun restoreHistory(commands: List<History>) = withContext(ioDispatcher) {
        commandDao.insertHistory(commands)
    }

    suspend fun deleteHistoryWithId(id: Int) = withContext(ioDispatcher) {
        commandDao.deleteHistoryById(id)
    }

    suspend fun deleteScriptWithId(id: Int) = withContext(ioDispatcher) {
        commandDao.deleteScriptById(id)
    }

    suspend fun clearHistory() = withContext(ioDispatcher) {
        commandDao.deleteAllHistory()
    }

    suspend fun clearLogs() = withContext(ioDispatcher) {
        terminalLogDao.deleteLogs()
    }

    suspend fun setFirstBootCompleted() = withContext(ioDispatcher) {
        val currentSettings = getSystemSettings().first()
        systemSettingsDao.updateSettings(currentSettings.copy(isFirstBoot = false))
    }

    suspend fun setLogSuccessFontColor(r: Int, g: Int, b: Int) = withContext(ioDispatcher) {
        val currentSettings = getSystemSettings().first()
        systemSettingsDao.updateSettings(
            currentSettings.copy(logSuccessFontColor = Color(r, g, b).toArgb())
        )
    }

    suspend fun setLogErrorFontColor(r: Int, g: Int, b: Int) = withContext(ioDispatcher) {
        val currentSettings = getSystemSettings().first()
        systemSettingsDao.updateSettings(
            currentSettings.copy(logErrorFontColor = Color(r, g, b).toArgb())
        )
    }

    suspend fun setLogInfoFontColor(r: Int, g: Int, b: Int) = withContext(ioDispatcher) {
        val currentSettings = getSystemSettings().first()
        systemSettingsDao.updateSettings(
            currentSettings.copy(logInfoFontColor = Color(r, g, b).toArgb())
        )
    }

    suspend fun setLogFontSize(size: Int) = withContext(ioDispatcher){
        val currentSettings = getSystemSettings().first()
        systemSettingsDao.updateSettings(currentSettings.copy(logFontSize = size))
    }
}