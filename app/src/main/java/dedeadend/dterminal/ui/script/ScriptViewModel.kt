package dedeadend.dterminal.ui.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dedeadend.dterminal.data.Repository
import dedeadend.dterminal.domin.Script
import dedeadend.dterminal.domin.UiEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptViewModel @Inject constructor(
    private val repository: Repository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    val scripts = repository.getScripts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private var scriptsBackup: List<Script>? = null

    private var _eventFlow = Channel<UiEvent>(Channel.RENDEZVOUS)
    val eventFlow = _eventFlow.receiveAsFlow()


    fun deleteScript(id: Int) {
        viewModelScope.launch(ioDispatcher) {
            scripts.value.find { it.id == id }?.let { scriptsBackup = listOf(it) }
            repository.deleteScriptWithId(id)
        }
    }

    fun undoDeleteHistoryCommand(id: Int) {
        viewModelScope.launch(ioDispatcher) {
            scriptsBackup?.let {
                repository.insertToScripts(scriptsBackup!!.get(0))
            }
        }
    }

}