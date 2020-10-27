package reasoner

import api.ReasonerConfig
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Paths

class OwlRLWrapper (private val config: ReasonerConfig = ReasonerConfig) {
    var profile: String = config.DEFAULT_PROFILE

    init {
        try {
            // test getting the default file
            val defaultFilePath = Paths.get(config.DEFAULT_DATA_DIR+config.DEFAULT_DATA_NAME)
            Files.readAllBytes(defaultFilePath.toAbsolutePath())
        } catch (e: Exception) {
            println("ERROR: could not get test file path. \n Reason: ${e.message}")
            // throw error to be used at response time
            throw e
        }
    }

    fun infer(data: String, profile: String = this.profile): Pair<String, String> {
        val type: String = when (profile.toLowerCase()) {
            config.RDFS_PROFILE -> config.RDFS_FLAG
            config.OWL_PROFILE -> config.OWL_FLAG
            else -> this.config.DEFAULT_FLAG // error, we'll use default one
        }

        // Create temp file with out model
        val tempDirectory = Paths.get(config.TMP_DIR_NAME).toFile()
        val tempFile = createTempFile(config.TMP_NAME_PREFIX, config.TMP_NAME_SUFFIX,tempDirectory)
        tempFile.writeText(data, Charsets.UTF_8)

        // Run OWL-RL
        val runtime: Runtime = Runtime.getRuntime()
        // Executes the command that runs the owlrl script (activates the environment first)
        val process = runtime.exec("${config.CMD_NAME}$type -f ${tempFile.path}")

        val inferenceResult: String = BufferedReader(process.inputStream.reader()).readText()

        // Read the error and skip the stacktrace
        var stack: Boolean = true
        var errorResult: String = ""
        process.errorStream.reader().readLines().forEach { line ->
            run {
                if (!stack) {
                    errorResult += "$line\n"
                }

                // Skip StackTrace
                if (line.contains(" at line ") && stack) {
                    stack = false
                }
            }
        }

        // Delete temp file to save space
        if (!tempFile.delete()) println("ERROR: could not delete ${tempFile.name}")

        return Pair(inferenceResult, errorResult)
    }

}
