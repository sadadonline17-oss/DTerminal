package dedeadend.dterminal.domain

interface CommandExecutor {
    suspend fun execute(command: String, isRoot: Boolean)
    suspend fun cancel()
}