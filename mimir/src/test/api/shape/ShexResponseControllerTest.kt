package api.shape

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ShexResponseControllerTest {

    @Test
    fun shexIsValid() {
        val data: String = """
@prefix :       <http://example.org/> .
@prefix schema: <http://schema.org/> .
@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .
@prefix foaf:  <http://www.w3.org/2001/FOAF#> .


:dave  schema:name           "Dave";         #  Fails as a :User
       schema:gender         "XYY";          #
       schema:birthDate      1980 .          #  1980 is not an xsd:date *)

:emily schema:name           "Emilee" ;       #  Passes
       schema:gender         schema:Female . #  

:frank foaf:name             "Frank" ;       #  Fails as a :User
       schema:gender:        schema:Male .   #  missing schema:name *)

:grace schema:name           "Grace" ;       #  Fails as a :User
       schema:gender         schema:Male ;   # 
       schema:knows          _:x .           #  \_:x is not an IRI *)

:harold schema:name         "Harold" ;    #  Fails as a :User
        schema:gender       schema:Male ;
        schema:knows        :grace .      #  :grace does not conform to :User *)
        """.trimIndent()
        val lang = "TTL"
        val shex: String = """
PREFIX :       <http://example.org/>
PREFIX schema: <http://schema.org/>
PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>

:User {
  schema:name          xsd:string  ;
  schema:birthDate     xsd:date?  ;
  schema:gender        [ schema:Male schema:Female ] OR xsd:string ;
  schema:knows         IRI @:User*
}
        """.trimIndent()
        val shapeMap: String = """
<http://example.org/harold>@<http://example.org/User>,
<http://example.org/grace>@<http://example.org/User>,
<http://example.org/frank>@<http://example.org/User>,
:emily@:User,
<http://example.org/emily>@:User,
:emily@<http://example.org/User>,
:dave@:User
        """.trimIndent()
        val expectedReport = """
ShEx Validation Report:
  ❌ <http://example.org/harold> does not conform with <http://example.org/User>
  ❌ <http://example.org/grace> does not conform with <http://example.org/User>
  ❌ <http://example.org/frank> does not conform with <http://example.org/User>
  ✅ :emily conforms with :User
  ✅ <http://example.org/emily> conforms with :User
  ✅ :emily conforms with <http://example.org/User>
  ❌ :dave does not conform with :User
        """.trimIndent()

        val testedClass = ShexIsValidController()

        val response = testedClass
                .shexIsValid(
                    ShExRequest(data, lang, shex, shapeMap)
                )

        assertEquals(expectedReport, response.body!!.trim())
    }
}