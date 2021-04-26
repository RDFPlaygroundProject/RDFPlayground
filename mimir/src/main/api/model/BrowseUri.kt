package api.model


import dot.DOTLang
import formatAs
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


data class UriRequest(val uri: String = "", val data_lang: String = "TTL")
data class UriResponse(val browse_error: String = "", val data_dot: String = "")

@RestController
class BrowseUriController {


    @PostMapping(
        "/api/model/browse",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
        produces = [MediaType.APPLICATION_JSON_UTF8_VALUE]
    )
    fun browseUri(@RequestBody requestBody: UriRequest): ResponseEntity<UriResponse>{
        if (requestBody.uri.isBlank()) {
            return ResponseEntity(UriResponse(), HttpStatus.NO_CONTENT)
        }

        // Read Uri and load into Model
        val model : Model
        try {
            model = RDFDataMgr.loadModel(requestBody.uri, RDFLanguages.TTL)
        } catch (e: Exception) {
            return ResponseEntity(UriResponse(browse_error = e.message.toString()), HttpStatus.OK)
        }

        val dotLang = DOTLang
        assert(RDFLanguages.isRegistered(dotLang)) {"Controller Error, DOT is not registered on Jena"}
        RDFLanguages.isRegistered(dotLang)

        val modelToDot = ByteArrayOutputStream()
        try {
            RDFDataMgr.write(modelToDot, model, RDFFormat(DOTLang))
        } catch (e:Exception) {
            return ResponseEntity(UriResponse(browse_error = e.message.toString()), HttpStatus.INTERNAL_SERVER_ERROR)
        }

        return ResponseEntity(UriResponse(data_dot = model.formatAs("TTL").first), HttpStatus.OK)
    }


    private fun readURI(uri : String): Model {
        val model : Model = ModelFactory.createDefaultModel()
        try {
            RDFDataMgr.read(model, uri)
        } catch (e: Exception) {
            throw e
        }
        return model
    }

}