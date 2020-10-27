package api.tdb

import api.TDBConfig
import org.apache.jena.query.Dataset
import org.apache.jena.query.ReadWrite
import org.apache.jena.rdf.model.Model
import org.apache.jena.tdb.TDBFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class Status(val status_code: Int, val status: String, val tdb_status: String)

@RestController
class StatusController{

    @GetMapping("/api/tdb/status")
    fun status(): ResponseEntity<String> {
        // Make a TDB-backed dataset to work
        val directory: String = TDBConfig.TDB_DIR
        val dataset: Dataset

        try {
            dataset = TDBFactory.createDataset(directory)
        } catch (e: Exception) {
            return ResponseEntity("Triples DB is offline, try later", HttpStatus.SERVICE_UNAVAILABLE)
        }

        // Access the DB to ensure everything is working properly
        dataset.begin(ReadWrite.READ)
        try {
            // Try to get te model to check that everything is fine
            val model: Model = dataset.defaultModel
        } catch (e: Exception) {
            dataset.end()
            return ResponseEntity("Triples DB is not currently working", HttpStatus.INTERNAL_SERVER_ERROR)
        } finally {
            dataset.end()
        }

        return ResponseEntity("Triples DB on-line", HttpStatus.OK)
    }
}