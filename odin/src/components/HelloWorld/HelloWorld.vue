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
          {{ this.graph_error_text }}
        </v-alert>
        <v-btn
          v-if="!check_syntax_error"
          :color="check_syntax_color"
          class="mr-2 mb-3 left white--text"
          :loading="check_syntax_loading"
          :disabled="check_syntax_loading"
          @click.prevent="checkSyntax"
        >
          <v-icon left>mdi-{{ this.check_syntax_icon }}-outline</v-icon>
          {{ this.check_syntax_text }}
        </v-btn>

        <!-- Tab titles -->
        <v-tabs
          v-model="graph_tab"
          background-color="primary accent-4"
          right
          dark
          icons-and-text
        >
          <v-tabs-slider></v-tabs-slider>

          <v-tab disabled>
            See the graph as:
          </v-tab>

          <v-tooltip bottom>
            <template v-slot:activator="{ on, attrs }">
              <v-tab href="#tab-graph-text" v-bind="attrs" v-on="on">
                Text
                <v-icon>mdi-text-subject</v-icon>
              </v-tab>
            </template>
            <span>Enter your RDF data in Turtle syntax.</span>
          </v-tooltip>

          <v-tooltip bottom>
            <template v-slot:activator="{ on, attrs }">
              <v-tab
                href="#tab-graph-graph"
                @click="checkSyntax"
                v-bind="attrs"
                v-on="on"
              >
                Graph
                <v-icon>mdi-share-variant</v-icon>
              </v-tab>
            </template>
            <span>
              View your RDF data as a graph.
            </span>
          </v-tooltip>
        </v-tabs>

        <!-- Tab Contents -->
        <v-tabs-items v-model="graph_tab" v-bind:style="styleObject.tabItem">
          <v-tab-item value="tab-graph-text">
            <v-card flat tile class="pb-0 pt-0">
              <v-card-text class="pb-0 pl-0">
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

          <v-tab-item
            value="tab-graph-graph"
            v-bind:style="styleObject.tabItem"
          >
            <v-row class="ma-2 mb-1">
              <div
                ref="visNetworkGraph"
                v-bind:style="styleObject.dotContainer"
              ></div>
            </v-row>
            <v-row class="ma-2 mt-0 mb-10">
              <v-dialog
                v-model="graph_vis_dialog"
                fullscreen
                hide-overlay
                transition="dialog-bottom-transition"
              >
                <template v-slot:activator="{ on, attrs }">
                  <v-btn color="primary" dark v-bind="attrs" v-on="on">
                    <v-icon>mdi-fullscreen</v-icon> View on Fullscreen
                  </v-btn>
                </template>
                <v-card>
                  <v-toolbar dark color="primary">
                    <v-btn icon dark @click="graph_vis_dialog = false">
                      <v-icon>mdi-close</v-icon>
                    </v-btn>
                    <v-toolbar-title>Graph Visualization</v-toolbar-title>
                    <v-spacer></v-spacer>
                    <v-toolbar-title class="hidden-sm-and-down"
                      >Save as image using secondary (right) click over the
                      visualization</v-toolbar-title
                    >
                  </v-toolbar>
                  <vis-network
                    ref="visNetworkGraph"
                    v-bind:style="styleObject.dotFullScreenContainer"
                    :nodes="graph_dot_vis.nodes"
                    :edges="graph_dot_vis.edges"
                    :options="this.options"
                  />
                </v-card>
              </v-dialog>
            </v-row>
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
          {{ this.tdb_text_alert_text }}
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
          icons-and-text
        >
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
            <v-row class="ma-2 mt-0">
              <vis-network
                ref="visTdbDot"
                v-bind:style="styleObject.dotContainer"
                :nodes="tdb_dot.nodes"
                :edges="tdb_dot.edges"
                :options="this.options"
              />
            </v-row>
            <v-row class="ma-2 mt-0 mb-10">
              <v-dialog
                v-model="tdb_vis_dialog"
                fullscreen
                hide-overlay
                transition="dialog-bottom-transition"
              >
                <template v-slot:activator="{ on, attrs }">
                  <v-btn color="primary" dark v-bind="attrs" v-on="on">
                    <v-icon>mdi-fullscreen</v-icon> View on Fullscreen
                  </v-btn>
                </template>
                <v-card>
                  <v-toolbar dark color="primary">
                    <v-btn icon dark @click="tdb_vis_dialog = false">
                      <v-icon>mdi-close</v-icon>
                    </v-btn>
                    <v-toolbar-title>TDB visualization</v-toolbar-title>
                    <v-spacer></v-spacer>
                    <v-toolbar-title class="hidden-sm-and-down">
                      Save as image using secondary (right) click over the
                      visualization
                    </v-toolbar-title>
                  </v-toolbar>
                  <vis-network
                    ref="visTdbDot"
                    v-bind:style="styleObject.dotFullScreenContainer"
                    :nodes="tdb_dot.nodes"
                    :edges="tdb_dot.edges"
                    :options="this.options"
                  />
                </v-card>
              </v-dialog>
            </v-row>
          </v-tab-item>
        </v-tabs-items>
      </v-col>

      <!-- Right Side -->
      <v-col cols="12" md="6">
        <v-dialog v-model="prefixcc_dialog" width="600">
          <template v-slot:activator="{ on, attrs }">
            <v-tooltip top v-bind="attrs" v-on="on">
              <template v-slot:activator="{ on, attrs }">
                <v-btn
                  class="primary accent-4 mr-2 mb-3 left white--text"
                  dark
                  v-bind="attrs"
                  v-on="on"
                  v-on:click="prefixcc_dialog = true"
                >
                  <v-icon left>mdi-magnify</v-icon> prefix lookup
                </v-btn>
              </template>
              <span>
                Look up a common prefix string to add to your data, graph,
                query, etc.
              </span>
            </v-tooltip>
          </template>

          <v-card>
            <v-card-title class="headline primary white--text">
              Prefix Lookup
            </v-card-title>

            <v-card-text class="pt-5">
              Use
              <a href="http://prefix.cc" target="PrefixCC">prefix.cc</a> service
              to look for common prefixes.
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
              <v-btn color="primary" text @click="prefixcc_dialog = false">
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
          icons-and-text
        >
          <v-tabs-slider></v-tabs-slider>

          <!-- Tab Titles -->
          <v-tooltip bottom>
            <template v-slot:activator="{ on, attrs }">
              <v-tab href="#tab-sparql" v-bind="attrs" v-on="on">
                Sparql
                <v-icon>mdi-database-search</v-icon>
              </v-tab>
            </template>
            <span
              >Specify a SPARQL query to run on the RDF data you have
              entered.</span
            >
          </v-tooltip>

          <v-tooltip bottom>
            <template v-slot:activator="{ on, attrs }">
              <v-tab
                href="#tab-owl"
                :disabled="graph_text_disabled"
                v-bind="attrs"
                v-on="on"
              >
                Owl
                <v-icon>mdi-owl</v-icon>
              </v-tab>
            </template>
            <span
              >Specify some axioms in RDFS/OWL to run reasoning over the RDF
              data.</span
            >
          </v-tooltip>

          <v-tooltip bottom>
            <template v-slot:activator="{ on, attrs }">
              <v-tab
                href="#tab-shacl"
                :disabled="graph_text_disabled"
                v-bind="attrs"
                v-on="on"
              >
                SHACL
                <v-icon>mdi-eye-check-outline</v-icon>
              </v-tab>
            </template>
            <span>Specify constraints in SHACL to validate the RDF graph.</span>
          </v-tooltip>

          <v-tooltip bottom>
            <template v-slot:activator="{ on, attrs }">
              <v-tab
                href="#tab-shex"
                :disabled="graph_text_disabled"
                v-bind="attrs"
                v-on="on"
              >
                ShEx
                <v-icon>mdi-code-tags-check</v-icon>
              </v-tab>
            </template>
            <span>Specify constraints in ShEx to validate the RDF graph.</span>
          </v-tooltip>
        </v-tabs>

        <!-- Tab Contents -->
        <!-- Sparql -->
        <v-tabs-items v-model="tools_tab">
          <v-tab-item value="tab-sparql">
            <v-card flat tile class="mb-0">
              <v-card-text>
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
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    class="ma-2"
                    color="primary"
                    dark
                    v-bind="attrs"
                    v-on="on"
                    @click.prevent="sparqlFieldReset"
                  >
                    Reset
                  </v-btn>
                </template>
                <span
                  >Returns the SPARQL Query field to the default value.</span
                >
              </v-tooltip>
              <v-switch
                v-if="!sparql_alert"
                v-model="graph_text_disabled"
                :label="
                  `SPARQL Query or Update to TDB (disables graph editing and other tools)`
                "
                v-on:change="sparqlClearSelectedFormatAndGetTdb"
              ></v-switch>
              <v-alert
                v-model="sparql_alert"
                type="error"
                close-text="Close Alert"
                dismissible
              >
                {{ this.sparql_alert_msg }}
              </v-alert>
            </v-row>
          </v-tab-item>

          <!-- Owl -->
          <v-tab-item value="tab-owl">
            <v-card flat tile class="mb-0">
              <v-card-text>
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
                {{ this.owl_alert_msg }}
              </v-alert>
            </v-row>
          </v-tab-item>

          <!-- SHACL -->
          <v-tab-item value="tab-shacl">
            <v-card flat tile class="mb-0">
              <v-card-text>
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
                {{ this.shacl_alert_msg }}
              </v-alert>
            </v-row>
          </v-tab-item>

          <!-- ShEx -->
          <v-tab-item value="tab-shex">
            <v-card flat tile class="mb-0">
              <v-card-text>
                <v-lined-textarea
                  class="mb-0 monospaced-area"
                  rows="8"
                  v-model="shex_text_shape"
                  counter
                  name="shex_text"
                  label="Write your shape rules here"
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
                  {{ this.shex_alert_msg }}
                </v-alert>
              </v-col>
              <v-col
                v-if="!shex_alert"
                cols="4"
                class="mt-0 ml-0 d-none d-lg-flex d-xl-flex d-md-none d-xs-none"
              >
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
              <v-col
                v-if="!shex_alert"
                cols="12"
                lg="8"
                xl="8"
                md="12"
                sm="8"
                xs="12"
                class="mt-0 pb-0 pt-0 pl-0"
              >
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
              <v-col
                v-if="!shex_alert"
                cols="12"
                md="4"
                sm="4"
                class="mt-0 pt-0 ml-0 d-flex d-lg-none d-xl-none d-md-flex d-xs-flex"
              >
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
          icons-and-text
        >
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
                      <v-icon x-large>{{ this.result_icon }}</v-icon>
                      {{ this.result_origin }}
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="10">
                  <v-card min-height="250">
                    <v-card-title>
                      Result
                    </v-card-title>
                    <v-card-text class="text-left" ref="result_text">
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
                      <v-btn
                        :disabled="!copyResultEnabled"
                        v-on:click="resultToClipboard"
                      >
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
      <v-footer absolute color="primary" padless>
        <v-row justify="center" no-gutters>
          <v-dialog v-model="about_dialog" persistent max-width="390">
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
              <v-card-text
                >About will modify all text fields in order to display to you a
                working example. Be sure to save your content before
                continuing.</v-card-text
              >
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

          <v-dialog v-model="demo_dialog" persistent max-width="1290">
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
                <v-icon class="mr-1">mdi-youtube</v-icon> Demo
              </v-btn>
            </template>
            <v-card>
              <v-card-title class="headline"> </v-card-title>
              <v-card-text>
                <div>
                  <iframe
                    class="v-responsive"
                    width="1240"
                    height="698"
                    src="https://www.youtube.com/embed/g_Zdh_y-8Vc"
                    frameborder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowfullscreen
                  >
                  </iframe>
                </div>
              </v-card-text>
              <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn
                  color="primary darken-1"
                  text
                  @click="demo_dialog = false"
                >
                  Close
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>

          <a href="https://github.com/BastyZ/RDFPlayground" target="_blank">
            <v-btn
              class="ma-1 mb-0 lighten-4 left white--text"
              rounded
              outlined
              dark
              color="white"
              text
            >
              View on <v-icon class="ml-1">mdi-github</v-icon>
            </v-btn>
          </a>

          <v-col class="primary py-2 text-center white--text" cols="12">
            RDF Playground © {{ new Date().getFullYear() }} —
            <strong>Bastián Inostroza</strong>
          </v-col>
        </v-row>
      </v-footer>
    </v-row>
  </v-container>
</template>

<style lang="css" scoped>
.monospaced-area {
  font-family: monospace;
  line-height: 1.3rem !important;
}
</style>

<script src="./HelloWorld.js"></script>
