package api.model

import dot.DOTLang
import loadModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFLanguages
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream


data class DotRequest(val data: String = "", val data_lang: String = "TTL")

@RestController
class DotController {

    @PostMapping(
        "/api/model/dot",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
        produces = [MimeType.dot]
    )
    fun dot(@RequestBody requestBody: DotRequest): ResponseEntity<String> {

        val header = HttpHeaders()
        header.add(HttpHeaders.CONTENT_TYPE, MimeType.dot)
        if (requestBody.data.isBlank()) {
            return ResponseEntity("", header, HttpStatus.OK)
        }
        // Create a model on Memory
        val model: Model

        // Convert to DOT and save on ByteArray Stream
        val modelOnDOT = ByteArrayOutputStream()

        try {
            model = loadModel(requestBody.data, requestBody.data_lang)
            // Check if DOT lang is loaded
            val dotLang = DOTLang
            assert(RDFLanguages.isRegistered(dotLang)) {"Controller Error, DOT is not registered on Jena"}
            RDFLanguages.isRegistered(dotLang)
            // write to DOT
            RDFDataMgr.write(modelOnDOT, model, RDFFormat(DOTLang)) // Writes DOT to modelOnDOT
        } catch (e: Exception) {
            return ResponseEntity("", header, HttpStatus.INTERNAL_SERVER_ERROR)
        }

        return ResponseEntity(modelOnDOT.toString(), header, HttpStatus.OK)
    }
}
