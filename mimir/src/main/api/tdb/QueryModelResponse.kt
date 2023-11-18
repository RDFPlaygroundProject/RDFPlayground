package api.tdb

import org.apache.jena.query.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory.createDefaultModel
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

import formatAs
import formatAskAs
import loadModel
import parseFormat
import toDOT
import toMimeType
import api.tdb.sparqlPatternToDot


data class QueryModelRequest(
    val data: String = "",
    val data_lang: String = "TTL",
    val query: String = "",
    val query_response_lang:String = ""
)

fun pickFormat(formats: MutableList<String>): String {
    return when (formats.size) {
        0 -> "No Format"
        1 -> formats.first()
        else -> {
            // We always expect to receive Text and Another format,
            // so we pick the other one
            if (formats.filterNot { it == "Text" }.isEmpty()) return "Text"
            else formats
                .filterNot { it == "Text" }
                .first()
        }
    }

}

@RestController
class QueryModelController {

    @PostMapping(
        "/api/tdb/query_model",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE]
    )
    fun queryModel(
        @RequestHeader(HttpHeaders.ACCEPT) formats: String,
        @RequestBody request: QueryModelRequest
    ): ResponseEntity<String> {
        val responseLangOptions = parseFormat(formats)
        var responseLang = pickFormat(responseLangOptions)
        val header = HttpHeaders()

        // no info case
        if (request.data.isBlank()) {
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity("No Content", header, HttpStatus.NO_CONTENT)
        }

        // load model to Memory
        val model: Model
        try {
            model = loadModel(request.data, request.data_lang)
            val dataOnDOT: String = model.toDOT()
        } catch (e: Exception) {
            // could not load model by some reason
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity("Can't read model", header, HttpStatus.BAD_REQUEST)
        }
        val query: Query

        try {
            // Query that model
            query = QueryFactory.create(request.query)
        } catch (e: Exception) {
            // could not load model by some reason
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity("Can't run query: \n Reason: ${e.message}", header, HttpStatus.BAD_REQUEST)
        }

        var queryResponseBody = String()
        var response_type:String = request.query_response_lang
        if (response_type == "Query") {
            queryResponseBody = sparqlPatternToDot()
        }
        else {
            try {
                QueryExecutionFactory.create(query, model).let { qExecution: QueryExecution ->
                    when {
                        query.isSelectType -> {
                            val resultSet: ResultSet = qExecution.execSelect()
                            queryResponseBody = resultSet.formatAs(responseLang)
                        }
                        query.isAskType -> {
                            val resultSet: Boolean = qExecution.execAsk()
                            queryResponseBody = resultSet.formatAskAs(responseLang)
                        }
                        query.isConstructType or query.isDescribeType -> {
                            val resultModel: Model = when {
                                query.isConstructType -> qExecution.execConstruct()
                                query.isDescribeType -> qExecution.execDescribe()
                                else -> createDefaultModel() // Should not be possible, but when is exhaustive
                            }
                            // Format to response, defaults to TTL
                            val responsePair: Pair<String, String> = resultModel.formatAs(responseLang)
                            queryResponseBody = responsePair.first
                            responseLang = responsePair.second
                        }
                    }
                }
            } catch (e: Exception) {
                // Return an error response
                header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
                return ResponseEntity("Unsupported Media type / Format", header, HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            }
        }
        header.add(HttpHeaders.CONTENT_TYPE, toMimeType(responseLang))
        return ResponseEntity(queryResponseBody, header, HttpStatus.OK)
    }
}
