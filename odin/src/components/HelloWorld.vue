<template>
  <v-container fluid>
    <v-row class="text-center justify-center">
      <!-- Left side -->
      <v-col v-if="!graph_text_disabled" cols="12" md="6">
        <v-alert
          class="mt-0 mb-2"
          v-model="check_syntax_error"
          type="error"
          close-text="Close Alert"
          dense
          dismissible
        >
          {{this.graph_error_text}}
        </v-alert>
        <v-btn
          v-if="!check_syntax_error"
          :color="check_syntax_color"
          class="mr-2 mb-3 left white--text"
          :loading="check_syntax_loading"
          :disabled="check_syntax_loading"
          @click.prevent="checkSyntax"
        >
          <v-icon left>mdi-{{this.check_syntax_icon}}-outline</v-icon> {{this.check_syntax_text}}
        </v-btn>

        <!-- Tab titles -->
        <v-tabs
            v-model="graph_tab"
            background-color="primary accent-4"
            right
            dark
            icons-and-text>
          <v-tabs-slider></v-tabs-slider>

          <v-tab disabled>
            See the graph as:
          </v-tab>

          <v-tab href="#tab-graph-text">
            Text
            <v-icon>mdi-text-subject</v-icon>
          </v-tab>

          <v-tab href="#tab-graph-graph" @click="checkSyntax">
            Graph
            <v-icon>mdi-share-variant</v-icon>
          </v-tab>
        </v-tabs>

        <!-- Tab Contents -->
        <v-tabs-items v-model="graph_tab" v-bind:style="styleObject.tabItem">
          <v-tab-item value="tab-graph-text">
            <v-card flat tile class="pb-0 pt-0">
              <v-card-text class="pb-0 pl-0">
                <!--v-textarea
                  class="monospaced-area"
                  v-model="graph_text"
                  :disabled="graph_text_disabled"
                  counter
                  rows="15"
                  name="graph_text"
                  label="Describe a graph here using Turtle (ttl)"
                  hint="@prefix : <http://example.org/> ."
                  v-on:input="checkSyntaxColorReset"
                ></v-textarea-->
                <v-lined-textarea
                  v-model="graph_text"
                  :nowrap="false"
                  counter
                  rows="15"
                  :disabled="graph_text_disabled"
                  label="Describe a graph here using Turtle (ttl)"
                  hint="@prefix : <http://example.org/> ."
                  v-on:input="checkSyntaxColorReset"
                />
              </v-card-text>
            </v-card>
          </v-tab-item>

          <v-tab-item value="tab-graph-graph" v-bind:style="styleObject.tabItem">
            <div ref="visNetworkGraph" v-bind:style="styleObject.dotContainer"></div>
          </v-tab-item>
        </v-tabs-items>

      </v-col>

      <!-- TDB alt left side -->
      <v-col v-if="graph_text_disabled" cols="12" md="6">
        <v-alert
          class="mt-0 mb-2"
          v-model="tdb_text_alert"
          type="error"
          close-text="Close Alert"
          dense
          dismissible
          >
          {{this.tdb_text_alert_text}}
        </v-alert>
        <v-btn
          v-if="!tdb_text_alert"
          class="primary accent-4 mr-2 mb-3 left white--text"
          :loading="tdb_fetch_loading"
          :disabled="tdb_fetch_loading"
          @click.prevent="getTdbModel"
        >
          Refresh TDB graph
        </v-btn>

        <!-- Tab titles -->
        <v-tabs
          v-model="tdb_tab"
          background-color="primary accent-4"
          right
          dark
          icons-and-text>
          <v-tabs-slider></v-tabs-slider>

          <v-tab disabled>
            See TDB as:
          </v-tab>

          <v-tab href="#tdb-text">
            Text
            <v-icon>mdi-text-subject</v-icon>
          </v-tab>

          <v-tab href="#tdb-graph">
            Graph
            <v-icon>mdi-share-variant</v-icon>
          </v-tab>
        </v-tabs>

        <!-- Tab Contents -->
        <v-tabs-items v-model="tdb_tab" v-bind:style="styleObject.tabItem">
          <v-tab-item value="tdb-text">
            <v-card flat tile max-height="600" class="pb-0 pt-0">
              <v-card-text class="pb-0">
                <!--v-textarea
                  class="monospaced-area"
                  v-model="this.tdb_text"
                  label="TDB graph (Turtle)"
                  readonly
                  rows="15"
                  no-resize
                  outlined
                  v-scroll
                ></v-textarea-->
                <v-lined-textarea
                  v-model="this.tdb_text"
                  label="TDB graph (Turtle)"
                  :nowrap="false"
                  counter
                  rows="15"
                  readonly
                  :no_resize="true"
                  outlined
                />
              </v-card-text>
            </v-card>
          </v-tab-item>

          <v-tab-item value="tdb-graph" v-bind:style="styleObject.tabItem">
            <v-row class="ma-2 mb-10">
              <vis-network
                ref="visTdbDot"
                v-bind:style="styleObject.dotContainer"
                :nodes="tdb_dot.nodes"
                :edges="tdb_dot.edges"
                :options="this.options"
              />
            </v-row>
          </v-tab-item>
        </v-tabs-items>
      </v-col>

      <!-- Right Side -->
      <v-col cols="12" md="6">


        <v-dialog
          v-model="prefixcc_dialog"
          width="600"
        >
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              class="primary accent-4 mr-2 mb-3 left white--text"
              dark
              v-bind="attrs"
              v-on="on"
            >
              <v-icon left>mdi-magnify</v-icon> prefix lookup
            </v-btn>
          </template>

          <v-card>
            <v-card-title class="headline primary white--text">
              Prefix Lookup
            </v-card-title>

            <v-card-text class="pt-5">
              Use <a href="http://prefix.cc" target="PrefixCC">prefix.cc</a> service to look for common prefixes.
              <v-row>
                <v-col cols="6">
                  <v-text-field
                    v-model="prefixcc_term"
                    label="Prefix"
                    hide-details="auto"
                    :rules="prefixcc_term_rules"
                    :loading="prefixcc_loading"
                    :error="prefixcc_result_error"
                  ></v-text-field>
                </v-col>
                <v-col cols="4">
                  <v-select
                    v-model="prefixcc_format_selected"
                    :items="prefixcc_format_choices"
                    label="Format"
                  ></v-select>
                </v-col>
                <v-col cols="2">
                  <v-btn
                    class="primary accent-2 white--text mt-1"
                    :disabled="prefixccLookupDisabled"
                    :loading="prefixcc_loading"
                    height="45"
                    @click.prevent="prefixLookUp"
                  >
                    <v-icon>mdi-magnify</v-icon>
                  </v-btn>
                </v-col>
              </v-row>

              <v-row>
                <v-col>
                  <v-text-field
                    v-model="prefixcc_result"
                    label="Lookup response"
                    outlined
                    readonly
                    :error="prefixcc_result_error"
                  ></v-text-field>
                </v-col>
              </v-row>
            </v-card-text>

            <v-divider></v-divider>

            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn
                color="primary"
                text
                @click="prefixcc_dialog = false"
              >
                Close dialog
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <v-tabs
            v-model="tools_tab"
            background-color="primary accent-4"
            grow
            dark
            icons-and-text>
          <v-tabs-slider></v-tabs-slider>

          <!-- Tab Titles -->
          <v-tab href="#tab-sparql">
            Sparql
            <v-icon>mdi-database-search</v-icon>
          </v-tab>

          <v-tab href="#tab-owl" :disabled="graph_text_disabled">
            Owl
            <v-icon>mdi-owl</v-icon>
          </v-tab>

          <v-tab href="#tab-shacl" :disabled="graph_text_disabled">
            SHACL
            <v-icon>mdi-eye-check-outline</v-icon>
          </v-tab>

          <v-tab href="#tab-shex" :disabled="graph_text_disabled">
            ShEx
            <v-icon>mdi-code-tags-check</v-icon>
          </v-tab>
        </v-tabs>

        <!-- Tab Contents -->
        <!-- Sparql -->
        <v-tabs-items v-model="tools_tab">
          <v-tab-item value="tab-sparql">
            <v-card flat tile class="mb-0">
              <v-card-text>
                <!--v-textarea
                    class="mb-0 monospaced-area"
                    rows="10"
                    v-model="sparql_text"
                    counter
                    name="sparql_text"
                    label="Query your graph..."
                    hint="SELECT * where {...}"
                ></v-textarea-->
                <v-lined-textarea
                  class="monospaced-area"
                  v-model="sparql_text"
                  :nowrap="false"
                  rows="10"
                  counter
                  name="sparql_text"
                  label="Query your graph..."
                  hint="SELECT * WHERE {...}"
                />
              </v-card-text>
            </v-card>
            <v-row class="ma-0 pa-0 pl-3">
              <v-col cols="6" class="mt-0 pa-0 pr-1">
                <v-overflow-btn
                  v-model="sparql_operation_selected"
                  v-if="!graph_text_disabled"
                  dense
                  class="mt-0 pa-0"
                  :items="sparql_operation_type"
                  v-on:change="sparqlSwitchFormat"
                  label="Operation type"
                />
                <v-overflow-btn
                  v-model="sparql_operation_selected"
                  v-if="graph_text_disabled"
                  dense
                  class="mt-0 pa-0"
                  :items="sparql_tdb_operation_type"
                  v-on:change="sparqlSwitchFormat"
                  label="Operation type"
                />
              </v-col>
              <v-col cols="6" class="mt-0 pa-0">
                <v-overflow-btn
                    v-model="sparql_format_selected"
                    dense
                    class="mt-0 pa-0"
                    :items="sparql_results_format"
                    label="Result format"
                    target="#sparql-results-format-dropdown"
                />
              </v-col>
            </v-row>
            <v-row class="ma-0 pa-0 pl-3">
              <v-btn
                v-if="!graph_text_disabled"
                class="ma-2"
                @click.prevent="sparqlModelQuery"
                :loading="sparql_run_loading"
                :disabled="sparqlBtnDisabled"
                color="primary"
              >
                Run Query
              </v-btn>
              <v-btn
                v-if="graph_text_disabled"
                class="ma-2"
                @click.prevent="sparqlTdb"
                :loading="sparql_tdb_run_loading"
                :disabled="sparqlTdbBtnDisabled"
                color="primary"
              >
                Query TDB
              </v-btn>
              <v-btn class="ma-2" @click.prevent="sparqlFieldReset" color="primary">Reset</v-btn>
              <v-switch
                v-if="!sparql_alert"
                v-model="graph_text_disabled"
                :label="`SPARQL Query or Update to TDB (disables graph editing and other tools)`"
                v-on:change="sparqlClearSelectedFormatAndGetTdb"
              ></v-switch>
              <v-alert
                v-model="sparql_alert"
                type="error"
                close-text="Close Alert"
                dismissible
              >
                {{this.sparql_alert_msg}}
              </v-alert>
            </v-row>
          </v-tab-item>

          <!-- Owl -->
          <v-tab-item value="tab-owl">
            <v-card flat tile class="mb-0">
              <v-card-text>
                <!--v-textarea
                  class="mb-0 monospaced-area"
                  rows="10"
                  v-model="owl_text"
                  counter
                  name="owl_text"
                  label="OPTIONAL: You can write reasoner specific triples here using Turtle (ttl)"
                  hint=":x rdfs:subPropertyOf :y ."
                ></v-textarea-->
                <v-lined-textarea
                  v-model="owl_text"
                  class="mb-0 monospaced-area"
                  :nowrap="false"
                  counter
                  rows="10"
                  name="owl_text"
                  label="OPTIONAL: You can write reasoner specific triples here using Turtle (ttl)"
                  hint=":x rdfs:subPropertyOf :y ."
                />
              </v-card-text>
            </v-card>
            <v-row class="ma-0 pa-0 pl-3">
              <v-col cols="6" class="mt-0 pa-0 pr-1">
                <v-overflow-btn
                  v-model="owl_type_selected"
                  dense
                  class="mt-0 pa-0"
                  :items="owl_type"
                  label="Select a reasoner"
                />
              </v-col>
            </v-row>
            <v-row class="ma-0 pa-0 pl-3">
              <v-btn
                class="ma-2"
                @click.prevent="owlReason"
                :loading="owl_run_loading"
                :disabled="owlBtnDisabled"
                color="primary"
              >
                Infer over graph
              </v-btn>
              <v-alert
                v-model="owl_alert"
                type="error"
                close-text="Close Alert"
                dismissible
              >
                {{this.owl_alert_msg}}
              </v-alert>
            </v-row>
          </v-tab-item>

          <!-- SHACL -->
          <v-tab-item value="tab-shacl">
            <v-card flat tile class="mb-0">
              <v-card-text>
                <!--v-textarea
                  class="mb-0 monospaced-area"
                  rows="12"
                  v-model="shacl_text_shape"
                  counter
                  name="shacl_text"
                  label="Write your shape rules here (graph prefixes do not apply)"
                  hint=":UserShape a sh:NodeShape;"
                ></v-textarea-->
                <v-lined-textarea
                  class="mb-0 monospaced-area"
                  rows="12"
                  v-model="shacl_text_shape"
                  counter
                  name="shacl_text"
                  label="Write your shape rules here (graph prefixes do not apply)"
                  hint=":UserShape a sh:NodeShape;"
                />
              </v-card-text>
            </v-card>
            <v-row class="ma-0 pa-0 pl-3">
              <v-btn
                class="ma-2"
                @click.prevent="shaclValidate"
                :loading="shacl_run_loading"
                :disabled="shaclBtnDisabled"
                color="primary"
              >
                Validate graph
              </v-btn>
              <v-alert
                v-model="shacl_alert"
                type="error"
                close-text="Close Alert"
                dismissible
              >
                {{this.shacl_alert_msg}}
              </v-alert>
            </v-row>
          </v-tab-item>

          <!-- ShEx -->
          <v-tab-item value="tab-shex">
            <v-card flat tile class="mb-0">
              <v-card-text>
                <!--v-textarea
                  class="mb-0 monospaced-area"
                  rows="8"
                  v-model="shex_text_shape"
                  counter
                  name="shex_text"
                  label="Write you shape rules here"
                  hint=":user { ... }"
                ></v-textarea-->
                <v-lined-textarea
                  class="mb-0 monospaced-area"
                  rows="8"
                  v-model="shex_text_shape"
                  counter
                  name="shex_text"
                  label="Write you shape rules here"
                  hint=":user { ... }"
                />
              </v-card-text>
            </v-card>
            <v-row class="ma-0 pa-0 pl-3">
              <v-col v-if="shex_alert" cols="12" class="mt-3">
                <v-alert
                  class="mt-3"
                  v-model="shex_alert"
                  type="error"
                  close-text="Close Alert"
                  dismissible
                >
                  {{this.shex_alert_msg}}
                </v-alert>
              </v-col>
              <v-col v-if="!shex_alert" cols="4" class="mt-0 ml-0 d-none d-lg-flex d-xl-flex d-md-none d-xs-none">
                <v-btn
                  class="ma-2"
                  @click.prevent="shexValidate"
                  :loading="shex_run_loading"
                  :disabled="shexBtnDisabled"
                  color="primary"
                >
                  Validate graph
                </v-btn>
              </v-col>
              <v-col v-if="!shex_alert" cols="12" lg="8" xl="8" md="12" sm="8" xs="12" class="mt-0 pb-0 pt-0 pl-0">
                <v-card flat tile class="mb-0">
                  <v-card-text class="pt-0">
                    <v-textarea
                      class="mb-0"
                      rows="4"
                      v-model="shex_text_shape_map"
                      counter
                      name="shex_text_map"
                      label="What you want to validate against what"
                      hint=":alice@:user"
                    ></v-textarea>
                  </v-card-text>
                </v-card>
              </v-col>
              <v-col v-if="!shex_alert" cols="12" md="4" sm="4" class="mt-0 pt-0 ml-0 d-flex d-lg-none d-xl-none d-md-flex d-xs-flex">
                <v-btn
                  class="ma-2 pt-0"
                  @click.prevent="shexValidate"
                  :loading="shex_run_loading"
                  :disabled="shexBtnDisabled"
                  color="primary"
                >
                  Validate graph
                </v-btn>
              </v-col>
            </v-row>
          </v-tab-item>
        </v-tabs-items>
      </v-col>
    </v-row>

    <!-- Results Row: It will display any output from the tools -->
    <v-row class="text-center pb-12" v-if="resultsAvailable">
      <v-col cols="12">
        <h1>Results</h1>
        <v-tabs
          v-model="result_tab"
          background-color="primary accent-4"
          left
          dark
          icons-and-text>
          <v-tabs-slider></v-tabs-slider>

          <v-tab disabled>
            See result as:
          </v-tab>

          <v-tab href="#tab-result-text">
            Text
            <v-icon>mdi-text-subject</v-icon>
          </v-tab>

          <v-tab href="#tab-result-graph" :disabled="!resultsDotAvailable">
            Graph
            <v-icon>mdi-share-variant</v-icon>
          </v-tab>

        </v-tabs>


        <v-tabs-items v-model="result_tab">

          <v-tab-item value="tab-result-text">
            <v-container>
              <v-row>
                <v-col cols="2">
                  <v-card color="blue darken-2" min-height="250" dark>
                    <v-card-title>
                      From:
                    </v-card-title>
                    <v-card-text>
                      <v-icon x-large>{{this.result_icon}}</v-icon>
                      {{this.result_origin}}
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="10">
                  <v-card min-height="250">
                    <v-card-title>
                      Result
                    </v-card-title>
                    <v-card-text class="text-left" ref="result_text">
                      <!--v-textarea
                        class="monospaced-area"
                        v-model="result_text"
                        :row-height="16"
                        flat
                        solo
                        auto-grow
                        disabled
                      /-->
                      <v-lined-textarea
                        class="monospaced-area"
                        v-model="result_text"
                        :row-height="16"
                        flat
                        solo
                        autogrow="true"
                        :disabled_input="true"
                      />
                    </v-card-text>
                    <v-card-actions>
                      <v-btn :disabled="!copyResultEnabled" v-on:click="resultToClipboard">
                        <v-icon>mdi-content-copy</v-icon>
                        Copy to clipboard
                      </v-btn>
                    </v-card-actions>
                  </v-card>
                </v-col>
              </v-row>
            </v-container>
          </v-tab-item>

          <v-tab-item value="tab-result-graph">
            <v-row class="ma-2 mb-10">
              <vis-network
                ref="visResultDot"
                v-bind:style="styleObject.dotResultsContainer"
                :nodes="result_dot.nodes"
                :edges="result_dot.edges"
                :options="this.options"
              />
            </v-row>
          </v-tab-item>

        </v-tabs-items>
      </v-col>
    </v-row>

    <v-row class="text-center mt-12">
      <!-- Foooter -->
      <v-footer
        absolute
        color="primary"
        padless
      >
        <v-row
          justify="center"
          no-gutters
        >

          <v-dialog
            v-model="about_dialog"
            persistent
            max-width="390"
          >
            <template v-slot:activator="{ on, attrs }">
              <v-btn
                class="lighten-4 ma-1 mb-0 left white--text"
                dark
                rounded
                outlined
                text
                v-bind="attrs"
                v-on="on"
              >
                <v-icon>mdi-information-variant</v-icon> About Example
              </v-btn>
            </template>
            <v-card>
              <v-card-title class="headline">
                Display interactive about?
              </v-card-title>
              <v-card-text>About will modify all text fields in order to display to you a working example. Be sure to save your content before continuing.</v-card-text>
              <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn
                  color="primary darken-1"
                  text
                  @click="about_dialog = false"
                >
                  Cancel
                </v-btn>
                <v-btn
                  color="primary darken-1"
                  text
                  :loading="aboutLoading"
                  @click="aboutAccepted"
                >
                  Continue
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>

          <!--v-btn
            class="ma-1 mb-0 left white--text"
            rounded
            color="white"
            text
            disabled
          >
            Wiew on <v-icon>mdi-github</v-icon>
          </v-btn -->

          <v-col
            class="primary py-2 text-center white--text"
            cols="12"
          >
            RDF Playground © {{ new Date().getFullYear() }} — <strong>Bastián Inostroza</strong>
          </v-col>
        </v-row>
      </v-footer>
    </v-row>
  </v-container>
