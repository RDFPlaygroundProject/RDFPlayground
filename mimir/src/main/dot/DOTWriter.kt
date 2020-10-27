package dot

import org.apache.jena.atlas.io.IndentedWriter
import org.apache.jena.graph.Graph
import org.apache.jena.riot.*
import org.apache.jena.riot.adapters.RDFWriterRIOT
import org.apache.jena.riot.system.PrefixMap
import org.apache.jena.riot.system.RiotLib
import org.apache.jena.riot.writer.WriterGraphRIOTBase
import org.apache.jena.sparql.util.Context
import java.io.OutputStream
import java.io.Writer


class DOTWriter: WriterGraphRIOTBase() {
    override fun getLang(): Lang {
        return RDFLanguages.contentTypeToLang("text/dot")
    }

    override fun write(out: Writer, graph: Graph, prefixMap: PrefixMap, baseURI: String, context: Context) {
        // MAYBE check charset tp be UTF-8
        val indentedWriter: IndentedWriter = RiotLib.create(out)
        output(indentedWriter, graph, prefixMap, baseURI, context)
    }

    override fun write(out: OutputStream, graph: Graph, prefixMap: PrefixMap, baseURI: String?, context: Context) {
        val indentedWriter = IndentedWriter(out)
        output(indentedWriter, graph, prefixMap, baseURI, context)
    }

    companion object DOTWriterFactory: WriterGraphRIOTFactory {
        override fun create(syntaxRDFFormat: RDFFormat?): WriterGraphRIOT {
            return DOTWriter()
        }
    }

    private fun output(
        indentedOutput: IndentedWriter,
        graph: Graph,
        prefixMap: PrefixMap,
        baseURI: String?,
        context: Context
    ) {
        val privateWriter = PrivateTurtleWriter(indentedOutput, prefixMap, baseURI, context)
        privateWriter.write(graph)
        indentedOutput.flush()
    }

    private class PrivateTurtleWriter(out: IndentedWriter, prefixMap: PrefixMap, baseURI: String?, context: Context) :
        DOTShell(out, prefixMap, baseURI, context) {

        internal fun write(graph: Graph) {
            writeGraphDOT(graph)
        }
    }

    // Model.write adapter - must be public.
    object RDFWriterDOT : RDFWriterRIOT("DOT")
}
