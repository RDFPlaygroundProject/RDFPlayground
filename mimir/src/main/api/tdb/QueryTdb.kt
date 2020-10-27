package api.tdb

import MimeType
import api.TDBConfig
import formatAs
import formatAskAs
import parseFormat
import toMimeType

import org.apache.jena.query.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.tdb.TDBFactory
import org.apache.jena.update.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlin.Exception

data class QueryTdbRequest(
    val query: String = "",
    val type: String = ""
)

@RestController
class QueryTdbController {

    @PostMapping(
        "/api/tdb/query_tdb",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE]
    )
    fun queryTdb(
        @RequestHeader(HttpHeaders.ACCEPT) formats: String,
        @RequestBody request: QueryTdbRequest
    ): ResponseEntity<String> {
        val header = HttpHeaders()
        // process requested languages to see if they're available
        val responseLangOptions: MutableList<String> = parseFormat(formats)
        var responseLang: String = pickFormat(responseLangOptions)

        // var containing the final "response" to the client
        var queryResponseBody = String()

        // no data case
        if (request.query.isBlank() or request.type.isBlank()) {
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity("No content", header, HttpStatus.NO_CONTENT)
        }
        // unspecified response language case
        if (responseLang == "No Format") {
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity("No format specified", header, HttpStatus.BAD_REQUEST)
        }

        // get TDB directory
        val directory: String = TDBConfig.TDB_DIR
        val dataset: Dataset

        try {
            // get database
            dataset = TDBFactory.createDataset(directory)
        } catch (e: Exception) {
            // cannot continue without the database
            header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
            return ResponseEntity("Couldn't access TDB", header, HttpStatus.INTERNAL_SERVER_ERROR)
        }

        // Choose path according to SPARQL type (Query or Update)
        when (request.type) {
            "query" -> {
                // access TDB
                dataset.begin(ReadWrite.READ)
                try {
                    // get as model to query over it
                    val model: Model = dataset.defaultModel

                    // process query
                    val query: Query = QueryFactory.create(request.query)

                    try {
                        // create query execution and execute it according to it's type
                        // types: SELECT ASK CONSTRUCT DESCRIBE
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
                                    // the process is fairly equal, exec, get resulting model
                                    // and return that on the desired format
                                    val resultModel: Model = when {
                                        query.isConstructType -> qExecution.execConstruct()
                                        query.isDescribeType -> qExecution.execDescribe()
                                        else -> ModelFactory.createDefaultModel() // Should not be possible, but when is exhaustive
                                    }
                                    // Format to response, defaults to TTL
                                    val responsePair: Pair<String, String> = resultModel.formatAs(responseLang)
                                    queryResponseBody = responsePair.first
                                    responseLang = responsePair.second
                                }
                            }
                        }
                    } catch (e: Exception) {
                        dataset.end()
                        // return Unsupported media type/format error
                        header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
                        return ResponseEntity("Unsupported Media Type/Format", header, HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    }

                } catch (e: Exception) {
                    dataset.end()
                    // if the query creation fails, send the error to the client
                    header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
                    return ResponseEntity("Error processing query: ${e.message}", header, HttpStatus.INTERNAL_SERVER_ERROR)
                } finally {
                    // end connection and transaction to TDB
                    dataset.end()
                }

                // respond to client
                header.add(HttpHeaders.CONTENT_TYPE, toMimeType(responseLang))
                return ResponseEntity(queryResponseBody, header, HttpStatus.OK)
            }


            "update" -> {
                // use Write access to modify TDB content
                dataset.begin(ReadWrite.WRITE)
                try {
                    // make write transaction
                    val update: UpdateRequest = UpdateFactory.create(request.query)

                    // create execution of the update
                    val updateProcessor: UpdateProcessor = UpdateExecutionFactory.create(update, dataset)

                    // execute update
                    updateProcessor.execute()

                    // end transaction
                    dataset.commit()

                } catch (e: Exception) {
                    // cancel transaction and end access before returning the error
                    dataset.abort()
                    dataset.end()

                    header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
                    return ResponseEntity("Error processing update: ${e.message}", header, HttpStatus.INTERNAL_SERVER_ERROR)
                } finally {
                    // close TDB access
                    dataset.end()
                }

                // there is not a response for the transaction
                // we'll make our own
                header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
                return ResponseEntity("SPARQL update executed correctly", header, HttpStatus.OK)
            }
            else -> {
                // we cannot infer the query type, so we'll send an error to the client
                header.add(HttpHeaders.CONTENT_TYPE, MimeType.text)
                return ResponseEntity("Type not specified", header, HttpStatus.BAD_REQUEST)
            }
        }
        // this section is not reachable c:
    }
}