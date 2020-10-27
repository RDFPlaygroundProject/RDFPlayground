package api.tdb

import org.apache.jena.query.Dataset
import org.apache.jena.query.ReadWrite
import org.apache.jena.rdf.model.Model
import org.apache.jena.tdb.TDBFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import api.TDBConfig
import formatAs
import toDOT

data class GetModelResponse(
    val data: String = "",
    val dot: String = ""
)

@RestController
class GetModelController {

    @GetMapping(
        "api/tdb/get_model"
    )
    fun getModel(): ResponseEntity<Any> {
        // Gets the contents of the TDB and returns it to the user
        val header = HttpHeaders()

        // get TDB directory
        val directory: String = TDBConfig.TDB_DIR
        val dataset: Dataset

        try {
            //access database
            dataset = TDBFactory.createDataset(directory)
        } catch (e: Exception) {
            // if we cannot access the database there is no point on continuing
            header.add(CONTENT_TYPE, MimeType.text)
            return ResponseEntity("Couldn't get TDB", header, HttpStatus.INTERNAL_SERVER_ERROR)
        }

        val modelString: String
        val dotModel: String

        dataset.begin(ReadWrite.READ)
        try {
            // get the model from the Database
            val model: Model = dataset.defaultModel
            // format as TTL
            modelString = model.formatAs("TTL").first
            // get DOT version
            dotModel = model.toDOT()
        } catch (e: Exception) {
            dataset.end()
            // there was an error on the model or converting it to DOT
            header.add(CONTENT_TYPE, MimeType.text)
            return ResponseEntity("Error processing Database: ${e.message}", header, HttpStatus.INTERNAL_SERVER_ERROR)
        } finally {
            // end connection
            dataset.end()
        }

        // send data to the user
        header.add(CONTENT_TYPE, MimeType.json)
        return ResponseEntity(
            GetModelResponse(data = modelString, dot = dotModel),
            header,
            HttpStatus.OK
        )
    }
}
