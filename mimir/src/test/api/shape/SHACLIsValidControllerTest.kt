package api.shape

import org.apache.jena.graph.Graph
import org.apache.jena.rdf.model.Model
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import org.apache.jena.shacl.ValidationReport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import loadModel
import toPrintableString

internal class SHACLIsValidControllerTest {

    @Test
    fun shaclIsValid() {
        assertTrue(true)
    }

    @Test
    fun jenaSHACLDoNotConforms() {
        // Generates a valid report without exceptions
        val data = """
@prefix : <http://example.org/#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix schema: <http://schema.org/>.

:dave  a :User ;                        #Fails as a :UserShape     
       schema:name       "Dave";
       schema:gender     :Unknown ;
       schema:birthDate  1980 ;
       schema:knows      :grace .

:emily a :User ;                        #Fails as a :UserShape          
       schema:name       "Emily", "Emilee";
       schema:gender     schema:Female .

:frank a :User ;                        #Fails as a :UserShape     
       foaf:name         "Frank" ;
       schema:gender     schema:Male .

_:x    a :User;                         #Fails as a :UserShape          
       schema:name       "Unknown" ;
       schema:gender     schema:Male ;
       schema:knows      _:x .
        """.trimIndent()
        val shape = """
@prefix : <http://example.org/#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix schema: <http://schema.org/>.
@prefix sh: <http://www.w3.org/ns/shacl#>.
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.


:UserShape a sh:NodeShape;
   sh:targetClass :User ;
   sh:property [                  # Blank node 1
    sh:path     schema:name ;
    sh:minCount 1;
    sh:maxCount 1;
    sh:datatype xsd:string ;
  ] ;
  sh:property [                   # Blank node 2
   sh:path schema:gender ;
   sh:minCount 1;
   sh:maxCount 1;
   sh:or (
    [ sh:in (schema:Male schema:Female) ]
    [ sh:datatype xsd:string]
   )
  ] ;
  sh:property [                   # Blank node 3  
   sh:path     schema:birthDate ;
   sh:maxCount 1;
   sh:datatype xsd:date ;
  ] ;
  sh:property [                   # Blank node 4 
   sh:path     schema:knows ;
   sh:nodeKind sh:IRI ;
   sh:class    :User ;
  ] .
        """.trimIndent()

        val dataModel: Model = loadModel(data, "TTL")
        val shapeModel: Model = loadModel(shape, "TTL")

        val shapes: Shapes = Shapes.parse(shapeModel)
        val dataGraph: Graph = dataModel.graph
        // get validation report
        val validationReport: ValidationReport = ShaclValidator.get().validate(shapes, dataGraph)

        assertNotEquals("Conforms\r\n", validationReport.toPrintableString())
    }

    @Test
    fun jenaSHACLConforms() {
        val data: String = """
@prefix : <http://example.org/#> .
:alice a :User .                     #Passes as a :UserShape 

<http://other.uri.com/bob> a :User . #Passes as a :UserShape  
        """.trimIndent()
        val shape: String = """
@prefix : <http://example.org/#> .
@prefix schema: <http://schema.org/>.
@prefix sh: <http://www.w3.org/ns/shacl#>.
:UserShape a              sh:NodeShape;
           sh:targetClass :User ;
           sh:nodeKind    sh:IRI .
        """.trimIndent()

        val dataModel: Model = loadModel(data, "TTL")
        val shapeModel: Model = loadModel(shape, "TTL")

        val shapes: Shapes = Shapes.parse(shapeModel)
        val dataGraph: Graph = dataModel.graph
        // get validation report
        val validationReport: ValidationReport = ShaclValidator.get().validate(shapes, dataGraph)
        assertEquals("Conforms\r\n", validationReport.toPrintableString())
    }
}