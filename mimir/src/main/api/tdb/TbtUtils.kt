package api.tdb

import org.apache.jena.query.Query;
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
import org.apache.jena.sparql.expr.ExprFunction1
import org.apache.jena.sparql.expr.ExprFunction2
import org.apache.jena.sparql.expr.ExprFunctionN
import org.apache.jena.sparql.expr.ExprAggregator
// import Hashing
import com.google.common.hash.Hashing;


import dot.DOTLang
import loadModel
import kotlin.text.substring


var union_count:Int = 0;


fun processQueryPattern(element: Element, nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, 
                        dotRepresentation: MutableList<MutableList<Triple<String, String, String>>>, 
                        condList: MutableList<MutableList<MutableMap<String, String>>>,
                        follow_up: MutableList<Element>? = mutableListOf() ) {
    when (element) {
        is ElementGroup -> {
            //println("group")
            var i = 1
            val len_elements = element.elements.size
            for (subElement in element.elements) {
                //println("   subelement: $i \n$subElement")
                i = if (i == len_elements) len_elements - i else i 
                var follow_up_element: MutableList<Element>? = element.elements.subList(i,len_elements)
                follow_up_element?.addAll(follow_up!!)
                processQueryPattern(subElement, nodeIds, dotRepresentation, condList, follow_up_element)
                i++
            }
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
            
        }
        is ElementFilter -> {
            // determine if element is a FILTER or a FILTER NOT EXISTS
            if(element.toString().contains("NOT EXISTS")){
                processFilterNotExists(element, nodeIds, dotRepresentation, condList, follow_up)
            }else{
                processFilter(element, nodeIds, dotRepresentation, condList, follow_up)
            }
            
        } 
    }
}

fun processFilter(element: ElementFilter, nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, 
                dotRepresentation: MutableList<MutableList<Triple<String, String, String>>>, 
                condList: MutableList<MutableList<MutableMap<String, String>>>,
                follow_up: MutableList<Element>? = mutableListOf()) {
    
    println("filter")
    
    val expr = element.getExpr()
    println(expr)
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
                    expr.args.forEach { arg -> processExpr(arg, nodeIds, condList) }
                }
            }
        }
        is ExprVar -> {
            // Handle variable expressions
            println("Variable: ${expr.asVar()}")
            var var_name = expr.asVar().toString()
            val color = if (var_name[1] == '_') "#9054cc" else "lightgray"
            nodeIds[union_count].getOrPut(var_name, {mutableMapOf("name" to var_name, "shape" to "ellipse", "color" to color, "draw" to "true")}  )
        }
        is NodeValue -> {
            // Handle constant expressions
            println("Constant: ${expr}")
            var const_name = expr.toString()
            val name = const_name.substringAfterLast('/')
            val color = if (name.length > 0 && name[0] == '_') "#9054cc" else "lightblue"
            nodeIds[union_count].getOrPut(const_name, {mutableMapOf("name" to const_name, "shape" to "ellipse", "color" to color, "draw" to "true")}  )
        } 
    }
}


fun processFilterNotExists(element: ElementFilter, nodeIds: MutableList<MutableMap<String,MutableMap<String, String> > >, 
                dotRepresentation: MutableList<MutableList<Triple<String, String, String>>>, 
                condList: MutableList<MutableList<MutableMap<String, String>>>,
                follow_up: MutableList<Element>? = mutableListOf()) {
    println("filter not exists")

    // access elements from inside the FILTER
    val filter = element.getExpr()
    when (filter) {
        is ExprFunction -> {
            // Handle function expressions
            val funcExpr = filter 
            println("Function expression: " + funcExpr.functionIRI)
    
            // Get the arguments of the function
            val args = funcExpr.args
            for (arg in args) {
                println("Argument: $arg")
                // Process individual arguments as needed
            }
        }
        is ExprVar -> {
            // Handle variable expressions
            val varExpr = filter 
            println("Variable expression: $varExpr")
            // Process variable as needed
        }
        is NodeValue -> {
            // Handle constant expressions
            val constExpr = filter
            println("Constant expression: $constExpr")
            // Process constant value as needed
        }
        else -> {
            // Handle other types of expressions as required
            println("Other expression type")
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
                if (triple.first.contains("UNION") || triple.third.contains("UNION")) {
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
    //println("DOT Representation:")
    //println(dotRepresentation.toString())
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
        