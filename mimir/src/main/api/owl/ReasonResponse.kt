package api.owl

import api.ReasonerConfig
import dot.colorTemplate.ReasonerNodeColor
import loadModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.system.PrefixMapFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reasoner.OwlRLWrapper
import toAlternateColoursDOT
import toDOT
import writeTo

data class ReasonRequest(
    val data: String = "",
    val data_lang: String = "TTL",
    val profile: String = ReasonerConfig.DEFAULT_PROFILE
)
data class ReasonResponse(
    val data: String = "",
    val error: String = "",
    val data_dot: String = ""
)

@RestController
class ReasonController {

    @PostMapping(
        "/api/owl/reason",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
        produces = [MediaType.APPLICATION_JSON_UTF8_VALUE, TEXT_PLAIN_VALUE]
    )
    fun reason(
        @RequestBody requestBody: ReasonRequest
    ): ResponseEntity<Any> {
        val header = HttpHeaders()
        if (requestBody.data.isBlank()) return ResponseEntity(ReasonResponse(),HttpStatus.NO_CONTENT)

        var data: String = requestBody.data
        val reasoner = OwlRLWrapper()

        try {
            // convert to TTL if not TTL
            if (requestBody.data_lang != "TTL")
                data = loadModel(data, requestBody.data_lang).writeTo(RDFFormat.TTL)

            // Call reasoner and get the result
            val (inferred, error) = reasoner.infer(data = data, profile = requestBody.profile)
            var dot: String = ""
            
            // try to convert to DOT (fails when there is literals, because it owlrl uses them as subjects) 
            dot = try {
                loadModel(inferred, "TTL").toDOT()
            } catch (e: Exception) {
                println("Error reasoning, DOT creation failed: \n reason: ${e.message}")
                // display an empty DOT
                "digraph G{\n  charset=\"utf-8\";\n  \n  // Edges\n  \n  // Nodes\n}\n"
            }

            // try to get a DOT of the original model and one of the difference
            // between original and inferred
            val dataModel: Model = loadModel(data, requestBody.data_lang)
            val dataDot: String = dataModel.toDOT()
            // exclude elements that are in the original model
            val diffModel: Model = loadModel(inferred, "TTL").difference(dataModel)
            // convert to a differently coloured DOT
            val diffDOT = diffModel.toAlternateColoursDOT(
                ReasonerNodeColor,
                prefixMap = PrefixMapFactory.create(dataModel.nsPrefixMap)
            )

            // Mix this DOT strings to display them with different pallets
            val mixDOT: String = deleteRepeatedNodes(original = dataDot, other = diffDOT)

            dot = mixDOT
            
            header.add(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            return ResponseEntity(ReasonResponse(inferred, error, dot), header, HttpStatus.OK)

        } catch (e: Exception) {
            println("Error while reasoning, aborting with Error: ${e.message}")
            header.add(CONTENT_TYPE, TEXT_PLAIN_VALUE)
            return ResponseEntity("Internal Server Error", header, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}

fun deleteRepeatedNodes(original: String, other: String): String {
    val originLines: List<String> = original.reader().readLines()
    val otherLines: List<String> = other.reader().readLines()

    // final Nodes and Edges
    val nodes = mutableListOf<String>()
    val edges = mutableListOf<String>()

    val nodeRegex: Regex = """"[-a-zA-Z0-9+&@#/%?=~_|!:,.; ]+" \[""".toRegex()

    // Clean lines
    originLines.forEach { line: String ->
        // process this lines before the __other__ ones
        when {
            line.isBlank() -> {} // do nothing
            line.trim().contains(" -> ") -> edges.add(line.trim())
            nodeRegex.containsMatchIn(line.trim()) -> nodes.add(line.trim())
        }
    }

    // Add only the nodes that are not on the original, and all the edges
    otherLines.forEach { line: String ->
        when {
            line.isBlank() -> {}
            line.trim().contains(" -> ") -> edges.add(line.trim())
            nodeRegex.containsMatchIn(line.trim()) -> {
                // FIXME: un-hardcode
                // temp Line is hardcoded to not include the color
                val tempLine: String = line.substring(0, line.length - 7).trim()
                // see if exist already, add it if it doesn't
                if (nodes.none { nodeLine -> nodeLine.startsWith(tempLine) }) {
                    nodes.add(line.trim())
                }
            }
        }
    }

    // build DOT document
    return """
digraph G{

    charset="utf-8";
    
    // Edges
    ${(edges.map { it }).joinToString(separator = "\n\t")}
    
    // Nodes
    ${(nodes.map { it }).joinToString(separator = "\n\t")}
    
}
    """.trimIndent()
}
