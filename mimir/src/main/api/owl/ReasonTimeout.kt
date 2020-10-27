package api.owl

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.http.MediaType
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import api.ReasonerConfig

data class ReasonTimeoutRequest(
    val data: String = "",
    val data_lang: String = "TTL",
    val profile: String = ReasonerConfig.DEFAULT_PROFILE,
    val timeout: Long = 60000
)

@RestController
class ReasonTimeoutController {

    @PostMapping(
        "/api/owl/reason_timeout",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
        produces = [MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.TEXT_PLAIN_VALUE])
    fun reasonTimeout(
        @RequestBody requestBody: ReasonTimeoutRequest
    ): ResponseEntity<Any> {
        val header = HttpHeaders()

        return runBlocking {
            // Runs ReasonResponse With a Timeout
            val response = withTimeoutOrNull(requestBody.timeout) {
                val request = ReasonRequest(requestBody.data, requestBody.data_lang, requestBody.profile)
                // delegate to ReasonResponse
                ReasonController().reason(request)
            }

            when(response) {
                null -> {
                    // Time has run out, respond that
                    header.add(CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    return@runBlocking ResponseEntity("Time ran out for this request", header, HttpStatus.REQUEST_TIMEOUT)
                }

                else -> return@runBlocking response
            }
        }
    }
}