package dot

import org.apache.jena.riot.Lang
import org.apache.jena.riot.LangBuilder

val DOTLang: Lang = LangBuilder
    .create("DOT", "text/dot")
    .addFileExtensions("dot")
    .build()