</template>

<style lang="css" scoped>
  .monospaced-area{
    font-family: monospace;
    line-height: 1.3rem !important;
  }
</style>

<script>
  import {parseDOTNetwork} from 'vis-network';
  import vis from 'vis';
  import VLinedTextarea from "./VLinedTextarea";

  const backAPI = "localhost:9060";

  const MimeTypes = {
      "Text": "text/plain ;charset=utf-8",
      "XML": "application/xml ;charset=utf-8",
      "SSE": "text/x-sse ;charset=utf-8",
      "CSV": "text/csv ;charset=utf-8",
      "JSON": "application/json ;charset=utf-8",
      "TSV": "text/tab-separated-values ;charset=utf-8",
      "TTL": "text/turtle ;charset=utf-8",
      "NTRIPLES": "application/n-triples ;charset=utf-8",
      "DOT": "text/vnd.graphviz ;charset=utf-8"
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
    name: 'HelloWorld',
      components: {VLinedTextarea},
      data: () => ({
      vis_graph: null,
      check_syntax_color: "primary",
      check_syntax_icon: "clipboard-pulse",
      check_syntax_text: "Check Syntax",
      check_syntax_loading: false,
      check_syntax_error: false,
      graph_tab: null, // left tab reference
      tools_tab: null, // right tab reference
      graph_text: "@base <http://example.org/> .\n"+   // graph description initial and current value
        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"+
        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n\n",
      graph_text_disabled: false,
      graph_dot: "digraph G {\n}",
      graph_error_text: "",
      tdb_tab: null,
      tdb_text: "Click on refresh to see the TDB graph",
      tdb_text_alert: false,
      tdb_text_alert_text: "",
      tdb_fetch_loading: false,
      tdb_dot: "",
      sparql_alert: false,
      sparql_alert_msg: "There was an error",
      sparql_text: "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
          "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n" +
          "\n" +
          "SELECT * WHERE {?s ?p ?o} LIMIT 10",
      sparql_operation_type: ['ASK', 'CONSTRUCT', 'DESCRIBE', 'SELECT'],
      sparql_tdb_operation_type: ['ASK', 'CONSTRUCT', 'DESCRIBE', 'SELECT', 'INPUT', 'DELETE', 'LOAD', 'CLEAR'],
      sparql_operation_selected: "",
      sparql_format_selected: "",
      sparql_results_format: [],
      sparql_run_loading: false,
      sparql_tdb_run_loading: false,
      owl_text: "@base           <http://ex.org/> .\n" +
          "@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
          "@prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#> .\n" +
          "@prefix owl:    <http://www.w3.org/2002/07/owl#> .\n" +
          "@prefix xsd:    <http://www.w3.org/2001/XMLSchema#> .",
      owl_type: ['RDFS', 'OWL'],
      owl_type_selected: "",
      owl_run_loading: false,
      owl_alert: false,
      owl_alert_msg: "There was an error",
      shacl_text_shape: "@base           <http://ex.org/> .\n" +
          "@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
          "@prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#> .\n" +
          "@prefix sh:     <http://www.w3.org/ns/shacl#> .\n" +
          "@prefix xsd:    <http://www.w3.org/2001/XMLSchema#> .",
      shacl_run_loading: false,
      shacl_alert: false,
      shacl_alert_msg: "There was an error",
      shex_text_shape: "BASE <http://ex.org/>\n" +
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
          width: '100%',
          height: '500px',
          border: '1px solid #d3d3d3',
        },
        dotResultsContainer: {
          flex: 1,
          width: '100%',
          height: '700px',
          border: '1px solid #d3d3d3',
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
          backgroundColor: 'secondary',
        }
      },
      prefixcc_dialog: false,
      prefixcc_loading: false,
      prefixcc_format_choices: ["TTL", "SPARQL"],
      prefixcc_format_selected: "TTL",
      prefixcc_term: "",
      prefixcc_term_rules: [
        value => !!value || 'Required.',
        value => (value || '').length <= 20 || 'Max 20 characters',
      ],
      prefixcc_result: "",
      prefixcc_result_error: false,
      about_dialog: false,
      aboutLoading: false,
      aboutGraph: "@base   <http://ex.org/> .\n" +
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
          "        rdfs:label \"RDF Playground\"@en .\n" +
          "\n" +
          "<binostroza> a foaf:Person ;\n" +
          "        foaf:name \"Bastián Inostroza\" ;\n" +
          "        marcrel:dgs <http://aidanhogan.com/foaf.rdf#me> ;\n" +
          "        schema:affiliation wikidata:Q232141 .\n" +
          "\n" +
          "<http://aidanhogan.com/foaf.rdf#me> schema:affiliation wikidata:Q232141 .",
      aboutSparql: "PREFIX marcrel: <http://id.loc.gov/vocabulary/relators/>\n" +
          "PREFIX schema:  <http://schema.org/>\n" +
          "\n" +
          "SELECT * WHERE {\n" +
          "    ?student marcrel:dgs ?professor .\n" +
          "    ?student schema:affiliation ?afiliation .\n" +
          "    ?professor schema:affiliation ?afiliation .\n" +
          "}\n" +
          "LIMIT 10",
      aboutOwl: "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
          "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
          "@prefix owl:  <http://www.w3.org/2002/07/owl#> .\n" +
          "@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .\n" +
          "\n" +
          "marcrel:dgs rdfs:subPropertyOf foaf:knows .",
      aboutShacl: "@base   <http://ex.org/> .\n" +
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
      aboutShexDescription: "BASE    <http://ex.org/>\n" +
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
      aboutShexPairing: "<http://ex.org/rdfpg>@<http://ex.org/Project>"
    }),

    methods: {
      aboutAccepted: function () {
        this.aboutLoading = true;
        // Fill all user writable fields with data
        this.graph_text = this.aboutGraph;
        this.sparql_text = this.aboutSparql;
        this.sparql_operation_selected='SELECT';
        this.sparqlSwitchFormat(this.sparql_operation_selected);
        this.owl_text = this.aboutOwl;
        this.owl_type_selected='OWL';
        this.shacl_text_shape = this.aboutShacl;
        this.shex_text_shape = this.aboutShexDescription;
        this.shex_text_shape_map = this.aboutShexPairing;
        // terminate animation and close
        this.aboutLoading = false;
        this.about_dialog = false;
      },
      prefixLookUp: function () {
        this.prefixcc_loading = true;
        let format = this.prefixcc_format_selected.toLowerCase();
        fetch(`http://prefix.cc/`+this.prefixcc_term+`.file.`+format, {
            method: 'GET'
        }
        ).then(response => {
          if(!response.ok) {
            this.prefixcc_result = "Prefix Not Found. Add it on prefix.cc! :D";
            this.prefixcc_result_error = true;
            this.prefixcc_loading = false;
          } else {
            // Successful request :D
            response.text().then(response => {
              this.prefixcc_result = response;
              this.prefixcc_result_error = false;
              this.prefixcc_loading = false;
            })
          }
        }).catch(error => {
          this.prefixcc_result = error;
          this.prefixcc_result_error = true;
          this.prefixcc_loading = false;
        });
      },
      checkSyntaxColorReset: function () {
        // resets the check syntax button colour to blue
        this.check_syntax_color = "primary";
      },
      checkSyntax: function () {
        // sends the text to the backend to have it's syntax verified, also gets a DOT version to display

        // Starts Loading animation
        this.check_syntax_loading = true;
        // Check the graph syntax, and if it's correct displays it on graph tab
        let requestBody = { data: this.graph_text.toString()};
        // request syntax check to backend
        fetch(`http://` + backAPI + `/api/model/syntax_check`, {
          method: 'POST',
          headers: new Headers({
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify(requestBody),
        })
          .then(response => {
            // display an error on the page console
            if (!response.ok) {
              response.json().then(responseBody => {
                  this.graph_error_text = 'response not 200, was ' + response.status + ' ' + responseBody.syntax_error;
                  this.check_syntax_loading = false;
                  this.check_syntax_error = true;
              });
            }
            else {
              response.json().then(content => {
                // if there is an error graph_dot will be empty
                this.graph_dot = content.data_dot;
                this.graph_error_text = content.syntax_error;
                if (content.data_dot === "" && content.syntax_error !== "") {
                  // the is a syntax error, so we convert the button colour to red and display the error
                  this.check_syntax_error = true;
                  this.check_syntax_icon = "close";
                  this.check_syntax_text = "Check Again";
                  this.check_syntax_color = "red darken-4"
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
                  } catch (e) {
                      this.check_syntax_loading = false;
                  }
                }

                this.check_syntax_loading = false;
              })
            }
          })
          .catch(error => {
              this.graph_error_text = 'There was a problem with the request: ' + error;
              this.check_syntax_loading = false;
              this.check_syntax_error = true;
          })
      },
      dotFetchResult: function(text, format) {
        // gets a dot version of the selected text (we need to specify the format)
        let dot = "";
        let reqBody = {
          data: text,
          data_lang: format
        };
        fetch(`http://` + backAPI + `/api/model/dot`, {
          method: 'POST',
          headers: new Headers({
            'Content-Type': 'application/json',
            'Accept': MimeTypes.Text+', '+MimeTypes.DOT
          }),
          body: JSON.stringify(reqBody)
        }).then(response => {
          if (response.ok) {
            response.text().then(body => {
              dot = body;
              this.result_dot = parseDOTNetwork(dot);
              return dot;
            })
          }
        }).catch(reason => console.warn('Error getting DOT: ' + reason));

        return dot;
      },
      getTdbModel: function () {
        // get the TDB entire model to be displayed
        this.tdb_fetch_loading = true;
        // get model and display it
        fetch(`http://` + backAPI + `/api/tdb/get_model`, {
          method: 'GET',
        }).then(response => {
          if (!response.ok) {
            response.text().then(body => {
              let msg = body.toString();
              this.tdbShowAlert("Error getting TDB graph: "+ response.status+ ' ' + msg);
            })
          } else {
            // process and display
            response.json().then(responseBody => {
              // response: { data: "model on TTL", dot: "model on DOT"}
              let model = responseBody.data;
              let dot = responseBody.dot;

              // display data on the TDB tab and stop the loading animation
              this.tdb_text = model;
              this.tdb_dot = parseDOTNetwork(dot);
              this.tdb_text_alert = false;
              this.tdb_fetch_loading = false;
            })
          }
        })
      },
      tdbShowAlert: function (message) {
        // displays an alert on top of the refresh tdb graph button
        this.tdb_text_alert = true;
        this.tdb_text_alert_text = message;
      },
      sparqlModelQuery: function () {
        // hides error message if not deleted yet
        this.sparql_alert = false;
        // sends the query and graph to the backend, and gets the result
        this.sparql_run_loading = true;
        let requestBody = {
          data: this.graph_text.toString(),
          data_lang: "TTL",                 // default, could be changed on the future
          query: this.sparql_text.toString(),
          query_response_lang: this.sparql_format_selected
        };
        fetch(`http://` + backAPI + `/api/tdb/query_model`, {
          method: 'POST',
          headers: new Headers({
            'Content-Type': 'application/json',
            'Accept': MimeTypes.Text+', '+MimeTypes[this.sparql_format_selected]
          }),
          body: JSON.stringify(requestBody),
        }).then(response => {
          if (!response.ok) {
            response.text().then(body => {
                let text = body.toString();
                this.sparqlShowAlert("Response was not OK, was: " + response.status + ' '+ text);
            });
            this.sparql_run_loading = false;
          }
          else {
            response.text().then(bodyText => {
              this.result_text = bodyText;
              this.result_icon = "mdi-database-search";
              this.result_origin = "SPARQL";
              this.sparql_run_loading = false;

              // Try to display the DOT version if applicable
              if (this.sparql_operation_selected === 'CONSTRUCT'
                || this.sparql_operation_selected === 'DESCRIBE'
              ) {
                if (this.sparql_format_selected === 'DOT') {
                  // Result Text is DOT and we can use that
                  this.result_dot = parseDOTNetwork(this.result_text);
                  this.sparql_run_loading = false;
                } else {
                  // Fetch DOT version from backend
                  this.dotFetchResult(this.result_text, this.sparql_format_selected);
                }
              }
            });
          }
        }).catch(reason => {
          this.sparqlShowAlert(reason);
          this.sparql_run_loading = false;
        });
      },
      sparqlTdb: function () {
        // hides error message if not deleted yet
        this.sparql_alert = false;
        // makes and sparql query directly to the backend TDB
        this.sparql_tdb_run_loading = true;
        let operation = this.sparql_operation_selected;
        let selected_operation_type = ""; // blank will be rejected by the backend
        if (['ASK', 'SELECT', 'CONSTRUCT', 'DESCRIBE'].includes(operation)) {
          // is a query
          selected_operation_type = "query"
        } else if (['INPUT', 'DELETE', 'LOAD', 'CLEAR'].includes(operation)) {
          // is an update
          selected_operation_type = "update"
        }
        let requestBody = {
          query: this.sparql_text.toString(),
          type: selected_operation_type.toString()
        };
        fetch(`http://` + backAPI + `/api/tdb/query_tdb`, {
          method: 'POST',
          headers: new Headers({
            'Content-Type': 'application/json',
            'Accept': MimeTypes.Text+', '+MimeTypes[this.sparql_format_selected]
          }),
          body: JSON.stringify(requestBody),
        }).then(response => {
          if (!response.ok) {
            // extract error message and display it
            response.text().then(body => {
              let text = body.toString();
              this.sparqlShowAlert("Response was not OK, was: " + response.status + ' ' + text);
            });
            this.sparql_tdb_run_loading = false;
          } else {
            // process and show response
            response.text().then(bodyText => {
              // set the image and text of the result section
              this.result_text = bodyText;
              this.result_icon = "mdi-database-search";
              this.result_origin = "SPARQL TDB";
              this.sparql_tdb_run_loading = false;

              // if the response is DOT text, display it on the result section
              if (['CONSTRUCT', 'DESCRIBE'].includes(this.sparql_operation_selected)) { // these operations support DOT
                if (this.sparql_format_selected === 'DOT') {
                  // Result Text is DOT and we can parse it to be displayed
                  this.result_dot = parseDOTNetwork(this.result_text);
                  this.sparql_tdb_run_loading = false;
                } else {
                  // Fetch DOT version from backend
                  this.dotFetchResult(this.result_text, this.sparql_format_selected);
                }
              }
            })
          }
        }).catch(error => {
          this.sparqlShowAlert(error);
          this.sparql_tdb_run_loading = false;
        })
      },
      sparqlClearSelectedFormatAndGetTdb: function () {
        // Clears the current result, making the user choose again according to the new options
        this.sparqlSwitchFormat(this.sparql_operation_selected);
        // Additionally try to load TDB graph
        this.getTdbModel();
      },
      sparqlSwitchFormat: function (type) {
        // Changes the selector options according to the user selection
        this.sparql_format_selected = "";
        if (type === 'ASK') {
          this.sparql_results_format = ['Text', 'XML'];
        } else if (type === 'SELECT') {
          this.sparql_results_format = ['Text', 'XML', 'CSV', 'TSV'];
        } else if (type === 'CONSTRUCT' || type === 'DESCRIBE') {
          this.sparql_results_format = ['SSE', 'JSON', 'TTL', 'NTRIPLES', 'DOT'];
        } else if (type === 'INPUT' || type === 'DELETE') {
          this.sparql_results_format = ['Text'];
        } else if (type === 'LOAD' || type === 'CLEAR') {
          this.sparql_results_format = ['Text'];
        }
      },
      sparqlFieldReset: function () {
        // Changes text to the original selection
        this.sparql_text = "SELECT * WHERE {?s ?p ?o} LIMIT 10";
      },
      sparqlShowAlert: function (message) {
        // Shows an alert on the buttons row
        this.sparql_alert_msg = message;
        this.sparql_alert = true;
      },
      owlReason: function () {
        // disables alert if not done by the user yet
        this.owl_alert = false;
        // sends the model to the backend to be reasoned over
        this.owl_run_loading = true;
        let profile = this.owl_type_selected;
        let data = this.graph_text.toString();
        // Add specifics
        if (this.owl_text !== "") {
          data += '\n' + this.owl_text.toString();
        }
        let reqBody = {
          data: data,
          data_lang: "TTL",
          profile: profile
        };

        fetch(`http://` + backAPI + `/api/owl/reason`, {
          method: 'POST',
          headers: new Headers({
            'Content-Type': 'application/json',
            'Accept': MimeTypes.Text+', '+ MimeTypes.JSON
          }),
          body: JSON.stringify(reqBody)
        }).then(response => {
          if (!response.ok) {
            this.owlShowAlert("Response was not OK, was: "+ response.status);
            this.owl_run_loading = false;
          } else {
            response.json().then(responseBody => {
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
            })
          }
        }).catch(reason => {
            this.owlShowAlert(reason);
            this.owl_run_loading = false;
        })
      },
      owlShowAlert: function (message) {
        // display an alert on the owl buttons row
        this.owl_alert_msg = message;
        this.owl_alert = true;
      },
      shaclValidate: function () {
        // disables alert if not done by the user yet
        this.shacl_alert = false;
        // validates the graph with the specified shacl rules on the backend
        this.shacl_run_loading = true;
        let reqBody = {
          data: this.graph_text.toString(),
          data_lang: "TTL",
          shape: this.shacl_text_shape.toString(),
          shape_lang: "TTL"
        };

        fetch(`http://` + backAPI + `/api/shape/shacl_isvalid`, {
          method: 'POST',
          headers: new Headers({
          'Content-Type': MimeTypes.JSON,
          'Accept': MimeTypes.Text+', '+ MimeTypes.JSON
          }),
          body: JSON.stringify(reqBody)
        }).then(response => {
          if (!response.ok || response.status === 204) {
            if (response.status === 204) {
              this.shaclShowAlert("There is no content to be processed");
              this.shacl_run_loading = false;
            } else {
              response.text().then(text => {
                this.shaclShowAlert("Problem: " + text.toString());
                this.shacl_run_loading = false;
              });
            }
          } else {
            response.json().then(responseBody => {
              let report = responseBody.report;
              let fusion = responseBody.fusion_dot;

              this.result_origin = "SHACL";
              this.result_icon = "mdi-eye-check-outline";
              this.result_dot = parseDOTNetwork(fusion);
              if (report === "") {
                this.result_text = "The data graph conforms with the shape graph!";
              } else {
                this.result_text = report;
              }
              this.shacl_run_loading = false;
            })
          }
        }).catch(reason => {
          this.shaclShowAlert(reason);
          this.shacl_run_loading = false;
        })
      },
      shaclShowAlert: function (message) {
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
          shape_map: this.shex_text_shape_map
        };

        fetch(`http://` + backAPI + `/api/shape/shex_isvalid`, {
          method: 'POST',
          headers: new Headers({
              'Content-Type': 'application/json',
              'Accept': MimeTypes.Text
          }),
          body: JSON.stringify(requestBody),
        }).then(response => {
          if (!response.ok) {
            response.text().then(body => {
              let text = body.toString();
              this.shexShowAlert("Response was not OK, was: " + response.status + ' '+ text);
            });
            this.shex_run_loading = false;
          } else {
            response.text().then(bodyText => {
              this.result_text = bodyText;
              this.result_icon = "mdi-code-tags-check";
              this.result_origin = "ShEx";
              this.shex_run_loading = false;
            });
          }
        }).catch(reason => {
          this.shexShowAlert(reason);
          this.shex_run_loading = false;
        });
      },
      shexShowAlert: function (message) {
        // shows an error message on the shex row bar
        this.shex_alert_msg = message;
        this.shex_alert = true;
      },
      resultToClipboard: async function () {
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
          return this.shex_text_shape === "" || this.shex_run_loading || this.shex_text_shape_map === "";
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
      }
    }
  }
</script>
