package dedeadend.dterminal.ui.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dedeadend.dterminal.data.Repository
import dedeadend.dterminal.domain.CommandExecutor
import dedeadend.dterminal.domain.History
import dedeadend.dterminal.domain.SystemSettings
import dedeadend.dterminal.domain.TerminalLog
import dedeadend.dterminal.domain.TerminalState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val commandExecutor: CommandExecutor,
    private val ioDispatcher: CoroutineDispatcher,
    private val repository: Repository
) : ViewModel() {

    val systemSettings = repository.getSystemSettings()
        .flowOn(ioDispatcher)
        .stateIn(viewModelScope, SharingStarted.Lazily, SystemSettings())

    val logs = repository.getLogs()
        .flowOn(ioDispatcher)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var state by mutableStateOf(TerminalState.Idle)
        private set

    var toolsMenu by mutableStateOf(false)
        private set

    var isRoot by mutableStateOf(false)
        private set

    var command by mutableStateOf("")
        private set

    init {
        viewModelScope.launch(ioDispatcher) {
            if (repository.getSystemSettings().first().isFirstBoot == true) {
                showWelcomeMessage()
                repository.setFirstBootCompleted()
            }
        }
    }

    fun toggleToolsMenu(show: Boolean) {
        toolsMenu = show
    }

    fun toggleRoot() {
        isRoot = !isRoot
    }

    fun onCommandChange(newCommand: String) {
        command = newCommand
    }

    fun clearOutput() {
        viewModelScope.launch(ioDispatcher) {
            repository.clearLogs()
        }
    }

    fun execute() {
        if (command.trim().isEmpty())
            return
        state = TerminalState.Running
        viewModelScope.launch(ioDispatcher) {
            val cmd = command.trim()
            command = ""
            repository.addHistory(History(cmd))
            repository.addLog(
                TerminalLog(
                    TerminalState.Info,
                    (if (isRoot) "#: " else "$: ") + cmd
                )
            )
            try {
                commandExecutor.execute(cmd, isRoot).collect { log ->
                    repository.addLog(log)
                }
            } catch (e: Exception) {
                repository.addLog(
                    TerminalLog(
                        TerminalState.Error,
                        e.message ?: "Unknown error"
                    )
                )
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    state = TerminalState.Idle
                }
            }
        }
    }

    fun terminate() =
        viewModelScope.launch(ioDispatcher) {
            repository.addLog(commandExecutor.cancel())
        }

    private suspend fun showWelcomeMessage() {
        val welcomeMessage = """
                        
            
             _____  _____                   _              _ 
            |  _  \|_   _|                 (_)            | |
            | |  | | | | ___ _ __ _ __ ___  _ _ __   __ _ | |
            | |  | | | |/ _ \ '__| '_ ` _ \| | '_ \ / _` || |
            | |__/ / | |  __/ |  | | | | | | | | | | (_| || |
            |_____/  \_/\___|_|  |_| |_| |_|_|_| |_|\__,_||_|
                                            
                                                  
                                                              
            😍 I'm finally installed! I was getting bored on the Github.com/dedeadend...
            
            ✨ Execute 'help' command to see DTerminal commands
            
            ☕ Coffee is not included!
            
            💚 Enjoy :)
            
            -------------------------------------------------
            
            """.trimIndent()

        repository.addLog(TerminalLog(TerminalState.Success, welcomeMessage))
    }
}

fun terminalLog2String(terminalLog: TerminalLog): String {
    return if (terminalLog.state == TerminalState.Info)
        "\n\n" + SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(terminalLog.date) + "\n" + terminalLog.message
    else
        terminalLog.message

}