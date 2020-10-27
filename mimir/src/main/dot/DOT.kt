package dot

import org.apache.jena.atlas.io.IndentedWriter
import org.apache.jena.graph.Graph
import org.apache.jena.sparql.serializer.SerializationContext
import org.apache.jena.sparql.sse.writers.WriterGraph
import java.io.OutputStream


object DOT {

    fun write(out: OutputStream, graph: Graph) {
        val indentedOut = IndentedWriter(out)
        write(indentedOut, graph)
        indentedOut.flush()
    }

    fun write(out: IndentedWriter, graph: Graph) {
        WriterGraph.output(
            out,
            graph,
            SerializationContext(graph.prefixMapping)
        )
        out.ensureStartOfLine()
    }
}

