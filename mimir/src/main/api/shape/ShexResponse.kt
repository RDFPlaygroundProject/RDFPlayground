package api.shape

import fr.inria.lille.shexjava.schema.ShexSchema
import fr.inria.lille.shexjava.validation.RecursiveValidationWithMemorization
import fr.inria.lille.shexjava.validation.ValidationAlgorithm
import org.apache.commons.rdf.api.Graph
import org.apache.commons.rdf.rdf4j.RDF4J
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

import shex.ShexShapeMap
import shex.ShexWrapper


data class ShExRequest(
    val data: String = "",
    val data_lang: String = "TTL",
    val shape: String = "",
    val shape_map: String = ""
)

@RestController
class ShexIsValidController {

    @PostMapping(
        "/api/shape/shex_isvalid",
        consumes = [MimeType.json],
        produces = [MimeType.text]
    )
    fun shexIsValid(@RequestBody requestBody: ShExRequest): ResponseEntity<String> {
        val header = HttpHeaders()
        header.add(HttpHeaders.CONTENT_TYPE, MimeType.text) // all responses are purely on text

        val dataGraph: Graph                // data graph
        val shape: ShexSchema               // shape expression graph
        val mapping: List<ShexShapeMap>     // fixed shape mapping (custom)
        val validation: ValidationAlgorithm // validation of the entire data graph
        val validationResult: String        // validation of the mapping

        if (requestBody.data.isBlank() || requestBody.shape.isBlank() || requestBody.shape_map.isBlank()) {
            return ResponseEntity("No Content", header, HttpStatus.NO_CONTENT)
        }

        val shexFactory = RDF4J()               // Create a factory to be used by ShexJava
        val shexWrap = ShexWrapper(shexFactory) // set global factory for ShexJava and start wrapper

        try {
            // Load data model as Graph
            dataGraph = shexWrap.createDataGraph(requestBody.data, requestBody.data_lang)
        } catch (e: Exception) {
            return ResponseEntity(
                "Data graph ill formed, ${e.message.toString()}", header, HttpStatus.BAD_REQUEST
            )
        }

        try {
            // Load schema to shex java
            shape = shexWrap.createSchema(requestBody.shape)
        } catch (e: Exception) {
            return ResponseEntity(
                "Shapes ill formed, ${e.message.toString()}", header, HttpStatus.BAD_REQUEST
            )
        }

        try {
            // Parse Shape map as Pairs with it's corresponding absolute IRIs
            mapping = shexWrap.parseMap(requestBody.shape_map)
        } catch (e: Exception) {
            return ResponseEntity(
                "Shape map ill formed, ${e.message.toString()}", header, HttpStatus.BAD_REQUEST
            )
        }

        try {
            // Validate graph and generate result text
            validation = RecursiveValidationWithMemorization(shape, dataGraph)
            validationResult = shexWrap.validateMapping(validation, mapping)
        } catch (e: Exception) {
            return ResponseEntity(
                "Error generating validation report: ${e.message.toString()}",
                header,
                HttpStatus.INTERNAL_SERVER_ERROR

            )
        }

        // Return validation results
        return ResponseEntity(
            validationResult, header, HttpStatus.OK
        )
    }
}
