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
import org.apache.jena.tdb.store.Hash


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
        return ResponseEntity(SyntaxCheckResponse(data_dot = dotRepresentation ), HttpStatus.OK)
    }


    fun sparqlPatternToDot(): String {
        // Example SPARQL query string
        val queryString = """
        SELECT DISTINCT ?particle 
        WHERE {
            {?particle ?Interaction ?force . }
            UNION {
                ?particle ?Contains ?_ .
                ?_ ?Component ?component .
                ?component ?Interaction ?force .
            }
            ?boson ?Mediates ?force .
            FILTER (?boson = ?Gluon || ?boson = ?Photon)  
        } 
        """
        // Parse the query string into a Query object
        val query = QueryFactory.create(queryString)
    
        // Get the query pattern from the parsed Query object
        val queryPattern: Element = query.queryPattern
    
        // Map to store unique identifiers for subjects and objects
        val nodeIds = HashMap<String, MutableMap<String, String> >()
    
        // Create DOT representation of the query pattern
        val dotRepresentation = StringBuilder("graph QueryPattern {\n")
    
        if (queryPattern is ElementGroup) {
            val group = queryPattern
            val selectVars = query.projectVars
            for (variable in selectVars) {
                val varName = variable.name
                nodeIds.getOrPut("?$varName", { mutableMapOf("shape" to "ellipse", "color" to "yellow") })
            }

            // Process the elements in the group
            for (el in group.elements) {
                if (el is org.apache.jena.sparql.syntax.ElementPathBlock) {
                    val patternBlock = el.pattern
                    // Loop through all triple patterns in the block
                    for (triplePath in patternBlock) {
                        val triple = triplePath.asTriple()
                        val subject = triple.subject
                        val obj = triple.`object`
    
                        // Assign node values to subjects and objects
                        var subject_name = subject.toString()
                        var color = if (subject_name[1] == '_') "purple" else "lightblue"
                        var obj_name = obj.toString()
                        nodeIds.getOrPut(subject_name, { mutableMapOf("shape" to "ellipse", "color" to color) })
                        color = if (obj_name[1] == '_') "purple" else "lightblue"
                        nodeIds.getOrPut(obj_name, { mutableMapOf("shape" to "ellipse", "color" to color) })
    
                        // Add DOT representation for edge representing the predicate
                        val label = if (triple.predicate.toString() != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") triple.predicate else "rdf:type"
                        dotRepresentation.append("  \"$subject_name\" -> \"$obj_name\" [label=\"${label}\"];\n")
                    }
                }
            }
            // Add DOT representation for subject node if not already added
            for ((subject_name, subject) in nodeIds){
                var shape = subject.get("shape")
                var color = subject.get("color")
                dotRepresentation.append("  \"$subject_name\" [shape=$shape, fillcolor=\"$color\", style=\"filled\"];\n")
            }
        }
    
        dotRepresentation.append("}")
    
        // Print DOT representation
        println("DOT Representation:")
        println(dotRepresentation.toString())
    
        return dotRepresentation.toString()
    }
                
}
