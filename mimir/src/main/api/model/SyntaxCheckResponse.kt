package api.model

import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFLanguages
import java.io.ByteArrayOutputStream

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;


import dot.DOTLang
import loadModel

data class SyntaxCheckRequest(val data: String = "", val data_lang: String = "TTL")
data class SyntaxCheckResponse(val syntax_error: String = "", val data_dot: String = "")

@RestController
class SyntaxCheckController {

    @PostMapping(
        "/api/model/syntax_check",
        consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE],
        produces = [MediaType.APPLICATION_JSON_UTF8_VALUE]
    )
    fun syntaxCheck(@RequestBody requestBody: SyntaxCheckRequest): ResponseEntity<SyntaxCheckResponse> {
        // base case
        if (requestBody.data.isBlank()) {
            return ResponseEntity(SyntaxCheckResponse("Nothing received"), HttpStatus.NO_CONTENT)
        }

        // Create a model on Memory
        val model: Model
        try {
            model = loadModel(requestBody.data, requestBody.data_lang)
        } catch (e: Exception) {
            return ResponseEntity(SyntaxCheckResponse(syntax_error = e.message.toString()), HttpStatus.OK)
        }
        // Check if DOT lang is loaded
        val dotLang = DOTLang
        assert(RDFLanguages.isRegistered(dotLang)) {"Controller Error, DOT is not registered on Jena"}
        RDFLanguages.isRegistered(dotLang)

        // Convert to DOT and save on ByteArray Stream
        val modelOnDOT = ByteArrayOutputStream()
        val dotRepresentation: String // Store DOT representation
        try {
            // write to DOT
            RDFDataMgr.write(modelOnDOT, model, RDFFormat(DOTLang)) // Writes DOT to modelOnDOT

            // Get DOT representation from sparqlPatternToDot()
            dotRepresentation = sparqlPatternToDot()
        } catch (e: Exception) {
            return ResponseEntity(SyntaxCheckResponse(syntax_error = e.message.toString()), HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return ResponseEntity(SyntaxCheckResponse(data_dot =  dotRepresentation), HttpStatus.OK)
    }


    fun sparqlPatternToDot(): String {
        // Example SPARQL query string
        val queryString = """SELECT ?baryon ?comp WHERE {
            ?baryon a ?Baryon ; ?contains ?blank .
            ?blank ?component ?comp .
        }"""
    
        // Parse the query string into a Query object
        val query = QueryFactory.create(queryString)
    
        // Get the query pattern from the parsed Query object
        val queryPattern: Element = query.queryPattern
    
        // Create DOT representation of the query pattern
        val dotRepresentation = StringBuilder("graph QueryPattern {\n")
    
        if (queryPattern is ElementGroup) {
            val group = queryPattern
            // Process the elements in the group
            for (el in group.elements) {
                print("--")
                println(el)
                if (el is org.apache.jena.sparql.syntax.ElementPathBlock) {
                    // Retrieve triple pattern elements
                    val triple = el.pattern.get(0) // Assuming there's only one triple pattern in the query
    
                    // Extract subject, predicate, and object from the triple pattern
                    val subject = triple.subject
                    val predicate = triple.predicate
                    val obj = triple.`object`
    
                    // Add DOT representation for subject node
                    dotRepresentation.append("  \"${subject}\" [shape=ellipse];\n")
    
                    // Add DOT representation for object node
                    dotRepresentation.append("  \"${obj}\" [shape=ellipse];\n")
    
                    // Add DOT representation for edge representing the predicate
                    dotRepresentation.append("  \"${subject}\" -> \"${obj}\" [label=\"${predicate}\"];\n")
                }
            }
        }
    
        dotRepresentation.append("}")
    
        // Print DOT representation
        println("SPARQL Pattern:")
        println(queryPattern.toString())
        println("DOT Representation:")
        println(dotRepresentation.toString())
    
        return dotRepresentation.toString()
    }
    
}
