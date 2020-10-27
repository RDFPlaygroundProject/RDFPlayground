import org.apache.jena.query.ResultSet
import org.apache.jena.query.ResultSetFormatter.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.sparql.sse.SSE
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import kotlin.text.contains

import dot.DOTLang
import dot.DOTShell
import dot.NodeColoring
import org.apache.commons.rdf.api.Graph
import org.apache.jena.atlas.io.IndentedWriter
import org.apache.jena.query.Dataset
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.system.PrefixMap
import org.apache.jena.riot.system.PrefixMapBase
import org.apache.jena.riot.system.PrefixMapFactory
import org.apache.jena.riot.system.PrefixMapWrapper
import org.apache.jena.shacl.ValidationReport
import org.apache.jena.shacl.lib.ShLib
import org.apache.jena.sparql.util.Context

fun loadModel(data: String, lang: String): Model {
    // Load data on to a model and returns the model
    val model = ModelFactory.createDefaultModel()
    val inputStream = ByteArrayInputStream(data.toByteArray(Charsets.UTF_8))
    try {
        model.read(inputStream, null, lang)
    } catch (e: Exception) {
        throw e
    }
    return model
}

fun Model.writeTo(lang: RDFFormat): String {
    // Writes to specified Lang
    val baos = ByteArrayOutputStream()
    try {
        RDFDataMgr.write(baos, this, lang)
    } catch (e: Exception) {
        throw e
    }
    return baos.toString()
}

fun Model.toDOT(): String {
    return this.writeTo(RDFFormat(DOTLang))
}

fun Model.toAlternateColoursDOT(colors: NodeColoring, prefixMap: PrefixMap = PrefixMapFactory.create(this.nsPrefixMap)): String {
    // Uses the internal DOT Writer added to Jena, with a different
    // set of colors for the nodes
    val graph = this.graph
    val baos = ByteArrayOutputStream()
    val indWriter = IndentedWriter(baos)
    val shell = DOTShell(indWriter, prefixMap, baseURI = "rdfplayground.dcc.uchile.cl", context = Context.emptyContext)
    shell.writeGraphDOT(graph, colors)

    indWriter.flush()
    return baos.toString()
}

fun ResultSet.formatAs(format: String): String {
    val set: ResultSet = this
    val outStream = ByteArrayOutputStream()
    // Write to ByteArray stream using ResultSetFormatter and return as String
    when (format) {
        "Text" -> out(outStream, set)
        "XML" -> outputAsXML(outStream, set)
        "SSE" -> outputAsSSE(outStream, set)
        "CSV" -> outputAsCSV(outStream, set)
        "JSON" -> outputAsJSON(outStream, set)
        "TSV" -> outputAsTSV(outStream, set)
        "TTL" -> output(outStream, set, Lang.TURTLE)
        else -> throw Exception("Unsupported language")
    }
    return outStream.toString()
}

fun Boolean.formatAskAs(format: String): String {
    val bool: Boolean = this
    val outStream = ByteArrayOutputStream()
    // Write Ask response to ByteArray stream using ResultSetFormatter and return as String
    when (format) {
        "Text" -> out(outStream, bool)
        "XML" -> outputAsXML(outStream, bool)
        "SSE" -> outputAsSSE(outStream, bool)
        "CSV" -> outputAsCSV(outStream, bool)
        "JSON" -> outputAsJSON(outStream, bool)
        "TSV" -> outputAsTSV(outStream, bool)
        "TTL" -> output(outStream, bool, Lang.TURTLE)
        else -> throw Exception("Unsupported language")
    }
    return outStream.toString()
}

fun Model.formatAs(format: String): Pair<String, String> {
    val outStream = ByteArrayOutputStream()
    var outFormat = "TTL"
    // Write Ask response to ByteArray stream using ResultSetFormatter and return as String
    when (format) {
        "Text" -> RDFDataMgr.write(outStream, this, Lang.TTL)
        "XML" -> {
            RDFDataMgr.write(outStream, this, Lang.RDFXML)
            outFormat = "XML"
        }
        "SSE" -> {
            SSE.write(outStream, this)
            outFormat = "SSE"
        }
        "CSV" -> {
            RDFDataMgr.write(outStream, this, Lang.CSV)
            outFormat = "CSV"
        }
        "JSON" -> {
            RDFDataMgr.write(outStream, this, Lang.RDFJSON)
            outFormat = "JSON"
        }
        "TSV" -> {
            RDFDataMgr.write(outStream, this, Lang.TSV)
            outFormat = "TSV"
        }
        "TTL" -> RDFDataMgr.write(outStream, this, Lang.TURTLE)
        "NTRIPLES" -> RDFDataMgr.write(outStream, this, Lang.NTRIPLES)
        "DOT" -> RDFDataMgr.write(outStream, this, DOTLang)
        else -> throw Exception("Unsupported language")
    }
    return Pair(outStream.toString(), outFormat)
}

fun parseFormat(acceptHeader: String): MutableList<String> {
    val list: MutableList<String> = mutableListOf()
    val accept: List<String> = acceptHeader.split(',', ignoreCase = true)
    accept.forEach {
        when {
            it.contains(MimeType.text, ignoreCase = true) -> list.add("Text")
            it.contains(MimeType.xml, ignoreCase = true) -> list.add("XML")
            it.contains(MimeType.sse, ignoreCase = true) -> list.add("SSE")
            it.contains(MimeType.csv, ignoreCase = true) -> list.add("CSV")
            it.contains(MimeType.json, ignoreCase = true) -> list.add("JSON")
            it.contains(MimeType.tsv, ignoreCase = true) -> list.add("TSV")
            it.contains(MimeType.ttl, ignoreCase = true) -> list.add("TTL")
            it.contains(MimeType.ntriples, ignoreCase = true) -> list.add("NTRIPLES")
            it.contains(MimeType.dot, ignoreCase = true) -> list.add("DOT")
            else -> println("Formato no reconocido")
        }
    }
    return list
}

object MimeType {
    const val text = "text/plain ;charset=utf-8"
    const val xml = "application/xml ;charset=utf-8"
    const val sse = "text/x-sse ;charset=utf-8"
    const val csv = "text/csv ;charset=utf-8"
    const val json = "application/json ;charset=utf-8"
    const val tsv = "text/tab-separated-values ;charset=utf-8"
    const val ttl = "text/turtle ;charset=utf-8"
    const val ntriples = "application/n-triples ;charset=utf-8"
    const val dot = "text/vnd.graphviz ;charset=utf-8"
}

fun toMimeType(type: String): String {
    return when (type) {
        "Text" -> MimeType.text
        "XML" -> MimeType.xml
        "SSE" -> MimeType.sse
        "CSV" -> MimeType.csv
        "JSON" -> MimeType.json
        "TSV" -> MimeType.tsv
        "TTL" -> MimeType.ttl
        "NTRIPLES" -> MimeType.ntriples
        "DOT" -> MimeType.dot
        else -> ""
    }
}

fun ValidationReport.toPrintableString(): String {
    val outStream = ByteArrayOutputStream()
    ShLib.printReport(outStream, this)

    return outStream.toString()
}

fun String.isAbbreviatedIRI(): Boolean {
    return this.contains(Regex("^[_a-zA-Z]*:[a-zA-Z]+"))
}
