package api

import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFWriterRegistry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

import dot.DOTLang
import dot.DOTWriter
import org.apache.jena.tdb.TDB
import reasoner.OwlRLWrapper


@SpringBootApplication
open class Application

fun main(args: Array<String>) {
    // Load Our DOT Writer to Jena before app start

    // Register Lang and Format and WriterFactory
    RDFLanguages.register(DOTLang)
    RDFWriterRegistry.register(DOTLang, RDFFormat(DOTLang))
    RDFWriterRegistry.register(RDFFormat(DOTLang), DOTWriter.DOTWriterFactory)

    // Check that OWL_RL Works
    OwlRLWrapper()

    // Start TDB to query it later
    TDB.init()

    // Runs the REST server
    SpringApplication.run(Application::class.java, *args)

    println("Running on http://localhost:9060")
}
