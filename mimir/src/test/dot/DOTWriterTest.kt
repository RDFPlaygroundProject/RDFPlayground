package dot

import dot.colorTemplate.NodeColor
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.*

internal class DOTWriterTest {
    private val format: RDFFormat = RDFFormat(DOTLang)
    private lateinit var writer: WriterGraphRIOT
    private lateinit var model: Model
    private val sampleTTL: String = """
@base <http://example.org/ns/> .
# In-scope base URI is http://example.org/ns/ at this point
<a2> <http://example.org/ns/b2> <c2> .
@base <foo/> .
# In-scope base URI is http://example.org/ns/foo/ at this point
<a3> <b3> <c3> .
@prefix b: <bar#> .
b:a4 b:b4 b:c4 .
@prefix ns2: <http://example.org/ns2#> .
ns2:a5 ns2:b5 ns2:c5 .
    """.trimIndent()
    private val sampleDOT: String = """digraph G{
  charset="utf-8";
  
  // Edges
  "http://example.org/ns/a2" -> "http://example.org/ns/c2" [label="http://example.org/ns/b2"]
  "http://example.org/ns2#a5" -> "http://example.org/ns2#c5" [label="ns2:b5"]
  "http://example.org/ns/foo/bar#a4" -> "http://example.org/ns/foo/bar#c4" [label="b:b4"]
  "http://example.org/ns/foo/a3" -> "http://example.org/ns/foo/c3" [label="http://example.org/ns/foo/b3"]
  
  // Nodes
  "http://example.org/ns/a2" [label="http://example.org/ns/a2",shape=ellipse,color=${NodeColor.uri}]
  "http://example.org/ns2#a5" [label="ns2:a5",shape=ellipse,color=${NodeColor.uri}]
  "http://example.org/ns/foo/bar#a4" [label="b:a4",shape=ellipse,color=${NodeColor.uri}]
  "http://example.org/ns/foo/a3" [label="http://example.org/ns/foo/a3",shape=ellipse,color=${NodeColor.uri}]
  "http://example.org/ns/c2" [label="http://example.org/ns/c2",shape=ellipse,color=${NodeColor.uri}]
  "http://example.org/ns2#c5" [label="ns2:c5",shape=ellipse,color=${NodeColor.uri}]
  "http://example.org/ns/foo/bar#c4" [label="b:c4",shape=ellipse,color=${NodeColor.uri}]
  "http://example.org/ns/foo/c3" [label="http://example.org/ns/foo/c3",shape=ellipse,color=${NodeColor.uri}]
}

""".trimIndent()
    private val sampleWithLiteralsTTL: String = """
@base <http://example.org/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix rel: <http://www.perceive.net/schemas/relationship/> .

<#green-goblin>
    rel:enemyOf <#spiderman> ;
    a foaf:Person ;    # in the context of the Marvel universe
    foaf:name "Green Goblin" .

<#spiderman>
    rel:enemyOf <#green-goblin> ;
    a foaf:Person ;
    foaf:name "Spiderman", "Hombre Araña"@es .
    """.trimIndent()
    private val sampleWithLiteralsDOT: String = """
digraph G{
  charset="utf-8";
  
  // Edges
  "http://example.org/#spiderman" -> "Hombre Araña" [label="foaf:name"]
  "http://example.org/#spiderman" -> "Spiderman" [label="foaf:name"]
  "http://example.org/#spiderman" -> "http://xmlns.com/foaf/0.1/Person" [label="rdf:type"]
  "http://example.org/#spiderman" -> "http://example.org/#green-goblin" [label="rel:enemyOf"]
  "http://example.org/#green-goblin" -> "Green Goblin" [label="foaf:name"]
  "http://example.org/#green-goblin" -> "http://xmlns.com/foaf/0.1/Person" [label="rdf:type"]
  "http://example.org/#green-goblin" -> "http://example.org/#spiderman" [label="rel:enemyOf"]
  
  // Nodes
  "http://example.org/#spiderman" [label="http://example.org/#spiderman",shape=ellipse,color=${NodeColor.uri}]
  "http://example.org/#green-goblin" [label="http://example.org/#green-goblin",shape=ellipse,color=${NodeColor.uri}]
  "Hombre Araña" [label="Hombre Araña",shape=record,color=${NodeColor.literal}]
  "Spiderman" [label="Spiderman",shape=record,color=${NodeColor.literal}]
  "http://xmlns.com/foaf/0.1/Person" [label="foaf:Person",shape=ellipse,color=${NodeColor.uri}]
  "Green Goblin" [label="Green Goblin",shape=record,color=${NodeColor.literal}]
}

    """.trimIndent()
    private lateinit var modelWithLiterals: Model

