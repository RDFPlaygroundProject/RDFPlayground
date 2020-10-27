package api.model

import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFLanguages
import java.io.ByteArrayOutputStream

import dot.DOTLang
import loadModel

data class SyntaxCheckRequest(val data: String = "", val data_lang: String = "TTL")
data class SyntaxCheckResponse(val syntax_error: String = "", val data_dot: String = "")

@RestController
class SyntaxCheckController {

    @PostMapping(
        "/api/model/syntax_check",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
        produces = [MediaType.APPLICATION_JSON_UTF8_VALUE]
    )
    fun syntaxCheck(@RequestBody requestBody: SyntaxCheckRequest): ResponseEntity<SyntaxCheckResponse> {
        // base case
        if (requestBody.data.isBlank()) {
            return ResponseEntity(SyntaxCheckResponse("Nothing received"), HttpStatus.NO_CONTENT)
        }

        // Create a model on Memory
        val model: Model
        try {
            model = loadModel(requestBody.data, requestBody.data_lang)
        } catch (e: Exception) {
            return ResponseEntity(SyntaxCheckResponse(syntax_error = e.message.toString()), HttpStatus.OK)
        }
        // Check if DOT lang is loaded
        val dotLang = DOTLang
        assert(RDFLanguages.isRegistered(dotLang)) {"Controller Error, DOT is not registered on Jena"}
        RDFLanguages.isRegistered(dotLang)

        // Convert to DOT and save on ByteArray Stream
        val modelOnDOT = ByteArrayOutputStream()
        try {
            // write to DOT
            RDFDataMgr.write(modelOnDOT, model, RDFFormat(DOTLang)) // Writes DOT to modelOnDOT
        } catch (e: Exception) {
            return ResponseEntity(SyntaxCheckResponse(syntax_error = e.message.toString()), HttpStatus.INTERNAL_SERVER_ERROR)
        }

        return ResponseEntity(SyntaxCheckResponse(data_dot =  modelOnDOT.toString()), HttpStatus.OK)
    }
}