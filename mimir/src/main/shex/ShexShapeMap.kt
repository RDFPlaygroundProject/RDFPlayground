package shex

import org.apache.commons.rdf.api.RDFTerm
import fr.inria.lille.shexjava.schema.Label

data class ShexShapeMap (
    val nodeLabel: RDFTerm,
    val shapeLabel: Label,
    val originalNode: String,
    val originalShape: String
)
