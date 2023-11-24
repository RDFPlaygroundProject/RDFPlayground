package api.tdb

import org.apache.jena.query.Query;
import org.apache.jena.query.Syntax
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.Element1;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementNotExists
import org.apache.jena.tdb.store.Hash
import org.apache.jena.sparql.expr.ExprFunction
import org.apache.jena.sparql.expr.ExprList
import org.apache.jena.sparql.expr.Expr
import org.apache.jena.sparql.expr.ExprVar
import org.apache.jena.sparql.expr.NodeValue
import org.apache.jena.sparql.expr.ExprAggregator
import org.apache.jena.sparql.util.ExprUtils
import org.apache.jena.sparql.core.TriplePath
import org.apache.jena.sparql.syntax.ElementTriplesBlock
import org.apache.jena.sparql.syntax.ElementOptional
// import Hashing
import com.google.common.hash.Hashing;


import dot.DOTLang
import loadModel
import kotlin.text.substring
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableListOf


var union_count:Int = 0;


fun processQueryPattern(element: Element, nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, 
                        dotRepresentation: MutableList<MutableList<Triple<String, String, String>>>, 
                        condList: MutableList<MutableList<MutableMap<String, String>>>,
                        follow_up: MutableList<Element>? = mutableListOf() ) : Pair<Int,Element>? {
    when (element) {
        is ElementGroup -> {
            //println("group")
            val start_idx = union_count
            val optionals = mutableListOf<Pair<Int, Element>>()
            var i = 1
            val len_elements = element.elements.size
            for (subElement in element.elements) {
                //println("   subelement: $i \n$subElement")
                i = if (i == len_elements) len_elements - i else i 
                var follow_up_element: MutableList<Element>? = element.elements.subList(i,len_elements)
                follow_up_element?.addAll(follow_up!!)
                val optional = processQueryPattern(subElement, nodeIds, dotRepresentation, condList, follow_up_element)
                val idx = optional?.first
                val pattern = optional?.second
                val optional_elems = if (pattern is ElementGroup && idx != null) pattern else null
                if (optional_elems != null ){
                    optionals.add(Pair(idx!!, optional_elems))
                }
                i++
            }
            var optional_count = 1
            for(pair in optionals){

                val optional_pattern = pair.second
                //println("optional pattern: $optional_pattern")
                val optional_idx = pair.first
                val optional_range = "${start_idx}..${optional_idx + optional_count}"
                val optional_name = "OPTIONAL_${optional_range}"

                val optional_graph = mutableListOf<Triple<String, String, String>>() // graph shape
                val optional_conds = mutableListOf<MutableMap<String, String>>()    // graph conditions
                val optional_nodeIds = mutableMapOf<String, MutableMap<String, String>>()      // graph nodes
                

                union_count += 1
                nodeIds.add(optional_nodeIds)
                condList.add(optional_conds)
                dotRepresentation.add(optional_graph)
                

                processQueryPattern(optional_pattern, nodeIds, dotRepresentation, condList)
                

                // update the optional graph's nodeIds with information of the current graph
                // such that only nodes that are in the optional graph receive previously
                // known information
                for (node in nodeIds[union_count-1]) {
                    if (node.key in nodeIds[union_count]) {
                        nodeIds[union_count][node.key] = node.value
                    }
                }
                
                // add optional node to graph
                for (node in nodeIds[union_count]) {
                    val optional_triple = Triple(optional_name, "optional", node.key )
                    dotRepresentation[union_count].add(optional_triple)
                }
                nodeIds[union_count].getOrPut(optional_name, {mutableMapOf("name" to optional_name, "shape" to "diamond", "color" to "pink", "draw" to "true")}  )
                

                // swap the optional graph with the current graph
                val temp_graph = dotRepresentation[union_count-1]
                dotRepresentation[union_count-1] = dotRepresentation[union_count]
                dotRepresentation[union_count] = temp_graph
                val tem_cond = condList[union_count-1]
                condList[union_count-1] = condList[union_count]
                condList[union_count] = tem_cond
                val temp_nodes = nodeIds[union_count-1]
                nodeIds[union_count-1] = nodeIds[union_count]
                nodeIds[union_count] = temp_nodes

                optional_count += 1
            }
            return null
        }
        is ElementPathBlock -> {
            //println("pathblock")
            // Process triple patterns
            for (triplePath in element.pattern) {
                val triple = triplePath.asTriple()
                val subject = triple.subject
                val obj = triple.`object`
                //println(triple)
    
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
                //println("    subject: $subject_name [$scolor | ${subject_name.substring(0,1)}]\n    object: $obj [$ocolor | ${subject_name.substring(0,1)}]] ")
                val label = if (triple.predicate.toString() != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") triple.predicate else "rdf:type"

                nodeIds[union_count].getOrPut(subject_name, {mutableMapOf("name" to subject_name, "shape" to "ellipse", "color" to scolor, "draw" to "true")}  )
                nodeIds[union_count].getOrPut(obj_name, {mutableMapOf("name" to obj_name, "shape" to "ellipse", "color" to ocolor, "draw" to "true")}  )
                
                // make triple of strings
                val triple_str = Triple(subject_name, label.toString(), obj_name)
                
                // Add DOT representation for the edge
                dotRepresentation[union_count].add(triple_str)
            }
            return null
        }
        is ElementUnion -> {
            //println("union")
            // make union node and link up to first node of graph
            val union_name = "UNION_"+ union_count.toString() //Hashing.sha1().hashString(element.toString(), Charsets.UTF_8).toString().substring(0,7)    // Generate a unique identifier for the union
            val connected_node = nodeIds[union_count].keys.first()
            nodeIds[union_count].getOrPut(union_name, {mutableMapOf("name" to union_name, "shape" to "diamond", "color" to "orange", "draw" to "true")}  )
            

            // copy current graph information to preserve for second branch of union
            val copy_graph = mutableListOf<Triple<String, String, String>>() // graph shape
            copy_graph.addAll(dotRepresentation[union_count])

            val copy_conds = mutableListOf<MutableMap<String, String>>()    // graph conditions
            copy_conds.addAll(condList[union_count])
            
            val rightSideNodeIds = nodeIds[union_count].toMutableMap()      // graph nodes

            val first_element = element.elements[0]
            val union_triple = Triple(union_name, "union", connected_node)
            processQueryPattern(first_element, nodeIds, dotRepresentation, condList) 
            dotRepresentation[union_count].add(union_triple)
            
            if (follow_up is MutableList && follow_up.size > 0) {
                for (sublement in follow_up) {
                    //println("follow up")
                    //println(sublement)
                    processQueryPattern(sublement, nodeIds, dotRepresentation, condList)
                }
            }
            //println("-------------------")

            union_count += 1
            // set copied elements to new union's context
            dotRepresentation.add(copy_graph)
            nodeIds.add(rightSideNodeIds)
            condList.add(copy_conds)

            val second_element = element.elements[1]
            processQueryPattern(second_element, nodeIds, dotRepresentation, condList)
            dotRepresentation[union_count].add(union_triple)
            return null
        }
        is ElementFilter -> {
            // determine if element is a FILTER or a FILTER NOT EXISTS
            if(element.toString().contains("NOT EXISTS")){
                processFilterNotExists(element, nodeIds, condList)
            }else{
                processFilter(element, nodeIds,condList)
            }
            return null
        } 
        is ElementOptional -> {
            //println(element)
            var optionals = element.getOptionalElement()
            var optionals_str =  """SELECT * WHERE ${optionals.toString()}"""
            var query = QueryFactory.create( optionals_str )
            val pattern = query.queryPattern as ElementGroup;
            // find way to graph an optional, then recursive call

            return Pair(union_count, pattern)

            // whatever is needed to graph an optional
        }
    }
    return null;
}

fun processFilter(element: ElementFilter, nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, 
                condList: MutableList<MutableList<MutableMap<String, String>>>) {
    val expr = element.getExpr()
    processExpr(expr, nodeIds, condList)
}

fun processExpr(expr: Expr, nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, 
                condList: MutableList<MutableList<MutableMap<String, String>>>,) {
    when (expr) {
        is ExprFunction -> {
            var func = expr.getOpName() + "_" + Hashing.sha1().hashString(expr.toString() + union_count.toString(), Charsets.UTF_8).toString().substring(0,7)
            when (expr.args.size){
                1 -> {
                    val arg = expr.args[0]
                    processExpr(arg, nodeIds, condList)
                    condList[union_count].add(
                        mutableMapOf(
                        "function" to func, "subject" to arg.toString(), 
                        "predicate" to "", "object" to func, 
                        "card" to "unary", "type" to "filter"
                        )
                    )
                }
                2 -> {
                    val arg1 = expr.args[0]
                    val arg2 = expr.args[1]
                    processExpr(arg1, nodeIds, condList)
                    processExpr(arg2, nodeIds, condList)
                    condList[union_count].add(
                        mutableMapOf(
                        "function" to func, "subject" to arg1.toString(),
                        "predicate" to func, "object" to arg2.toString(), 
                        "card" to "binary", "type" to "filter"
                        )
                    )
                }
                else -> {
                    expr.args.forEach { arg -> run{
                        processExpr(arg, nodeIds, condList)
                        condList[union_count].add(
                            mutableMapOf(
                                "function" to func, "subject" to arg.toString(),
                                "predicate" to "", "object" to func,
                                "card" to "n-ary", "type" to "filter"
                            )
                        )
                    } }
                }
            }
        }
        is ExprVar -> {
            // Handle variable expressions
            //println("Variable: ${expr.asVar()}")
            var var_name = expr.asVar().toString()
            val color = if (var_name[1] == '_') "#9054cc" else "lightgray"
            nodeIds[union_count].getOrPut(var_name, {mutableMapOf("name" to var_name, "shape" to "ellipse", "color" to color, "draw" to "true")}  )
        }
        is NodeValue -> {
            // Handle constant expressions
            //println("Constant: ${expr}")
            var const_name = expr.toString()
            val name = const_name.substringAfterLast('/', "LITERAL")
            val color: String;
            if (name != "LITERAL") {
                if (name[0] == '_'){
                    color = "#9054cc" // purple
                } else {
                    color = "lightblue"
                }
            } else {
                color = "#00ff00"
            }
            nodeIds[union_count].getOrPut(const_name, {mutableMapOf("name" to const_name, "shape" to "ellipse", "color" to color, "draw" to "true")}  )
        } 
    }
}




fun processFilterNotExists(element: ElementFilter, nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, 
                condList: MutableList<MutableList<MutableMap<String, String>>>) {
    // access elements from inside the FILTER
    val filter = element.getExpr() // (notexists (bgp (triple ?s ?p ?o) ... ))
    var filter_str = filter.toString().substringAfter("bgp", "ERROR ").dropLast(2).trimIndent() // get the filter's graph
    
    val elements : MutableList<Triple<String, String, String>> = mutableListOf()
    var filter_triples = filter_str.split("(triple")
    //println(filter_triples)
    filter_triples.forEach { pattern -> 
        if (pattern.isNotBlank()) {
            var parts = pattern.trim().split(" ")
            if (parts.size == 1) {
                return@forEach
            }
            var subject = parts[0].trim()
            var predicate = parts[1].trim()
            var obj = parts[2].trimEnd(')')

            val triple = Triple(subject, predicate, obj)
            elements.add(triple)
        }
    }
    processNotExistsExpr(elements, nodeIds, condList)
}

fun processNotExistsExpr(elements: MutableList<Triple<String, String, String>>, 
            nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, 
            condList: MutableList<MutableList<MutableMap<String, String>>>) {
    //println("process not exists expr")
    //println(elements)
    for (triple in elements){
        val subject = triple.first
        val predicate = triple.second
        val obj = triple.third

        // Assign node values to subjects and objects
        var subject_name = subject.toString() 
        
        var scolor = "red"
        var obj_name = obj.toString() 
        
        var ocolor = "red"
        //println("    subject: $subject_name [$scolor | ${subject_name.substring(0,1)}]\n    object: $obj [$ocolor | ${subject_name.substring(0,1)}]] ")
        val label = if (predicate.toString() != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") predicate else "rdf:type"

        nodeIds[union_count].getOrPut(subject_name, {mutableMapOf("name" to subject_name, "shape" to "ellipse", "color" to scolor, "draw" to "true")}  )
        nodeIds[union_count].getOrPut(obj_name, {mutableMapOf("name" to obj_name, "shape" to "ellipse", "color" to ocolor, "draw" to "true")}  )
        
        
        // Add DOT representation for the edge
        condList[union_count].add(
            mutableMapOf(
                "function" to "notexists", "subject" to subject_name,
                "predicate" to label, "object" to obj_name,
                "card" to "binary", "type" to "notExists"
            )
        )
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

    val condsList = mutableListOf<MutableList<MutableMap<String, String>>>()
    condsList.add(mutableListOf<MutableMap<String, String>>())

    if (queryPattern is ElementGroup) {
        var selectVars = query.projectVars
        for (varName in selectVars) {
            var varNameStr = varName.toString()
            nodeIds[0].getOrPut(varNameStr, {mutableMapOf("name" to varNameStr, "shape" to "ellipse", "color" to "yellow")}  )
        }

        // Process the SparQL query's WHERE clause
        processQueryPattern(queryPattern, nodeIds, subGraphs, condsList)

        // concatenate all subgraphs
        var i = 0
        //println("===========================")

        // Draw all branches of the query (query branches by UNIONs)
        for (subGraphElem in subGraphs) {
            val subGraph = StringBuilder()

            // add DOT definitions for each edge of the subgraph
            for (triple in subGraphElem) {
                if (triple.first.contains("UNION") || triple.first.contains("OPTIONAL") ) {
                    subGraph.append("  \"${triple.first}\" -- \"${triple.third}_${i}\" [label=\"${triple.second}\", color=\"gray\"];\n")
                    continue
                }
                subGraph.append("  \"${triple.first}_${i}\" -> \"${triple.third}_${i}\" [label=\"${triple.second}\"];\n")
            }
            dotRepresentation.append(subGraph.toString())

            // add DOT definitions for each condition of the subgraph
            val conds = condsList[i]
            for (condMap in conds) {
                val cond = StringBuilder()
                val condType = condMap["type"]
                val condFunc = condMap["function"]
                val condSubject = condMap["subject"]
                val condObject = condMap["object"]
                val condPredicate = condMap["predicate"]
                val condCardinality = condMap["card"]
                

                if (condType == "filter") {
                    if (condCardinality == "unary") {
                        cond.append("  \"${condSubject}_${i}\" -> \"${condFunc}\" [label=\"\", color=\"lightgreen\"];\n")
                        cond.append("  \"${condFunc}\" [shape=\"box\", fillcolor=\"lightgreen\", style=\"filled\"];\n")
                    } else if (condCardinality == "binary") {
                        val func = condFunc!!.substringBefore("_", "ERROR")
                        cond.append("  \"${condSubject}_${i}\" -> \"${condObject}_${i}\" [label=\"${func}\", color=\"lightgreen\"];\n")
                    }
                } else if (condType == "notExists") {
                    cond.append("  \"${condSubject}_${i}\" -> \"${condObject}_${i}\" [label=\"${condPredicate}\", color=\"red\"];\n")
                }
                dotRepresentation.append(cond.toString())
            }
            
            // add DOT definitions for each node of the subgraph
            val idMap = nodeIds[i]
            idMap.forEach { (_, properties) ->
                var name = properties.getOrDefault("name", "").toString()
                val shape = properties.getOrDefault("shape", "ellipse").toString()
                val color = properties.getOrDefault("color", "lightblue").toString()
                //println("name: $name  color: $color")

                // Serialize each node such that it stays on it's branch
                if (name.isEmpty() ) return@forEach
                if (name.contains("UNION") || name.contains("OPTIONAL")) {
                    dotRepresentation.append("  \"${name}\" [shape=$shape, fillcolor=\"$color\", style=\"filled\"];\n")
                    return@forEach
                }
                dotRepresentation.append("  \"${name}_${i}\" [shape=$shape, fillcolor=\"$color\", style=\"filled\"];\n")
            }
            i++
        }    
    }

    dotRepresentation.append("}")
    //println("DOT Representation:")
    //println(dotRepresentation.toString())
    var i = 0
    for( map in nodeIds){
        //println("union: $i")
        for( (key, valu) in map) {
            //println("  key: $key")
            for (v in valu) {
                //println("    $v")
            }
        }
        i++
    }
    
    return dotRepresentation.toString()
}
        