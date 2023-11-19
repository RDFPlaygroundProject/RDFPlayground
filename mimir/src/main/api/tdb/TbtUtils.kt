package api.tdb

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.tdb.store.Hash



import dot.DOTLang
import loadModel


var union_count:Int = 0;


fun processQueryPattern(element: Element, nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, dotRepresentation: MutableList<MutableList<Triple<String, String, String>>>, follow_up: MutableList<Element>? = mutableListOf() ) {
    when (element) {
        is ElementGroup -> {
            println("group")
            var i = 1
            val len_elements = element.elements.size
            for (subElement in element.elements) {
                println("   subelement: $i \n$subElement")
                i = if (i == len_elements) len_elements - i else i 
                processQueryPattern(subElement, nodeIds, dotRepresentation, element.elements.subList(i,len_elements))
                i++
            }
        }
        is ElementPathBlock -> {
            println("pathblock")
            // Process triple patterns
            for (triplePath in element.pattern) {
                val triple = triplePath.asTriple()
                val subject = triple.subject
                val obj = triple.`object`
                println(triple)
    
                // Assign node values to subjects and objects
                var subject_name = subject.toString() 
                
                var scolor = "lightblue"
                if (subject_name[1] == '_') {
                    scolor = "#9054cc" // purple
                } else if (subject_name[0] == '?'){
                    scolor = "lightgray"
                } 
                var obj_name = obj.toString() 
                
                var ocolor =  "lightblue"
                if (obj_name[1] == '_') {
                    ocolor = "#9054cc" // purple
                } else if (obj_name[0] == '?'){
                    ocolor = "lightgray"
                } 
                println("    subject: $subject_name [$scolor | ${subject_name.substring(0,1)}]\n    object: $obj [$ocolor | ${subject_name.substring(0,1)}]] ")
                val label = if (triple.predicate.toString() != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") triple.predicate else "rdf:type"

                nodeIds[union_count].getOrPut(subject_name, {mutableMapOf("name" to subject_name, "shape" to "ellipse", "color" to scolor, "draw" to "true")}  )
                nodeIds[union_count].getOrPut(obj_name, {mutableMapOf("name" to obj_name, "shape" to "ellipse", "color" to ocolor, "draw" to "true")}  )
                
                // make triple of strings
                val triple_str = Triple(subject_name, label.toString(), obj_name)
                
                // Add DOT representation for the edge
                dotRepresentation[union_count].add(triple_str)
            }
        }
        is ElementUnion -> {
            println("union")
            // make union node and link up to first node of graph
            val union_name = "UNION_"+ union_count.toString() //Hashing.sha1().hashString(element.toString(), Charsets.UTF_8).toString().substring(0,7)    // Generate a unique identifier for the union
            val connected_node = nodeIds[union_count].keys.first()
            nodeIds[union_count].getOrPut(union_name, {mutableMapOf("name" to union_name, "shape" to "diamond", "color" to "orange", "draw" to "true")}  )
            

            // copy current graph into a new graph
            val copy_graph = mutableListOf<Triple<String, String, String>>()
            copy_graph.addAll(dotRepresentation[union_count])
            
            
            // copy all NodeIds from current graph into new graph
            val rightSideNodeIds = nodeIds[union_count].toMutableMap()

            val first_element = element.elements[0]
            val union_triple = Triple(union_name, "union", connected_node)
            processQueryPattern(first_element, nodeIds, dotRepresentation) 
            dotRepresentation[union_count].add(union_triple)
            
            if (follow_up is MutableList && follow_up.size > 0) {
                for (sublement in follow_up) {
                    println("follow up")
                    println(sublement)
                    processQueryPattern(sublement, nodeIds, dotRepresentation)
                }
            }
            println("-------------------")

            union_count += 1
            dotRepresentation.add(copy_graph)
            nodeIds.add(rightSideNodeIds)

            val second_element = element.elements[1]
            processQueryPattern(second_element, nodeIds, dotRepresentation)
            dotRepresentation[union_count].add(union_triple)
            
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

fun processFilter(element: Element, nodeIds: MutableMap<String, MutableMap<String, String>>, dotRepresentation: MutableList<MutableList<Triple<String, String, String>>>, isFilterNotExists: Boolean) {
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
                dotRepresentation[union_count].add(Triple("", "", "")) // Change edge color to red
            }
        } else {
            for (edge in affectedEdges) {
                dotRepresentation[union_count].add(Triple("", "", "")) // Green undirected edge with condition label
            }
        }
    }   
}

fun sparqlPatternToDot(queryString: String): String {
    union_count = 0
    // Parse the query string into a Query object
    val query = QueryFactory.create(queryString)

    // Get the query pattern from the parsed Query object
    val queryPattern: Element = query.queryPattern

    // Map to store unique identifiers for subjects and objects
    val nodeIds = mutableListOf<MutableMap<String, MutableMap<String, String> >>()
    
    nodeIds.add(mutableMapOf())

    // Create DOT representation of the query pattern
    val dotRepresentation = StringBuilder("graph QueryPattern {\n")
    val subGraphs = mutableListOf<MutableList<Triple<String, String, String>>>()
    subGraphs.add(mutableListOf<Triple<String, String, String>>())

    if (queryPattern is ElementGroup) {
        var selectVars = query.projectVars
        for (varName in selectVars) {
            var varNameStr = varName.toString()
            nodeIds[0].getOrPut(varNameStr, {mutableMapOf("name" to varNameStr, "shape" to "ellipse", "color" to "yellow")}  )
        }
        processQueryPattern(queryPattern, nodeIds, subGraphs)

        // concatenate all subgraphs
        var i = 0
        println("===========================")
        for (subGraphElem in subGraphs) {
            val subGraph = StringBuilder()
            for (triple in subGraphElem) {
                if (triple.first.contains("UNION") || triple.third.contains("UNION")) {
                    subGraph.append("  \"${triple.first}\" -- \"${triple.third}_${i}\" [label=\"${triple.second}\", color=\"gray\"];\n")
                    continue
                }
                subGraph.append("  \"${triple.first}_${i}\" -> \"${triple.third}_${i}\" [label=\"${triple.second}\"];\n")
            }
            dotRepresentation.append(subGraph.toString())
            val idMap = nodeIds[i]
            // add DOT definitions for each node of the subgraph
            idMap.forEach { (_, properties) ->
                var name = properties.getOrDefault("name", "").toString()
                val shape = properties.getOrDefault("shape", "ellipse").toString()
                val color = properties.getOrDefault("color", "lightblue").toString()
                println("name: $name  color: $color")
                if (name.isEmpty() ) return@forEach
                if (name.contains("UNION")){
                    dotRepresentation.append("  \"${name}\" [shape=$shape, fillcolor=\"$color\", style=\"filled\"];\n")
                    return@forEach
                }
                dotRepresentation.append("  \"${name}_${i}\" [shape=$shape, fillcolor=\"$color\", style=\"filled\"];\n")
            }
            i++
        }    
    }

    dotRepresentation.append("}")
    println("DOT Representation:")
    println(dotRepresentation.toString())
    var i = 0
    for( map in nodeIds){
        println("union: $i")
        for( (key, valu) in map) {
            println("  key: $key")
            for (v in valu) {
                println("    $v")
            }
        }
        i++
    }
    
    return dotRepresentation.toString()
}
        