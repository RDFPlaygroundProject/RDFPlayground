package api.shape

import toPrintableString
import loadModel
import org.apache.jena.graph.Graph
import org.apache.jena.rdf.model.Model
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import org.apache.jena.shacl.ValidationReport
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import toDOT

data class SHACLRequest(
    val data: String = "",
    val data_lang: String = "TTL",
    val shape: String = "",
    val shape_lang: String = "TTL"
)

data class SHACLResponse(
    val report: String,
    val fusion_dot: String
)

@RestController
class SHACLIsValidController {

    @PostMapping(
        "/api/shape/shacl_isvalid",
        consumes = [MimeType.json]
    )
    fun shaclIsValid(@RequestBody requestBody: SHACLRequest): ResponseEntity<Any> {
        val header = HttpHeaders()

        // Define the terms outside of try in to use them later
        val dataGraph: Graph
        val shapesModel: Model
        val shapes: Shapes
        val fusionDot: String                   // dot of the union of both graphs
        val validationReport: ValidationReport  // SCHACL report

        if (requestBody.data.isBlank() || requestBody.shape.isBlank()) {
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity("No Content", header, HttpStatus.NO_CONTENT)
        }

        try {
            // Load shape graph
            shapesModel = loadModel(requestBody.shape, requestBody.shape_lang)
            shapes = Shapes.parse(shapesModel)
        } catch (e: Exception) {
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity("Shapes ill formed, ${e.message.toString()}", header, HttpStatus.BAD_REQUEST)
        }

        try {
            // Load data graph
            dataGraph = loadModel(requestBody.data, requestBody.data_lang).graph
        } catch (e: Exception) {
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity(
                "Data graph ill formed, ${e.message.toString()}", header, HttpStatus.BAD_REQUEST
            )
        }

        try {
            // Create DOT with data and shapes
            fusionDot = loadModel(requestBody.data, requestBody.data_lang).union(shapesModel).toDOT()
        } catch (e: Exception) {
            return ResponseEntity(e.message.toString(), header, HttpStatus.BAD_REQUEST)
        }

        try {
            // Get a validation report
            validationReport = ShaclValidator.get().validate(shapes, dataGraph)
        } catch (e: Exception) {
            // Error trying to make the report
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity(e.message.toString(), header, HttpStatus.INTERNAL_SERVER_ERROR)
        }

        header.add(HttpHeaders.CONTENT_TYPE, MimeType.json)
        // default Response as text
        return when {
            validationReport.conforms() -> {
                // Respond "" instead of Conforms\n
                ResponseEntity(
                    SHACLResponse("", fusionDot),
                    header,
                    HttpStatus.OK
                )
            }

            else -> {
                // Return complete report as given by Jena
                ResponseEntity(
                    SHACLResponse(validationReport.toPrintableString(), fusionDot),
                    header,
                    HttpStatus.OK
                )
            }
        }
    }
}