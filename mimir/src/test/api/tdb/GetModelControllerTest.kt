package api.tdb

import dot.DOTLang
import dot.DOTWriter

import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFWriterRegistry
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

internal class GetModelControllerTest {

    @BeforeEach
    fun setUp() {
        // Register Lang and Format and WriterFactory
        RDFLanguages.register(DOTLang)
        RDFWriterRegistry.register(DOTLang, RDFFormat(DOTLang))
        RDFWriterRegistry.register(RDFFormat(DOTLang), DOTWriter.DOTWriterFactory)
    }

    @Test
    fun getModel() {
        val response = GetModelController().getModel()
        when (response.body) {
            is String -> assertEquals("", response.body) // This is an error, and this should be working properly
            is GetModelResponse -> {
                assertNotEquals("", (response.body as GetModelResponse).data)
                assertNotEquals(
                    """diagraph G{
                        charset="utf-8";
                        
                        // Edges
                        
                        // Nodes
                    }
                    """.trimIndent().trimMargin(),
                    (response.body as GetModelResponse).dot.trimIndent().trimMargin())
            }
        }
    }
}