    @BeforeEach
    fun setUp() {
        // Create a model based on sampleTTL
        model = ModelFactory.createDefaultModel()
        val stream: InputStream = ByteArrayInputStream(sampleTTL.toByteArray(Charsets.UTF_8))
        model.read(stream, null, "TTL")

        // create another model for a graph with literals
        modelWithLiterals = ModelFactory.createDefaultModel()
        val otherStream: InputStream = ByteArrayInputStream(sampleWithLiteralsTTL.toByteArray(Charsets.UTF_8))
        modelWithLiterals.read(otherStream, null, "TTL")

        writer = DOTWriter.create(syntaxRDFFormat = null)
    }

    @Test
    fun getLang() {
        // Register Lang
        RDFLanguages.register(DOTLang)
        // Test that writer got the lang correctly from RDFLanguages, and can be used by DOTWriter
        val lang: Lang = writer.lang
        assertTrue(RDFLanguages.isRegistered(lang))
    }

    @Test
    fun formatRegistry() {
        // Register Lang and Format
        RDFLanguages.register(DOTLang)
        RDFWriterRegistry.register(DOTLang, format)
        // Register WriterFactory
        RDFWriterRegistry.register(format, DOTWriter.DOTWriterFactory)
        // Checks if format is correctly added
        assertTrue(RDFWriterRegistry.registered().contains(format))
    }

    @Test
    fun writeWithFormat() {
        // Register Lang and Format
        RDFLanguages.register(DOTLang)
        RDFWriterRegistry.register(DOTLang, format)
        // Register WriterFactory
        RDFWriterRegistry.register(format, DOTWriter.DOTWriterFactory)

        val dotBAOS = ByteArrayOutputStream()
        // Try and use the Register Writer to write
        RDFDataMgr.write(dotBAOS, model, format)

        assertEquals(sampleDOT, dotBAOS.toString())
    }

    @Test
    fun graphWithLiterals() {
        // Register Lang and Format
        RDFLanguages.register(DOTLang)
        RDFWriterRegistry.register(DOTLang, format)
        // Register WriterFactory
        RDFWriterRegistry.register(format, DOTWriter.DOTWriterFactory)

        // convert a sample with literals to DOT
        val dotBAOS = ByteArrayOutputStream()
        RDFDataMgr.write(dotBAOS, modelWithLiterals, format)

        assertEquals(sampleWithLiteralsDOT, dotBAOS.toString())
    }

    @Test
    fun graphWithBlankNodes() {
        val turtleWithBNode = """
@base <http://example.org/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix rel: <http://www.perceive.net/schemas/relationship/> .

<#green-goblin>
    rel:enemyOf <#spiderman> ;
    a foaf:Person ;    # in the context of the Marvel universe
    foaf:name "Green Goblin" .

<#spiderman>
    rel:enemyOf <#green-goblin> ;
    a foaf:Person ;
    foaf:name "Spiderman", "Hombre Araña"@es ;
    foaf:knows (_:MaryJane [ foaf:name "Aunt May"] [ foaf:name "Uncle Ben"] ) .

_:MaryJane foaf:name "Mary Jane" .
        """.trimIndent()

        // Create a model
        val aModel = ModelFactory.createDefaultModel()
        val aStream: InputStream = ByteArrayInputStream(turtleWithBNode.toByteArray(Charsets.UTF_8))
        aModel.read(aStream, null, "TTL")

        // Register Lang and Format
        RDFLanguages.register(DOTLang)
        RDFWriterRegistry.register(DOTLang, format)
        // Register WriterFactory
        RDFWriterRegistry.register(format, DOTWriter.DOTWriterFactory)

        // convert a sample with B Nodes to DOT
        val dotBAOS = ByteArrayOutputStream()
        RDFDataMgr.write(dotBAOS, aModel, format)

        // We cannot use some fixed string due to UID on Node_Blank.blankNodeLabel
        assertTrue(dotBAOS.toString().contains("\"Mary Jane\" [label=\"Mary Jane\",shape=record,color=${NodeColor.literal}]"))
    }
}