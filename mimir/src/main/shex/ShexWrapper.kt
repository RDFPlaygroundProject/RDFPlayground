package shex

import api.ShexConfig
import fr.inria.lille.shexjava.GlobalFactory
import fr.inria.lille.shexjava.schema.Label
import fr.inria.lille.shexjava.schema.ShexSchema
import fr.inria.lille.shexjava.schema.parsing.GenParser
import fr.inria.lille.shexjava.validation.ValidationAlgorithm
import isAbbreviatedIRI
import loadModel
import org.apache.commons.rdf.api.Graph
import org.apache.commons.rdf.api.IRI
import org.apache.commons.rdf.rdf4j.RDF4J
import org.apache.jena.atlas.io.IndentedWriter
import org.apache.jena.riot.system.PrefixMap
import org.apache.jena.riot.system.PrefixMapFactory
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import writeTo
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths

class ShexWrapper(private val factory: RDF4J = RDF4J()) {
    private val modelPrefixMap: PrefixMap = PrefixMapFactory.create()
    private val shapePrefixMap: PrefixMap = PrefixMapFactory.create()

    init {
        GlobalFactory.RDFFactory = factory // set global factory for ShexJava
    }

    fun createDataGraph(data: String, lang: String): Graph {
        // Convert Data to Turtle (on Jena)
        val dataModel = loadModel(data, lang)
        val dataOnTTL: String = dataModel.writeTo(org.apache.jena.riot.RDFFormat.TTL)

        val baseIRI = "http://example.org/"

        // use RDF4J with eclipse to create apache.commons.rdf.api.Graph
        val eclipseModel: org.eclipse.rdf4j.model.Model = Rio.parse(
            dataOnTTL.reader(),
            baseIRI,
            RDFFormat.TURTLE
        )

        // generate data model prefixMap
        modelPrefixMap.putAll(dataModel.nsPrefixMap)

        // Get a graph from the data
        return factory.asGraph(eclipseModel)
    }

    fun createSchema(shape: String): ShexSchema {
        // Load schema to shex java
        val tempDirectory = Paths.get(ShexConfig.TMP_DIR_NAME).toFile()
        val tempShexFile: File = createTempFile(
            ShexConfig.TMP_NAME_PREFIX,
            ShexConfig.TMP_NAME_SUFFIX,
            tempDirectory
        )
        tempShexFile.writeText(shape, Charsets.UTF_8)

        // create custom prefixMapping from Schema
        generatePrefixMap(shapePrefixMap, tempShexFile)

        val schema: ShexSchema = GenParser.parseSchema(factory, tempShexFile.toPath())
        tempShexFile.delete()
        return schema
    }

    private fun generatePrefixMap(prefixMap: PrefixMap, file: File) {
        // look for prefixes to create our map
        val regex = Regex("^prefix\\s+[a-zA-Z]*:\\s+<(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>|PREFIX\\s+[a-zA-Z]*:\\s+<(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>")
        file.forEachLine {
            when {
                it.contains(regex) -> {
                    var prefixString = it.trim()
                    // get prefix abbreviation
                    prefixString = prefixString.removePrefix("prefix ").removePrefix("PREFIX ").trimStart()
                    val abbreviation = prefixString.substringBefore(':') + ':'
                    val extended = prefixString.substringAfter('<').substringBefore('>')
                    prefixMap.add(abbreviation, extended)
                }
            }
        }
    }

    fun parseMap(shapeMap: String): List<ShexShapeMap> {
        val mappingList: MutableList<ShexShapeMap> = mutableListOf()

        // format map
        shapeMap.trim()
        shapeMap.replace("\n\r ".toRegex(), "") // delete line breaks betweeen statements

        // convert to List
        val elements = shapeMap.split(",", ";")
        elements.forEach { element ->
            run {
                if (!element.contains('@')) {
                    throw Exception("Bad Mapping Syntax: missing @, use nodeLabel@shapeLabel syntax")
                } else {
                    val pair = element.split('@')
                    if (pair.size != 2) {
                        throw Exception("Bad Mapping Syntax: missing element on mapping")
                    } else {
                        // shex java uses exclusively expanded IRIs, so here we create expanded
                        // versions for any mapping and save the user input to be printed later

                        // Node label processing
                        val nodeLabel = pair[0].trim()
                        var expandedNodeLabel = nodeLabel.trim('<').trim('>')
                        // expand node IRI for shex java
                        if (pair[0].trim().isAbbreviatedIRI()) expandedNodeLabel = modelPrefixMap.expand(nodeLabel)
                        val node: IRI = factory.createIRI(expandedNodeLabel)

                        // Shape label processing
                        val shapeLabel = pair[1].trim('<').trim('>')
                        var expandedShapeLabel = shapeLabel
                        if (shapeLabel.isAbbreviatedIRI()) expandedShapeLabel = shapePrefixMap.expand(shapeLabel)
                        val shape: Label = Label(factory.createIRI(expandedShapeLabel))

                        // save to mapping object
                        mappingList.add(
                            ShexShapeMap(
                                node, // RDF term
                                shape, // shape label
                                pair[0].trim(), // original node as given
                                pair[1].trim()  // original shape as given
                            ) // save original terms
                        )
                    }
                }
            }
        }
        return mappingList.toList()
    }

    fun validateMapping(validationAlgorithm: ValidationAlgorithm, shapeMapping: List<ShexShapeMap>): String {
        val baos = ByteArrayOutputStream()
        val writer: IndentedWriter = IndentedWriter(baos)
        writer.println("ShEx Validation Report:")
        writer.incIndent() // adds an indentation to each line

        shapeMapping.forEach { mapping: ShexShapeMap ->
            // Validate pair
            validationAlgorithm.validate(mapping.nodeLabel, mapping.shapeLabel)

            // Print results for each pair
            when (validationAlgorithm.typing.isConformant(mapping.nodeLabel, mapping.shapeLabel)) {
                true ->
                    writer.println("✅ ${mapping.originalNode} conforms with ${mapping.originalShape}")
                false ->
                    writer.println("❌ ${mapping.originalNode} does not conform with ${mapping.originalShape}")
            }
        }
        writer.flush()
        return baos.toString()
    }
}