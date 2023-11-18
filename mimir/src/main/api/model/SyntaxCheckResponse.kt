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
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementFilter;
import com.google.common.hash.Hashing;


import dot.DOTLang
import loadModel

data class SyntaxCheckRequest(val data: String = "", val data_lang: String = "TTL")
data class SyntaxCheckResponse(val syntax_error: String = "", val data_dot: String = "")

@RestController
class SyntaxCheckController {
    var union_count = 0;
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


    fun processQueryPattern(element: Element, nodeIds: HashMap<String, MutableList<MutableMap<String, String> > >, dotRepresentation: StringBuilder, union: Int=0 ) {
        when (element) {
            is ElementGroup -> {
                println("group")
                for (subElement in element.elements) {
                    processQueryPattern(subElement, nodeIds, dotRepresentation, union)
                }
            }
            is ElementPathBlock -> {
                println("pathblock")
                // Process triple patterns
                for (triplePath in element.pattern) {
                    val triple = triplePath.asTriple()
                    val subject = triple.subject
                    val obj = triple.`object`
        
                    // Assign node values to subjects and objects
                    var subject_name = subject.toString()
                    var scolor = if (subject_name[1] == '_') "purple" else "lightblue"
                    var obj_name = obj.toString()
                    var ocolor = if (obj_name[1] == '_') "purple" else "lightblue"
                    val label = if (triple.predicate.toString() != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") triple.predicate else "rdf:type"
                    println("subject: $subject_name, object: $obj_name")
                    if(union > 0){
                        println("union: $union")
                        var new_subject = subject_name  + "_${union}"
                        var new_object = obj_name + "_${union}"
                        // Subject node
                        if (nodeIds.containsKey(subject_name)) {  // additional instance
                            var subject_entry = nodeIds.get(subject_name)
                            subject_entry?.add(
                                mutableMapOf("name" to new_subject, "shape" to "ellipse", 
                                             "color" to scolor, "draw" to "true"
                            ))
                        } else { // first instance
                            var new_entry = nodeIds.getOrPut(subject_name, {
                                mutableListOf(mutableMapOf("name" to subject_name,
                                                           "shape" to "ellipse", "color" to scolor, "draw" to "false"))})
                            new_entry.add(
                                mutableMapOf("name" to new_subject, "shape" to "ellipse", 
                                             "color" to scolor, "draw" to "true"))
                        }
                        // Object node
                        if (nodeIds.containsKey(obj_name)) { // additional instance
                            var object_entry = nodeIds.get(obj_name)
                            object_entry?.add(
                                mutableMapOf("name" to new_object, "shape" to "ellipse", 
                                             "color" to ocolor, "draw" to "true"))
                        } else { // first instance
                            var new_entry = nodeIds.getOrPut(obj_name, {
                                mutableListOf(mutableMapOf("name" to obj_name, 
                                                           "shape" to "ellipse", "color" to ocolor, "draw" to "false"))})
                            new_entry.add(
                                mutableMapOf("name" to new_object, "shape" to "ellipse", 
                                             "color" to ocolor, "draw" to "true"))
                        }
                        // Add DOT representation for edge representing the predicate
                        println("  \"$new_subject\" -> \"$new_object\" [label=\"${label}\"];\n")
                        dotRepresentation.append("  \"$new_subject\" -> \"$new_object\" [label=\"${label}\"];\n")
                    } else {
                        println("triple")
                        nodeIds.getOrPut(subject_name, {mutableListOf(mutableMapOf("name" to subject_name,"shape" to "ellipse", "color" to scolor, "draw" to "true"))})
                        nodeIds.getOrPut(obj_name, {mutableListOf(mutableMapOf("name" to obj_name, "shape" to "ellipse", "color" to ocolor, "draw" to "true"))})
                        // Add DOT representation for edge representing the predicate
                        dotRepresentation.append("  \"$subject_name\" -> \"$obj_name\" [label=\"${label}\"];\n")
                        println("  \"$subject_name\" -> \"$obj_name\" [label=\"${label}\"];\n")
                    }
                }
            }
            is ElementUnion -> {
                print("union")
                // Handle UNION patterns
                val union_name = Hashing.sha1().hashString(element.toString(), Charsets.UTF_8).toString().substring(0,7)    // Generate a unique identifier for the union
                nodeIds.getOrPut(union_name, {mutableListOf(mutableMapOf("name" to union_name, "shape" to "diamond", "color" to "orange", "draw" to "true"))})
                val connected_node = nodeIds.keys.first()
                for (subElement in element.elements) {
                    union_count += 1
                    // copy nodeIds to new map with each name suffixed with "_union_count"
                    processQueryPattern(subElement, nodeIds, dotRepresentation, union_count)
                    // Add DOT representation for edge representing the union to a node of the new_nodeIds
                    dotRepresentation.append("  \"$union_name\" -- \"${connected_node}_${union_count}\" [label=\"union\", color=\"gray\" ];\n")
                }
            }
            is ElementFilter -> {
                // Handle FILTER patterns
                // You can add specific handling for FILTER if needed
                // For this example, skipping it
            }
            // You might need to handle other types of query elements as required
            else -> {
                // Handle other elements or skip if not required
            }
        }
    }

    fun processFilter(element: Element, nodeIds: HashMap<String, MutableMap<String, String>>, dotRepresentation: StringBuilder, isFilterNotExists: Boolean) {
        // Process FILTER or FILTER NOT EXISTS patterns
        if (element is ElementFilter) {
            // Assuming you have logic to identify affected nodes and edges
            val affectedNodes = listOf("node1", "node2") // Replace this with the nodes affected by FILTER
            val affectedEdges = listOf("edge1", "edge2") // Replace this with the edges affected by FILTER
    
            // Modify affected nodes' borders or affected edges' colors based on the type of filter condition
            if (isFilterNotExists) {
                for (node in affectedNodes) {
                    nodeIds[node]?.put("color", "red") // Change node border color to red
                }
                for (edge in affectedEdges) {
                    dotRepresentation.append("  \"$edge\" [color=\"red\"];\n") // Change edge color to red
                }
            } else {
                for (edge in affectedEdges) {
                    dotRepresentation.append("  \"$edge\" [label=\"Condition\", color=\"green\", dir=\"none\"];\n") // Green undirected edge with condition label
                }
            }
        }
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
        union_count = 0
        // Parse the query string into a Query object
        val query = QueryFactory.create(queryString)

        // Get the query pattern from the parsed Query object
        val queryPattern: Element = query.queryPattern

        // Map to store unique identifiers for subjects and objects
        val nodeIds = HashMap<String, MutableList<MutableMap<String, String> > >()

        // Create DOT representation of the query pattern
        val dotRepresentation = StringBuilder("graph QueryPattern {\n")

    
        if (queryPattern is ElementGroup) {
            var selectVars = query.projectVars
            for (varName in selectVars) {
                var varNameStr = varName.toString()
                nodeIds.getOrPut(varNameStr, {mutableListOf(mutableMapOf("name" to varNameStr, "shape" to "ellipse", "color" to "yellow"))}  )
            }
            processQueryPattern(queryPattern, nodeIds, dotRepresentation, union_count)
    
            // Add DOT representation for node styles
            for ((_, subjectInstances) in nodeIds) {
                for (subject in subjectInstances) {
                    if (subject["draw"] == "true"){
                        var name = subject["name"]
                        var shape = subject["shape"]
                        var color = subjectInstances[0]["color"]
                        dotRepresentation.append("  \"$name\" [shape=$shape, fillcolor=\"$color\", style=\"filled\"];\n")
                    }
                }
            }
    
        }
    
        dotRepresentation.append("}")
        println("DOT Representation:")
        println(dotRepresentation.toString())
        for( (key, valu) in nodeIds) {
            println("key: $key")
            for (v in valu) {
                println("  $v")
            }
        }
        return dotRepresentation.toString()
    }
           
}

