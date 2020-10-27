package api

object ReasonerConfig {
    var TMP_DIR_NAME = "src/main/reasoner/temp/"
    var TMP_NAME_PREFIX = "tmp"
    var TMP_NAME_SUFFIX = ".ttl"

    var DEFAULT_DATA_NAME = "test.ttl"
    var DEFAULT_DATA_DIR = "src/main/reasoner/"
    var ENV_ACTIVATION_SCRIPT = "./src/main/reasoner/owlrl/venv/bin/activate"
    var CMD_NAME = "python src/main/reasoner/owlrl/scripts/owlrl"
    var DEFAULT_PROFILE = "rdfs"
    var DEFAULT_FLAG = " -r"

    var RDFS_FLAG = " -r"
    var RDFS_PROFILE = "rdfs"
    var OWL_PROFILE = "owl"
    var OWL_FLAG = " -w"
}
