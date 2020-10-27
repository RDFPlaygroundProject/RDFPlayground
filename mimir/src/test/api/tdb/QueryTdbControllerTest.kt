package api.tdb

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.http.ResponseEntity

internal class QueryTdbControllerTest {

    private val lineSep = System.lineSeparator()
    private var header = MimeType.text + ',' + MimeType.csv

    @Test
    fun selectQueryTdb() {
        val query: String = "SELECT (count(*) AS ?count) { ?s ?p ?o} LIMIT 10"
        val request: QueryTdbRequest = QueryTdbRequest(query, "query")

        val expected: String = "count${lineSep}"

        val response: ResponseEntity<String> = QueryTdbController().queryTdb(header, request)

        assertTrue(response.body!!.startsWith(expected))
    }

    @Test
    fun updateQueryTdb() {
        val query: String = """PREFIX : <http://example/>
            INSERT { :test :atTime ?now } WHERE { BIND(now() AS ?now) }""".trimIndent()
        val request: QueryTdbRequest = QueryTdbRequest(query, "update")

        header = MimeType.text

        val expected = "SPARQL update executed correctly"

        val response: ResponseEntity<String> = QueryTdbController().queryTdb(header, request)

        assertEquals(expected, response.body)
    }
}