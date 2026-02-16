package dedeadend.dterminal.data

import android.os.Build
import android.os.SystemClock
import dedeadend.dterminal.domain.CommandExecutor
import dedeadend.dterminal.domain.History
import dedeadend.dterminal.domain.TerminalLog
import dedeadend.dterminal.domain.TerminalState
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ShellCommandExecutor @Inject constructor(
    private val repository: Repository,
    private val ioDispatcher: CoroutineDispatcher
) : CommandExecutor {
    private var process: Process? = null
    override suspend fun execute(command: String, isRoot: Boolean) {
        withContext(ioDispatcher) {
            repository.addHistory(History(command))
            repository.addLog(
                TerminalLog(
                    TerminalState.Info,
                    (if (isRoot) "#: " else "$: ") + command
                )
            )
            try {
                process = ProcessBuilder(if (isRoot) "su" else "sh")
                    .redirectErrorStream(true)
                    .start()
                launch {
                    process?.outputStream?.bufferedWriter()?.use { writer ->
                        command.lines().forEach { cmd ->
                            if (cmd.trim().isNotBlank()) {
                                if (!executedAsCustomCommand(repository, cmd)) {
                                    writer.write(cmd + "\n")
                                    writer.flush()
                                }
                            }
                        }
                        writer.write("exit\n")
                        writer.flush()
                    }
                }
                process?.inputStream?.bufferedReader()?.use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        repository.addLog(TerminalLog(TerminalState.Success, line!!))
                    }
                }
                process?.waitFor()
            } catch (e: Exception) {
                repository.addLog(TerminalLog(TerminalState.Error, e.message ?: "Unknown Error"))
            } finally {
                process?.let { process ->
                    process.inputStream?.close()
                    process.outputStream?.close()
                    process.errorStream?.close()
                    process.destroyForcibly()
                }
                process = null
            }
        }
    }

    override suspend fun cancel() {
        withContext(ioDispatcher) {
            if (process == null) {
                repository.addLog(
                    TerminalLog(TerminalState.Error, "There is no active process")
                )
            }
            process?.let { process ->
                val pid = getPid()
                if (pid != -1) {
                    Runtime.getRuntime().exec("pkill -P $pid")
                }
                process.destroy()
                if (!process.waitFor(1000, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly()
                    process.waitFor()
                }
                process.inputStream.close()
                process.outputStream.close()
                process.errorStream.close()
                repository.addLog(
                    TerminalLog(TerminalState.Error, "Process terminated by user")
                )
            }
            process = null
        }
    }

    private fun getPid(): Int {
        return try {
            val field = process!!.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            field.getInt(process)
        } catch (_: Exception) {
            -1
        }
    }

    private suspend fun executedAsCustomCommand(
        repository: Repository,
        command: String
    ): Boolean {
        when (command.split(" ")[0].lowercase().trim()) {

            "help" -> {
                val helpText = """
                DTerminal Commands:
                -------------------
                - help:  Show this list
                - about:  Information about DTerminal
                - color1 [r] [g] [b]:  Set the Normal log font color
                - color2 [r] [g] [b]:  Set the Error log font color
                - color3 [r] [g] [b]:  Set the Info log font color
                - font [size]:  Set the log font size
                - random [a] [b]:  Get a random number between a and b
                - clear/cls:  Clear the terminal logs
                - echo [a]:  Echo a message
                - sysinfo:  Information about your device
                - whoami:  Get your username
                - date:  Get the current date and time
                - sudo [command]:  Execute a command as root
            """.trimIndent()
                repository.addLog(TerminalLog(TerminalState.Success, helpText))
                return true
            }

            "about" -> {
                val aboutText = """
                        
                     _____  _____                   _              _ 
                    |  _  \|_   _|                 (_)            | |
                    | |  | | | | ___ _ __ _ __ ___  _ _ __   __ _ | |
                    | |  | | | |/ _ \ '__| '_ ` _ \| | '_ \ / _` || |
                    | |__/ / | |  __/ |  | | | | | | | | | | (_| || |
                    |_____/  \_/\___|_|  |_| |_| |_|_|_| |_|\__,_||_|
                                                    
                                                          
                                                                      
                    ♠️ DTerminal v1.1 - Created by Ehsan Nasiri. 
                    
                    🌐 Open Source on GitHub: github.com/dedeadend
                    
                    ☕ Coffee is not included!
                    
                    💚 Enjoy :)
                    
                    
                    -------------------------------------------------
                """.trimIndent()
                repository.addLog(TerminalLog(TerminalState.Success, aboutText))
                return true
            }

            "color1" -> {
                val parts = command.trim().split("\\s+".toRegex())
                if (parts.size == 2) {
                    if (parts[1] == "def") {
                        repository.setLogSuccessFontColor(-1, -1, -1)
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Success,
                                "Color set successfully."
                            )
                        )
                    } else {
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Error,
                                "Usage: color1 [red] [green] [blue]\n" +
                                        "Example: color1 250 140 0\n" +
                                        "Default: color1 def"
                            )
                        )
                    }
                } else if (parts.size == 4) {
                    try {
                        val red = parts[1].toInt()
                        val green = parts[2].toInt()
                        val blue = parts[3].toInt()
                        if (red in 0..255 && green in 0..255 && blue in 0..255) {
                            repository.setLogSuccessFontColor(red, green, blue)
                            repository.addLog(
                                TerminalLog(
                                    TerminalState.Success,
                                    "Color set successfully."
                                )
                            )
                        } else {
                            repository.addLog(
                                TerminalLog(
                                    TerminalState.Error,
                                    "Invalid color values.\n" +
                                            "Note: Values must be between 0 and 255."
                                )
                            )
                        }
                    } catch (_: NumberFormatException) {
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Error,
                                "Usage: color1 [red] [green] [blue]\n" +
                                        "Example: color1 250 140 0\n" +
                                        "Default: color1 def"
                            )
                        )
                    }
                } else {
                    repository.addLog(
                        TerminalLog(
                            TerminalState.Error,
                            "Usage: color1 [red] [green] [blue]\n" +
                                    "Example: color1 250 140 0\n" +
                                    "Default: color1 def"
                        )
                    )
                }
                return true
            }

            "color2" -> {
                val parts = command.trim().split("\\s+".toRegex())
                if (parts.size == 2) {
                    if (parts[1] == "def") {
                        repository.setLogErrorFontColor(-1, -1, -1)
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Success,
                                "Color set successfully."
                            )
                        )
                    } else {
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Error,
                                "Usage: color2 [red] [green] [blue]\n" +
                                        "Example: color2 250 140 0\n" +
                                        "Default: color2 def"
                            )
                        )
                    }
                } else if (parts.size == 4) {
                    try {
                        val red = parts[1].toInt()
                        val green = parts[2].toInt()
                        val blue = parts[3].toInt()
                        if (red in 0..255 && green in 0..255 && blue in 0..255) {
                            repository.setLogErrorFontColor(red, green, blue)
                            repository.addLog(
                                TerminalLog(
                                    TerminalState.Success,
                                    "Color set successfully."
                                )
                            )
                        } else {
                            repository.addLog(
                                TerminalLog(
                                    TerminalState.Error,
                                    "Invalid color values.\n" +
                                            "Note: Values must be between 0 and 255."
                                )
                            )
                        }
                    } catch (_: NumberFormatException) {
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Error,
                                "Usage: color2 [red] [green] [blue]\n" +
                                        "Example: color2 250 140 0\n" +
                                        "Default: color2 def"
                            )
                        )
                    }
                } else {
                    repository.addLog(
                        TerminalLog(
                            TerminalState.Error,
                            "Usage: color2 [red] [green] [blue]\n" +
                                    "Example: color2 250 140 0\n" +
                                    "Default: color2 def"
                        )
                    )
                }
                return true
            }

            "color3" -> {
                val parts = command.trim().split("\\s+".toRegex())
                if (parts.size == 2) {
                    if (parts[1] == "def") {
                        repository.setLogInfoFontColor(-1, -1, -1)
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Success,
                                "Color set successfully."
                            )
                        )
                    } else {
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Error,
                                "Usage: color3 [red] [green] [blue]\n" +
                                        "Example: color3 250 140 0\n" +
                                        "Default: color3 def"
                            )
                        )
                    }
                } else if (parts.size == 4) {
                    try {
                        val red = parts[1].toInt()
                        val green = parts[2].toInt()
                        val blue = parts[3].toInt()
                        if (red in 0..255 && green in 0..255 && blue in 0..255) {
                            repository.setLogInfoFontColor(red, green, blue)
                            repository.addLog(
                                TerminalLog(
                                    TerminalState.Success,
                                    "Color set successfully."
                                )
                            )
                        } else {
                            repository.addLog(
                                TerminalLog(
                                    TerminalState.Error,
                                    "Invalid color values.\n" +
                                            "Note: Values must be between 0 and 255."
                                )
                            )
                        }
                    } catch (_: NumberFormatException) {
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Error,
                                "Usage: color3 [red] [green] [blue]\n" +
                                        "Example: color3 250 140 0\n" +
                                        "Default: color3 def"
                            )
                        )
                    }
                } else {
                    repository.addLog(
                        TerminalLog(
                            TerminalState.Error,
                            "Usage: color3 [red] [green] [blue]\n" +
                                    "Example: color3 250 140 0\n" +
                                    "Default: color3 def"
                        )
                    )
                }
                return true
            }

            "font" -> {
                val parts = command.trim().split("\\s+".toRegex())
                if (parts.size == 2) {
                    try {
                        val fontSize = parts[1].toInt()
                        if (fontSize in 5..25) {
                            repository.setLogFontSize(fontSize)
                            repository.addLog(
                                TerminalLog(
                                    TerminalState.Success,
                                    "Font size set successfully."
                                )
                            )
                        } else {
                            repository.addLog(
                                TerminalLog(
                                    TerminalState.Error,
                                    "Invalid font size value.\n" +
                                            "Note: Value must be between 5 and 25."
                                )
                            )
                        }
                    } catch (_: NumberFormatException) {
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Error,
                                "Invalid font value.\n" +
                                        "Note: Value must be between 5 and 25."
                            )
                        )
                    }
                } else {
                    repository.addLog(
                        TerminalLog(
                            TerminalState.Error,
                            "Usage: font [size]\n" +
                                    "Example: font 13"
                        )
                    )
                }
                return true
            }

            "random" -> {
                val parts = command.trim().split("\\s+".toRegex())
                if (parts.size == 3) {
                    try {
                        val num1 = parts[1].toInt()
                        val num2 = parts[2].toInt()
                        if (num1 > num2) {
                            val randomNum = (num2..num1).random()
                            repository.addLog(TerminalLog(TerminalState.Success, ("$randomNum")))

                        } else {
                            val randomNum = (num1..num2).random()
                            repository.addLog(TerminalLog(TerminalState.Success, ("$randomNum")))
                        }
                    } catch (_: NumberFormatException) {
                        repository.addLog(
                            TerminalLog(
                                TerminalState.Error,
                                "Invalid input. Please enter valid numbers."
                            )
                        )
                    }
                } else {
                    repository.addLog(
                        TerminalLog(
                            TerminalState.Error,
                            "Usage: random [a] [b]\n" +
                                    "Example: random 1 100"
                        )
                    )
                }
                return true
            }

            "clear", "cls" -> {
                repository.clearLogs()
                return true
            }

            "echo" -> {
                if (command.split(" ").size > 1) {
                    val message = command.substringAfter(" ").trim()
                    repository.addLog(TerminalLog(TerminalState.Success, message))
                } else {
                    repository.addLog(
                        TerminalLog(
                            TerminalState.Error,
                            "Usage: echo [a]\n" +
                                    "Example: echo Hello DTerminal!"
                        )
                    )
                }
                return true
            }

            "sysinfo" -> {
                val manufacturer = Build.MANUFACTURER
                val model = Build.MODEL
                val androidVersion = Build.VERSION.RELEASE
                val sdkVersion = Build.VERSION.SDK_INT
                val board = Build.BOARD
                val hardware = Build.HARDWARE
                val upTime = SystemClock.elapsedRealtime()
                val hours = (upTime / (1000 * 60 * 60)) % 24
                val minutes = (upTime / (1000 * 60)) % 60
                val seconds = (upTime / 1000) % 60
                val uptimeString = String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d", hours, minutes, seconds
                )
                val info = """
                    [ DEVICE INFORMATION ]
                    ----------------------
                    Manufacturer : ${manufacturer.uppercase()}
                    Model        : $model
                    Board        : $board
                    Hardware     : $hardware
                    
                    
                    [ SOFTWARE SYSTEM ]
                    -------------------
                    Android Ver  : $androidVersion (API $sdkVersion)
                    Kernel       : ${System.getProperty("os.version")}
                    
                    
                    [ SYSTEM ]
                    ----------
                    Uptime: $uptimeString
                """.trimIndent()

                repository.addLog(TerminalLog(TerminalState.Success, info))
                return true
            }

            "whoami" -> {
                val userName = "dterminal"
                val deviceName = Build.MODEL.lowercase().replace(" ", "_")
                repository.addLog(TerminalLog(TerminalState.Success, "$userName@$deviceName"))
                return true
            }

            "date" -> {
                val formatter = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault())
                val currentDate = formatter.format(Date())
                repository.addLog(TerminalLog(TerminalState.Success, currentDate))
                return true
            }

            "sudo" -> {
                repository.addLog(
                    TerminalLog(
                        TerminalState.Success,
                        "Nice try! But you have no power here :)"
                    )
                )
                return true
            }

            else -> {
                return false
            }
        }
    }
}