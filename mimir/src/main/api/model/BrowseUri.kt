package api.model


import dot.DOTLang
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFLanguages
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream


data class UriRequest(val url: String = "")
data class UriResponse(val browse_error: String = "", val model_dot: String = "")

@RestController
class BrowseUriController {


    @PostMapping(
        "/api/model/browse",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
        produces = [MediaType.APPLICATION_JSON_UTF8_VALUE]
    )
    fun browseUri(@RequestBody requestBody: UriRequest): ResponseEntity<UriResponse>{
        if (requestBody.url.isBlank()) {
            return ResponseEntity(UriResponse("Nothing to look up" + requestBody.url), HttpStatus.NO_CONTENT)
        }

        // Read Uri and load into Model
        val model : Model = ModelFactory.createDefaultModel()
        try {
            RDFDataMgr.read(model, requestBody.url)

        } catch (e: Exception) {
            return ResponseEntity(
                UriResponse("Apache Jena was unable to load data in a model. Error was: \n" +
                        e.message.toString()),
                HttpStatus.OK,
            )
        }

        // Check if DOT lang is loaded
        val dotLang = DOTLang
        assert(RDFLanguages.isRegistered(dotLang)) {"Controller Error, DOT is not registered on Jena"}
        RDFLanguages.isRegistered(dotLang)

        val modelOnDot = ByteArrayOutputStream()
        try {
            RDFDataMgr.write(modelOnDot, model, RDFFormat(DOTLang))
        } catch (e:Exception) {
            return ResponseEntity(UriResponse(browse_error = e.message.toString()), HttpStatus.INTERNAL_SERVER_ERROR)
        }

        return ResponseEntity(UriResponse(model_dot = modelOnDot.toString()), HttpStatus.OK)
    }

}
