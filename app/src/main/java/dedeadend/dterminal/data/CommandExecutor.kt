package dedeadend.dterminal.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

interface CommandExecutor {
    fun execute(command: String, isroot: Boolean): Flow<String>
    fun cancel() : String
}

class ShellCommandExecutor : CommandExecutor {
    private var process: Process? = null

    override fun execute(command: String, isroot: Boolean): Flow<String> = callbackFlow{
        val commands = command.lines()
        for (cmd in commands) {
            if (cmd.isEmpty()) continue
            if (isroot)
                process = ProcessBuilder("su", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            else
                process = ProcessBuilder("/system/bin/sh", "-c", cmd)
                    .redirectErrorStream(true)
                    .start()
            val reader = process?.inputStream?.bufferedReader()
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                trySend(line!!)
            }
            process?.waitFor()
            process?.destroy()
            process = null
        }
        close()
    }

    override fun cancel(): String {
        process?.let {
            it.destroy()
            process = null
            return "[Process Terminated by User]"
        }
        return "[There is No Active Process]"
    }
}