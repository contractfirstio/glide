package glide.startup

import java.util.Locale

object StartupGreeting {
    fun playOnLaunch() {
        Thread(
            {
                try {
                    speak(buildMessage())
                } catch (_: Exception) {
                    // TTS is best-effort; never block or crash startup.
                }
            },
            "glide-startup-greeting",
        ).apply {
            isDaemon = true
            start()
        }
    }

    private fun buildMessage(): String {
        val period = when (java.time.LocalTime.now().hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            else -> "Evening"
        }
        return "$period Claire"
    }

    private fun speak(message: String) {
        when (hostOs()) {
            HostOs.MACOS -> speakMac(message)
            HostOs.WINDOWS -> speakWindows(message)
            HostOs.LINUX -> speakLinux(message)
            HostOs.OTHER -> Unit
        }
    }

    private fun speakMac(message: String) {
        ProcessBuilder("say", message)
            .redirectErrorStream(true)
            .start()
            ?.waitFor()
    }

    private fun speakWindows(message: String) {
        val escaped = message.replace("'", "''")
        val command =
            "Add-Type -AssemblyName System.Speech; " +
                "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('$escaped');"
        ProcessBuilder("powershell", "-NoProfile", "-Command", command)
            .redirectErrorStream(true)
            .start()
            ?.waitFor()
    }

    private fun speakLinux(message: String) {
        if (commandExists("spd-say")) {
            ProcessBuilder("spd-say", message).start()?.waitFor()
            return
        }
        if (commandExists("espeak")) {
            ProcessBuilder("espeak", message).start()?.waitFor()
        }
    }

    private fun commandExists(name: String): Boolean =
        ProcessBuilder("which", name)
            .redirectErrorStream(true)
            .start()
            ?.waitFor() == 0

    private fun hostOs(): HostOs {
        val os = System.getProperty("os.name", "").lowercase(Locale.US)
        return when {
            os.contains("mac") || os.contains("darwin") -> HostOs.MACOS
            os.contains("win") -> HostOs.WINDOWS
            os.contains("nux") || os.contains("linux") -> HostOs.LINUX
            else -> HostOs.OTHER
        }
    }

    private enum class HostOs {
        MACOS,
        WINDOWS,
        LINUX,
        OTHER,
    }
}
