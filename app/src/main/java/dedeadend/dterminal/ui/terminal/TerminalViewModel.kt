package dedeadend.dterminal.ui.terminal

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dedeadend.dterminal.data.CommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val commandExecutor: CommandExecutor
) : ViewModel() {

    var isRunning by mutableStateOf(false)
        private set

    var toolsMenu by mutableStateOf(false)
        private set

    var isRoot by mutableStateOf(false)
        private set

    var command by mutableStateOf("")
        private set

    private val _output = mutableStateListOf<String>()
    val output: List<String> = _output


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
        _output.clear()
    }

    fun execute() {
        viewModelScope.launch() {
            isRunning = true
            val cmd = command
            command = ""
            try {
                commandExecutor.execute(cmd, isRoot).flowOn(Dispatchers.IO).collect { newLine ->
                    _output.add(newLine)
                }
            } catch (e: Exception) {
                _output.add(e.message ?: "[An error occurred]")
            } finally {
                isRunning = false
            }
        }
    }

    fun terminate() = _output.add(commandExecutor.cancel())
}