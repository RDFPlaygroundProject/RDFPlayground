import { parseDOTNetwork } from "vis-network";
import vis from "vis";
import VLinedTextarea from "../VLinedTextarea/VLinedTextarea";

const backAPI = "localhost:9060";

const MimeTypes = {
  Text: "text/plain ;charset=utf-8",
  XML: "application/xml ;charset=utf-8",
  SSE: "text/x-sse ;charset=utf-8",
  CSV: "text/csv ;charset=utf-8",
  JSON: "application/json ;charset=utf-8",
  TSV: "text/tab-separated-values ;charset=utf-8",
  TTL: "text/turtle ;charset=utf-8",
  SPARQL: "application/sparql-query ; charset=utf-8",
  NTRIPLES: "application/n-triples ;charset=utf-8",
  DOT: "text/vnd.graphviz ;charset=utf-8",
};

// definitions for DOT graph, according to https://forum.vuejs.org/t/vue-cli-webpack-how-to-install-vis/5729
let options = {
  nodes: {
    font: {
      size: 12,
    },
  },
  edges: {
    length: 120,
    font: {
      size: 12,
    },
  },
};
// eslint-disable-next-line no-unused-vars
let network = null;

export default {
  name: "HelloWorld",
  components: { VLinedTextarea },
  data: () => ({
    vis_graph: null,
    check_syntax_color: "primary",
    check_syntax_icon: "clipboard-pulse",
    check_syntax_text: "Check Syntax",
    check_syntax_loading: false,
    check_syntax_error: false,
    graph_tab: null, // left tab reference
    tools_tab: null, // right tab reference
    graph_text:
      "@base <http://example.org/> .\n" + // graph description initial and current value
      "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
      "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n\n",
    graph_text_disabled: false,
    graph_dot: "digraph G {\n}",
    graph_dot_vis: "",
    graph_error_text: "",
    tdb_tab: null,
    tdb_text: "Click on refresh to see the TDB graph",
    tdb_text_alert: false,
    tdb_text_alert_text: "",
    tdb_fetch_loading: false,
    tdb_dot: "",
    sparql_alert: false,
    sparql_alert_msg: "There was an error",
    sparql_text:
      "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
      "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n" +
      "\n" +
      "SELECT * WHERE {?s ?p ?o} LIMIT 10",
    sparql_operation_type: ["ASK", "CONSTRUCT", "DESCRIBE", "SELECT"],
    sparql_tdb_operation_type: [
      "ASK",
      "CONSTRUCT",
      "DESCRIBE",
      "SELECT",
      "INPUT",
      "DELETE",
      "LOAD",
      "CLEAR",
    ],
    sparql_operation_selected: "",
    sparql_format_selected: "",
    sparql_results_format: [],
    sparql_run_loading: false,
    sparql_tdb_run_loading: false,
    owl_text:
      "@base           <http://ex.org/> .\n" +
      "@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
      "@prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#> .\n" +
      "@prefix owl:    <http://www.w3.org/2002/07/owl#> .\n" +
      "@prefix xsd:    <http://www.w3.org/2001/XMLSchema#> .",
    owl_type: ["RDFS", "OWL"],
    owl_type_selected: "",
    owl_run_loading: false,
    owl_alert: false,
    owl_alert_msg: "There was an error",
    shacl_text_shape:
      "@base           <http://ex.org/> .\n" +
      "@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
      "@prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#> .\n" +
      "@prefix sh:     <http://www.w3.org/ns/shacl#> .\n" +
      "@prefix xsd:    <http://www.w3.org/2001/XMLSchema#> .",
    shacl_run_loading: false,
    shacl_alert: false,
    shacl_alert_msg: "There was an error",
    shex_text_shape:
      "BASE <http://ex.org/>\n" +
      "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
      "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n" +
      "PREFIX sx:      <http://www.w3.org/ns/shex#>\n" +
      "PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>",
    shex_text_shape_map: "",
    shex_run_loading: false,
    shex_alert: false,
    shex_alert_msg: "There was an error",
    result_tab: null, // result tab reference
    result_dot_enabled: false,
    result_text: "",
    result_dot: "",
    result_dot_ref: "visResultDot",
    result_icon: "",
    result_origin: "",
    styleObject: {
      dotContainer: {
        flex: 1,
        width: "100%",
        height: "418px",
        border: "1px solid #d3d3d3",
      },
      dotFullScreenContainer: {
        flex: 1,
        height: "900px",
        border: "1px solid #d3d3d3",
      },
      dotResultsContainer: {
        flex: 1,
        width: "100%",
        height: "700px",
        border: "1px solid #d3d3d3",
      },
      tabItem: {
        flex: 1,
        minHeight: 800,
      },
      basicFlex: {
        flex: 1,
        minHeight: 420,
      },
      resultFromCol: {
        backgroundColor: "secondary",
      },
    },
    prefixcc_dialog: false,
    prefixcc_loading: false,
    prefixcc_format_choices: ["TTL", "SPARQL"],
    prefixcc_format_selected: "TTL",
    prefixcc_term: "",
    prefixcc_term_rules: [
      (value) => !!value || "Required.",
      (value) => (value || "").length <= 20 || "Max 20 characters",
    ],
    prefixcc_result: "",
    prefixcc_result_error: false,
    graph_vis_dialog: false,
    tdb_vis_dialog: false,
    demo_dialog: false,
    about_dialog: false,
    aboutLoading: false,
    aboutGraph:
      "@base   <http://ex.org/> .\n" +
      "@prefix dct:    <http://purl.org/dc/terms/>  .\n" +
      "@prefix doap:   <http://usefulinc.com/ns/doap#> .\n" +
      "@prefix foaf:   <http://xmlns.com/foaf/0.1/> .\n" +
      "@prefix marcrel:        <http://id.loc.gov/vocabulary/relators/> .\n" +
      "@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
      "@prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#> .\n" +
      "@prefix schema: <http://schema.org/> .\n" +
      "@prefix wikidata:       <http://www.wikidata.org/entity/> .\n" +
      "\n" +
      "<rdfpg> a doap:Project ;\n" +
      "        dct:creator <binostroza> ;\n" +
      '        rdfs:label "RDF Playground"@en .\n' +
      "\n" +
      "<binostroza> a foaf:Person ;\n" +
      '        foaf:name "Bastián Inostroza" ;\n' +
      "        marcrel:dgs <http://aidanhogan.com/foaf.rdf#me> ;\n" +
      "        schema:affiliation wikidata:Q232141 .\n" +
      "\n" +
      "<http://aidanhogan.com/foaf.rdf#me> schema:affiliation wikidata:Q232141 .",
    aboutSparql:
      "PREFIX marcrel: <http://id.loc.gov/vocabulary/relators/>\n" +
      "PREFIX schema:  <http://schema.org/>\n" +
      "\n" +
      "SELECT * WHERE {\n" +
      "    ?student marcrel:dgs ?professor .\n" +
      "    ?student schema:affiliation ?afiliation .\n" +
      "    ?professor schema:affiliation ?afiliation .\n" +
      "}\n" +
      "LIMIT 10",
    aboutOwl:
      "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
      "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
      "@prefix owl:  <http://www.w3.org/2002/07/owl#> .\n" +
      "@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .\n" +
      "\n" +
      "marcrel:dgs rdfs:subPropertyOf foaf:knows .",
    aboutShacl:
      "@base   <http://ex.org/> .\n" +
      "@prefix dct:    <http://purl.org/dc/terms/>  .\n" +
      "@prefix doap:   <http://usefulinc.com/ns/doap#> .\n" +
      "@prefix foaf:   <http://xmlns.com/foaf/0.1/> .\n" +
      "@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
      "@prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#> .\n" +
      "@prefix sh:     <http://www.w3.org/ns/shacl#> .\n" +
      "@prefix xsd:    <http://www.w3.org/2001/XMLSchema#> .\n" +
      "\n" +
      "<PersonShape>\n" +
      "        a sh:NodeShape ;\n" +
      "        sh:nodeKind sh:IRI ;\n" +
      "        sh:targetClass foaf:Person ;\n" +
      "        sh:property [\n" +
      "                sh:path foaf:name ;\n" +
      "                sh:count 1 ;\n" +
      "                sh:datatype xsd:string ;\n" +
      "        ]  .\n" +
      "\n" +
      "<ProjectShape>\n" +
      "        a sh:NodeShape ;\n" +
      "        sh:nodeKind sh:IRI ;\n" +
      "        sh:targetClass doap:Project ;\n" +
      "        sh:property [\n" +
      "                sh:path dct:creator ;\n" +
      "                sh:minCount 1 ;\n" +
      "                sh:node <PersonShape> ;\n" +
      "        ] .",
    aboutShexDescription:
      "BASE    <http://ex.org/>\n" +
      "PREFIX dct:     <http://purl.org/dc/terms/>\n" +
      "PREFIX doap:    <http://usefulinc.com/ns/doap#>\n" +
      "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n" +
      "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
      "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n" +
      "PREFIX sx:      <http://www.w3.org/ns/shex#>\n" +
      "PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>\n" +
      "\n" +
      "<Person> {\n" +
      "        foaf:name xsd:string ;\n" +
      "        a [foaf:Person]\n" +
      "}\n" +
      "\n" +
      "<Project> {\n" +
      "        dct:creator @<Person> + ;\n" +
      "        a [doap:project]\n" +
      "}",
    aboutShexPairing: "<http://ex.org/rdfpg>@<http://ex.org/Project>",
  }),

  methods: {
    aboutAccepted: function() {
      this.aboutLoading = true;
      // Fill all user writable fields with data
      this.graph_text = this.aboutGraph;
      this.sparql_text = this.aboutSparql;
      this.sparql_operation_selected = "SELECT";
      this.sparqlSwitchFormat(this.sparql_operation_selected);
      this.graph_text_disabled = false;
      this.owl_text = this.aboutOwl;
      this.owl_type_selected = "OWL";
      this.shacl_text_shape = this.aboutShacl;
      this.shex_text_shape = this.aboutShexDescription;
      this.shex_text_shape_map = this.aboutShexPairing;
      // terminate animation and close
      this.aboutLoading = false;
      this.about_dialog = false;
    },
    prefixLookUp: function() {
      this.prefixcc_loading = true;
      let format = this.prefixcc_format_selected.toLowerCase();
      fetch(`http://prefix.cc/` + this.prefixcc_term + `.file.` + format, {
        method: "GET",
      })
        .then((response) => {
          if (!response.ok) {
            this.prefixcc_result = "Prefix Not Found. Add it on prefix.cc! :D";
            this.prefixcc_result_error = true;
            this.prefixcc_loading = false;
          } else {
            // Successful request :D
            response.text().then((response) => {
              this.prefixcc_result = response;
              this.prefixcc_result_error = false;
              this.prefixcc_loading = false;
            });
          }
        })
        .catch((error) => {
          this.prefixcc_result = error;
          this.prefixcc_result_error = true;
          this.prefixcc_loading = false;
        });
    },
    checkSyntaxColorReset: function() {
      // resets the check syntax button colour to blue
      this.check_syntax_color = "primary";
    },
    checkSyntax: function() {
      // sends the text to the backend to have it's syntax verified, also gets a DOT version to display

      // Starts Loading animation
      this.check_syntax_loading = true;
      // Check the graph syntax, and if it's correct displays it on graph tab
      let requestBody = { data: this.graph_text.toString() };
      // request syntax check to backend
      fetch(`http://` + backAPI + `/api/model/syntax_check`, {
        method: "POST",
        headers: new Headers({
          "Content-Type": "application/json",
        }),
        body: JSON.stringify(requestBody),
      })
        .then((response) => {
          // display an error on the page console
          if (!response.ok) {
            response.json().then((responseBody) => {
              this.graph_error_text =
                "response not 200, was " +
                response.status +
                " " +
                responseBody.syntax_error;
              this.check_syntax_loading = false;
              this.check_syntax_error = true;
            });
          } else {
            response.json().then((content) => {
              // if there is an error graph_dot will be empty
              this.graph_dot = content.data_dot;
              this.graph_error_text = content.syntax_error;
              if (content.data_dot === "" && content.syntax_error !== "") {
                // the is a syntax error, so we convert the button colour to red and display the error
                this.check_syntax_error = true;
                this.check_syntax_icon = "close";
                this.check_syntax_text = "Check Again";
                this.check_syntax_color = "red darken-4";
              } else {
                // syntax is correct, so we colour the button green and
                this.check_syntax_error = false;
                this.check_syntax_icon = "check";
                this.check_syntax_text = "Correct Syntax";
                this.check_syntax_color = "green darken-3";
                // load the content of graph.dot on visjs div
                try {
                  let parsedData = parseDOTNetwork(content.data_dot);
                  const refContainer = this.$refs.visNetworkGraph;
                  network = new vis.Network(refContainer, parsedData, options);
                  this.graph_dot_vis = parseDOTNetwork(this.graph_dot);
                } catch (e) {
                  this.check_syntax_loading = false;
                }
              }

              this.check_syntax_loading = false;
            });
          }
        })
        .catch((error) => {
          this.graph_error_text =
            "There was a problem with the request: " + error;
          this.check_syntax_loading = false;
          this.check_syntax_error = true;
        });
    },
    dotFetchResult: function(text, format) {
      // gets a dot version of the selected text (we need to specify the format)
      let dot = "";
      let reqBody = {
        data: text,
        data_lang: format,
      };
      fetch(`http://` + backAPI + `/api/model/dot`, {
        method: "POST",
        headers: new Headers({
          "Content-Type": "application/json",
          Accept: MimeTypes.Text + ", " + MimeTypes.DOT,
        }),
        body: JSON.stringify(reqBody),
      })
        .then((response) => {
          if (response.ok) {
            response.text().then((body) => {
              dot = body;
              this.result_dot = parseDOTNetwork(dot);
              return dot;
            });
          }
        })
        .catch((reason) => console.warn("Error getting DOT: " + reason));

      return dot;
    },
    getTdbModel: function() {
      // get the TDB entire model to be displayed
      this.tdb_fetch_loading = true;
      // get model and display it
      fetch(`http://` + backAPI + `/api/tdb/get_model`, {
        method: "GET",
      }).then((response) => {
        if (!response.ok) {
          response.text().then((body) => {
            let msg = body.toString();
            this.tdbShowAlert(
              "Error getting TDB graph: " + response.status + " " + msg
            );
          });
        } else {
          // process and display
          response.json().then((responseBody) => {
            // response: { data: "model on TTL", dot: "model on DOT"}
            let model = responseBody.data;
            let dot = responseBody.dot;

            // display data on the TDB tab and stop the loading animation
            this.tdb_text = model;
            this.tdb_dot = parseDOTNetwork(dot);
            this.tdb_text_alert = false;
            this.tdb_fetch_loading = false;
          });
        }
      });
    },
    tdbShowAlert: function(message) {
      // displays an alert on top of the refresh tdb graph button
      this.tdb_text_alert = true;
      this.tdb_text_alert_text = message;
    },
    sparqlModelQuery: function() {
      // hides error message if not deleted yet
      this.sparql_alert = false;
      // sends the query and graph to the backend, and gets the result
      this.sparql_run_loading = true;
      let requestBody = {
        data: this.graph_text.toString(),
        data_lang: "TTL", // default, could be changed on the future
        query: this.sparql_text.toString(),
        query_response_lang: this.sparql_format_selected,
      };
      fetch(`http://` + backAPI + `/api/tdb/query_model`, {
        method: "POST",
        headers: new Headers({
          "Content-Type": "application/json",
          Accept:
            MimeTypes.Text + ", " + MimeTypes[this.sparql_format_selected],
        }),
        body: JSON.stringify(requestBody),
      })
        .then((response) => {
          if (!response.ok) {
            response.text().then((body) => {
              let text = body.toString();
              this.sparqlShowAlert(
                "Response was not OK, was: " + response.status + " " + text
              );
            });
            this.sparql_run_loading = false;
          } else {
            response.text().then((bodyText) => {
              this.result_text = bodyText;
              this.result_icon = "mdi-database-search";
              this.result_origin = "SPARQL";
              this.sparql_run_loading = false;

              // Try to display the DOT version if applicable
              if (
                this.sparql_operation_selected === "CONSTRUCT" ||
                this.sparql_operation_selected === "DESCRIBE"
              ) {
                if (this.sparql_format_selected === "DOT") {
                  // Result Text is DOT and we can use that
                  this.result_dot = parseDOTNetwork(this.result_text);
                  this.sparql_run_loading = false;
                } else {
                  // Fetch DOT version from backend
                  this.dotFetchResult(
                    this.result_text,
                    this.sparql_format_selected
                  );
                }
              }
            });
          }
        })
        .catch((reason) => {
          this.sparqlShowAlert(reason);
          this.sparql_run_loading = false;
        });
    },
    sparqlTdb: function() {
      // hides error message if not deleted yet
      this.sparql_alert = false;
      // makes and sparql query directly to the backend TDB
      this.sparql_tdb_run_loading = true;
      let operation = this.sparql_operation_selected;
      let selected_operation_type = ""; // blank will be rejected by the backend
      if (["ASK", "SELECT", "CONSTRUCT", "DESCRIBE"].includes(operation)) {
        // is a query
        selected_operation_type = "query";
      } else if (["INPUT", "DELETE", "LOAD", "CLEAR"].includes(operation)) {
        // is an update
        selected_operation_type = "update";
      }
      let requestBody = {
        query: this.sparql_text.toString(),
        type: selected_operation_type.toString(),
      };
      fetch(`http://` + backAPI + `/api/tdb/query_tdb`, {
        method: "POST",
        headers: new Headers({
          "Content-Type": "application/json",
          Accept:
            MimeTypes.Text + ", " + MimeTypes[this.sparql_format_selected],
        }),
        body: JSON.stringify(requestBody),
      })
        .then((response) => {
          if (!response.ok) {
            // extract error message and display it
            response.text().then((body) => {
              let text = body.toString();
              this.sparqlShowAlert(
                "Response was not OK, was: " + response.status + " " + text
              );
            });
            this.sparql_tdb_run_loading = false;
          } else {
            // process and show response
            response.text().then((bodyText) => {
              // set the image and text of the result section
              this.result_text = bodyText;
              this.result_icon = "mdi-database-search";
              this.result_origin = "SPARQL TDB";
              this.sparql_tdb_run_loading = false;

              // if the response is DOT text, display it on the result section
              if (
                ["CONSTRUCT", "DESCRIBE"].includes(
                  this.sparql_operation_selected
                )
              ) {
                // these operations support DOT
                if (this.sparql_format_selected === "DOT") {
                  // Result Text is DOT and we can parse it to be displayed
                  this.result_dot = parseDOTNetwork(this.result_text);
                  this.sparql_tdb_run_loading = false;
                } else {
                  // Fetch DOT version from backend
                  this.dotFetchResult(
                    this.result_text,
                    this.sparql_format_selected
                  );
                }
              }
            });
          }
        })
        .catch((error) => {
          this.sparqlShowAlert(error);
          this.sparql_tdb_run_loading = false;
        });
    },
    sparqlClearSelectedFormatAndGetTdb: function() {
      // Clears the current result, making the user choose again according to the new options
      this.sparqlSwitchFormat(this.sparql_operation_selected);
      // Additionally try to load TDB graph
      this.getTdbModel();
    },
    sparqlSwitchFormat: function(type) {
      // Changes the selector options according to the user selection
      this.sparql_format_selected = "";
      if (type === "ASK") {
        this.sparql_results_format = ["Text", "XML"];
      } else if (type === "SELECT") {
        this.sparql_results_format = ["Text", "XML", "CSV", "TSV"];
      } else if (type === "CONSTRUCT" || type === "DESCRIBE") {
        this.sparql_results_format = ["SSE", "JSON", "TTL", "NTRIPLES", "DOT"];
      } else if (type === "INPUT" || type === "DELETE") {
        this.sparql_results_format = ["Text"];
      } else if (type === "LOAD" || type === "CLEAR") {
        this.sparql_results_format = ["Text"];
      }
    },
    sparqlFieldReset: function() {
      // Changes text to the original selection
      this.sparql_text =
        "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
        "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n" +
        "\n" +
        "SELECT * WHERE {?s ?p ?o} LIMIT 10";
    },
    sparqlShowAlert: function(message) {
      // Shows an alert on the buttons row
      this.sparql_alert_msg = message;
      this.sparql_alert = true;
    },
    owlReason: function() {
      // disables alert if not done by the user yet
      this.owl_alert = false;
      // sends the model to the backend to be reasoned over
      this.owl_run_loading = true;
      let profile = this.owl_type_selected;
      let data = this.graph_text.toString();
      // Add specifics
      if (this.owl_text !== "") {
        data += "\n" + this.owl_text.toString();
      }
      let reqBody = {
        data: data,
        data_lang: "TTL",
        profile: profile,
      };

      fetch(`http://` + backAPI + `/api/owl/reason`, {
        method: "POST",
        headers: new Headers({
          "Content-Type": "application/json",
          Accept: MimeTypes.Text + ", " + MimeTypes.JSON,
        }),
        body: JSON.stringify(reqBody),
      })
        .then((response) => {
          if (!response.ok) {
            this.owlShowAlert("Response was not OK, was: " + response.status);
            this.owl_run_loading = false;
          } else {
            response.json().then((responseBody) => {
              let inferred = responseBody.data;
              let errorMsg = responseBody.error;
              let inferredDot = responseBody.data_dot;
              if (errorMsg !== "") {
                this.owlShowAlert(errorMsg);
                this.owl_run_loading = false;
              } else {
                this.result_text = inferred;
                this.result_origin = profile;
                this.result_icon = "mdi-owl";
                this.result_dot = parseDOTNetwork(inferredDot);
                this.owl_run_loading = false;
              }
            });
          }
        })
        .catch((reason) => {
          this.owlShowAlert(reason);
          this.owl_run_loading = false;
        });
    },
    owlShowAlert: function(message) {
      // display an alert on the owl buttons row
      this.owl_alert_msg = message;
      this.owl_alert = true;
    },
    shaclValidate: function() {
      // disables alert if not done by the user yet
      this.shacl_alert = false;
      // validates the graph with the specified shacl rules on the backend
      this.shacl_run_loading = true;
      let reqBody = {
        data: this.graph_text.toString(),
        data_lang: "TTL",
        shape: this.shacl_text_shape.toString(),
        shape_lang: "TTL",
      };

      fetch(`http://` + backAPI + `/api/shape/shacl_isvalid`, {
        method: "POST",
        headers: new Headers({
          "Content-Type": MimeTypes.JSON,
          Accept: MimeTypes.Text + ", " + MimeTypes.JSON,
        }),
        body: JSON.stringify(reqBody),
      })
        .then((response) => {
          if (!response.ok || response.status === 204) {
            if (response.status === 204) {
              this.shaclShowAlert("There is no content to be processed");
              this.shacl_run_loading = false;
            } else {
              response.text().then((text) => {
                this.shaclShowAlert("Problem: " + text.toString());
                this.shacl_run_loading = false;
              });
            }
          } else {
            response.json().then((responseBody) => {
              let report = responseBody.report;
              let fusion = responseBody.fusion_dot;

              this.result_origin = "SHACL";
              this.result_icon = "mdi-eye-check-outline";
              this.result_dot = parseDOTNetwork(fusion);
              if (report === "") {
                this.result_text =
                  "The data graph conforms with the shape graph!";
              } else {
                this.result_text = report;
              }
              this.shacl_run_loading = false;
            });
          }
        })
        .catch((reason) => {
          this.shaclShowAlert(reason);
          this.shacl_run_loading = false;
        });
    },
    shaclShowAlert: function(message) {
      // displays an error en the shacl button row bar
      this.shacl_alert_msg = message;
      this.shacl_alert = true;
    },
    shexValidate: function() {
      // disables alert if not done by the user yet
      this.shex_alert = false;
      // validates the graph with the specified shex rules on the backend
      this.shex_run_loading = true;
      let requestBody = {
        data: this.graph_text.toString(),
        data_lang: "TTL",
        shape: this.shex_text_shape,
        shape_map: this.shex_text_shape_map,
      };

      fetch(`http://` + backAPI + `/api/shape/shex_isvalid`, {
        method: "POST",
        headers: new Headers({
          "Content-Type": "application/json",
          Accept: MimeTypes.Text,
        }),
        body: JSON.stringify(requestBody),
      })
        .then((response) => {
          if (!response.ok) {
            response.text().then((body) => {
              let text = body.toString();
              this.shexShowAlert(
                "Response was not OK, was: " + response.status + " " + text
              );
            });
            this.shex_run_loading = false;
          } else {
            response.text().then((bodyText) => {
              this.result_text = bodyText;
              this.result_icon = "mdi-code-tags-check";
              this.result_origin = "ShEx";
              this.shex_run_loading = false;
            });
          }
        })
        .catch((reason) => {
          this.shexShowAlert(reason);
          this.shex_run_loading = false;
        });
    },
    shexShowAlert: function(message) {
      // shows an error message on the shex row bar
      this.shex_alert_msg = message;
      this.shex_alert = true;
    },
    resultToClipboard: async function() {
      // copies result text to the clipboard of the client
      await navigator.clipboard.writeText(this.result_text);
    },
  },

  computed: {
    sparqlBtnDisabled() {
      return this.sparql_format_selected === "" || this.sparql_run_loading;
    },
    sparqlTdbBtnDisabled() {
      return this.sparql_format_selected === "" || this.sparql_tdb_run_loading;
    },
    owlBtnDisabled() {
      return this.owl_type_selected === "" || this.owl_run_loading;
    },
    shaclBtnDisabled() {
      return this.shacl_text_shape === "" || this.shacl_run_loading;
    },
    shexBtnDisabled() {
      return (
        this.shex_text_shape === "" ||
        this.shex_run_loading ||
        this.shex_text_shape_map === ""
      );
    },
    copyResultEnabled() {
      return this.result_text !== "";
    },
    resultsAvailable() {
      return this.result_text !== "";
    },
    resultsDotAvailable() {
      return this.result_dot !== "";
    },
    prefixccLookupDisabled() {
      return this.prefixcc_term.length < 1 || this.prefixcc_term.length > 20;
    },
  },
};
