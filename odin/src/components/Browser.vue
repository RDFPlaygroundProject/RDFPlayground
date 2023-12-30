<template>
  <v-container>
    <!-- Left-side tab -->
    <!-- Input field -->
    <v-row class="ma-0">
      <v-text-field
          v-model="url"
          label="Insert an URL here">
      </v-text-field>
    </v-row>
    <!-- Search and See Graph buttons -->
    <v-row class="mt-0">
      <v-col class="text-left">
        <v-btn
            color="primary"
            dense
            :loading="search_loading"
            :disabled="search_loading"
            @click="searchIri"
        >
          Search
          <template v-slot:loader>
            <span class="custom-loader">
              <v-icon light>mdi-cached</v-icon>
            </span>
          </template>
        </v-btn>
      </v-col>
      <v-col class="text-right">
        <v-btn
            color="green darken-3"
            class="white--text"
            v-if="network_built"
            @click="browse_vis_dialog = true"
        >
          See Graph
        </v-btn>
      </v-col>
    </v-row>
    <!-- Examples panel -->
    <v-row class="md-0">
      <v-col>
        <v-expansion-panels tile>
          <v-expansion-panel>
            <v-expansion-panel-header color="green darken-3" class="white--text">
              URL Examples
            </v-expansion-panel-header>
            <v-expansion-panel-content class="mt-4 text-justify">
              <ul>
                <li>http://dbpedia.org/resource/Isaac_Asimov</li>
                <li>https://sws.geonames.org/1668284</li>
                <li>http://www.wikidata.org/entity/Q466</li>
              </ul>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-col>
    </v-row>
    <!-- Error dialog -->
    <v-row class="md-0">
      <v-col md="12">
        <v-alert
            v-model="browse_error_dialog"
            dense
            dismissible
            type="error"
        >
          {{ this.browse_error_text }}
        </v-alert>
      </v-col>
    </v-row>

    <!-- Graph Visualization dialog -->
    <v-row class="ma-2 mt-0 mb-10">
      <v-dialog
          v-model="browse_vis_dialog"
          eager
          fullscreen
          hide-overlay
          transition="dialog-bottom-transition"
      >
        <v-card>
          <!-- Toolbar -->
          <v-toolbar
              dark
              color="primary"
          >
            <v-btn
                icon
                dark
                @click="browse_vis_dialog = false"
            >
              <v-icon>mdi-close</v-icon>
            </v-btn>
            <v-toolbar-title>Graph Visualization</v-toolbar-title>
            <v-spacer></v-spacer>
            <v-btn
              dark
              rounded
              color="green darken-3"
              @click="ttl_data_dialog = !ttl_data_dialog"
            >
              <span v-if="ttl_data_dialog">Graph Visualization</span>
              <span v-else>Turtle Document</span>
            </v-btn>
            <v-tooltip bottom>
              <template v-slot:activator="{on, attrs}">
                <v-btn
                    icon
                    @click="help_dialog = true"
                    class="ml-3"
                    v-bind="attrs"
                    v-on="on"
                >
                  <v-icon>mdi-information-outline</v-icon>
                </v-btn>
              </template>
              <span>Important notes</span>
            </v-tooltip>
            <v-dialog
                v-model="help_dialog"
                max-width="700px"
            >
              <v-card
                  dark
                  tile>
                <v-toolbar>
                  <v-toolbar-title>Important Notes</v-toolbar-title>
                  <v-spacer></v-spacer>
                  <v-btn
                      icon
                      @click="help_dialog = false"
                  >
                    <v-icon>mdi-close</v-icon>
                  </v-btn>
                </v-toolbar>
                <v-card-text>
                  <v-list
                      three-line
                      disabled
                  >
                    <v-list-item-group>
                      <v-list-item
                          v-for="(note, i) in notes"
                          :key="i"
                      >
                        <v-list-item-icon>
                          <v-icon v-text="note.icon"></v-icon>
                        </v-list-item-icon>
                        <v-list-item-content>
                          <v-list-item-title v-text="note.text"></v-list-item-title>
                          <v-list-item-subtitle v-text="note.subtext"></v-list-item-subtitle>
                        </v-list-item-content>
                      </v-list-item>

                    </v-list-item-group>
                  </v-list>
                </v-card-text>
              </v-card>
            </v-dialog>
          </v-toolbar>

          <v-row class="ma-2 mt-0 mb-10 ml-0">
            <!-- Documents and Properties sidebar -->
            <v-col cols="4">
              <v-toolbar
                  dense
                  dark
              >
                <v-toolbar-title>Documents and Properties</v-toolbar-title>
              </v-toolbar>
              <v-treeview
                  class="overflow-auto"
                  v-model="selection"
                  v-bind:style="styleBrowserObject.sideProperties"
                  :items="selectable_properties"
                  selectable
                  activatable
                  open-on-click
                  dense
                  return-object
              ></v-treeview>
            </v-col>
            <!-- Document Visualization (graph and text) -->
            <v-col cols="8">
              <div ref="browseNetworkGraph"
                   v-bind:style="styleBrowserObject.dotFullscreenContainer"
                   v-show="!ttl_data_dialog"
              >
              </div>
              <div v-show="ttl_data_dialog">
                <v-textarea
                    :value="ttl_data"
                    readonly
                    outlined
                    shaped
                    no-resize
                    dense
                    label="Documents in turtle format"
                    rows="29"
                ></v-textarea>
              </div>
            </v-col>
          </v-row>
        </v-card>
      </v-dialog>
      <!-- Extend network dialog -->
      <v-dialog
          v-model="extend_network_dialog"
          width="700"
          height="300"
          persistent
          transition="scale-transition"
      >
        <v-card tile>
          <v-card-title>
            Extend Network
          </v-card-title>
          <v-card-text>
              <div class="text-justify">
                <p>You're about to download the following document, and extend the current network:</p>
                <a v-bind:href=url_from_network target="_blank">
                  {{url_from_network}}
                </a>
                <p></p>
                <p>Press the Start button to continue.</p>
              </div>
              <v-alert
                v-model="extend_network_error"
                dense
                text
                dismissible
                type="error"
              >
                {{ this.browse_error_text }}
              </v-alert>
              <v-alert
                v-model="extend_network_succeed"
                dense
                text
                dismissible
                type="success"
              >
                The document has been successfully retrieved and added to the Document and Properties List
              </v-alert>

          </v-card-text>
          <v-card-actions>
            <v-col class="text-left">
              <v-btn
                  dark
                  @click="extend_network_dialog = false"
              >
                  Return
              </v-btn>
            </v-col>
            <v-col class="text-right">
              <v-btn
                  color="primary"
                  :loading="search_loading"
                  :disabled="search_loading"
                  @click="searchIri"
              >
                Start
                <template v-slot:loader>
                  <span class="custom-loader">
                    <v-icon light>mdi-cached</v-icon>
                  </span>
                </template>
              </v-btn>
            </v-col>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-row>
  </v-container>
</template>

<script>


import {Network} from 'vis-network';
import {parseDOTNetwork} from 'vis-network';
// import vis from 'vis';

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
  interaction: {
    hover: true
  },
  physics: {
    barnesHut: {
      gravitationalConstant: -4000
    },
  }
};

// eslint-disable-next-line no-unused-vars
let browseNetwork = null;

export default {

  name: "Browser",
  data: () => ({
    browse_error_text: "",
    browse_error_dialog: false,
    browse_vis_dialog: false,
    central_node: {},
    dot_data: "",
    extend_network_dialog: false,
    extend_network_error: false,
    extend_network_succeed: false,
    help_dialog: false,
    first_time_help: true,
    loader: null,
    parsed_data: {},
    network: {},
    network_built: false,
    search_loading: false,
    searched_documents: [],
    selectable_properties: [],
    selection: [],
    sidebar_id: 1,
    ttl_data: "",
    ttl_data_dialog: false,
    url: "",
    url_from_network: "",
    notes: [
      { text: "Double click interaction on nodes",
        subtext: "This event will popup a dialog where you can extend the " +
            "network. Only URL-like nodes will trigger the dialog.",
        icon:"mdi-gesture-double-tap"
      },
      { text: "Mouseover nodes and edges on graph",
        subtext: "This will show more information about the element." +
            " Edges have their document name, and nodes their full name," +
            " or description, language and datatype if available, if it's a literal.",
        icon:"mdi-information-variant"
      },
      { text: "Documents and Properties sidebar",
        subtext: "You can select properties individually or the entire document," +
            " but beware of selecting large documents (> 1000 properties);" +
            " it'll freeze your browser for a long time.",
        icon:"mdi-format-list-bulleted"
      },

    ],
    styleBrowserObject: {
      dotFullscreenContainer: {
        flex: 1,
        width: '100%',
        height: '840px',
        border: '1px solid #d3d3d3',
      },

      sideProperties: {
        flex: 1,
        height: '792px',
        border: '1px solid #d3d3d3'
      }
    }
  }),

  methods: {
    searchIri: function () {
      // Sends URL from input to API, and creates a graph network with the response
      // Works to create the graph, and to extend it
      let requestBody;
      this.search_loading = true;

      // First search case
      if (!this.extend_network_dialog){
        // Restart variables
        this.browse_error_dialog = false;
        this.central_node = {};
        this.parsed_data = {};
        this.searched_documents = [];
        this.selectable_properties = [];
        //this.selection = [];
        this.dot_data = "";
        this.ttl_data = "";
        this.sidebar_id = 1;

        if (this.network_built) {
          browseNetwork.destroy();
          this.network_built = false;
        }

        // Validate URL through REGEX
       /* if(!this.validURL(this.url)) {
          this.browse_error_text = "Not a valid URL. Try this instead \n" +
              "---> http://dbpedia.org/resource/University_of_Chile <---";
          this.browse_error_dialog = true;
          this.search_loading = false;
          return
        }*/

        requestBody = { url: this.url.toString() };
      }
      // Extend Network case
      else {
        if (this.searched_documents.includes(this.url_from_network)) {
          this.browse_error_text = "The document from this URL its already on the graph";
          this.extend_network_error = true;
          this.search_loading = false
          return;
        }
        requestBody = { url: this.url_from_network.toString() };
      }

      // Send URL to API
      fetch('http://localhost:9060/api/model/browse', {
        method: 'POST',
        headers: new Headers({
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify(requestBody),
      })
          .then(response => {
            if (!response.ok) {
              // If response's not ok, then pop up the error
              if (!this.extend_network_dialog) this.browse_error_dialog = true;
              else this.extend_network_error = true;

              this.search_loading = false;
              response.json().then(content => {
                this.browse_error_text = 'Response not 200, instead we got ' + response.status + '. Error: '
                    + content.browse_error
              })

            }
            else {
              response.json().then(content => {
                if (content.model_dot === "" && content.browse_error !== "") {
                  // Check if model is empty and display the error from the API
                  if (!this.extend_network_dialog) this.browse_error_dialog = true;
                  else this.extend_network_error = true;

                  this.browse_error_text = content.browse_error;
                  this.search_loading = false;
                }
                else {
                  this.dot_data = content.model_dot
                  try {
                    // Build Network for the 1st time
                    if (!this.extend_network_dialog) {
                      this.ttl_data = content.model_ttl
                      this.buildNetwork()
                      this.search_loading = false;
                      this.network_built = true;
                      this.browse_vis_dialog = true;
                      if (this.first_time_help) {
                        this.help_dialog = true;
                        this.first_time_help = false;
                      }
                    }
                    // Extend the network
                    else {
                      this.mergeTTL(this.ttl_data, content.model_ttl)
                      this.extendNetwork()
                      this.search_loading = false;
                      this.extend_network_succeed = true;
                    }
                  }
                  catch (e) {
                    if (!this.extend_network_dialog) this.browse_error_dialog = true;
                    else this.extend_network_error = true;
                    this.browse_error_text = e;
                    this.search_loading = false;
                  }
                }
              });
            }
          })
          .catch(error => {
            if (!this.extend_network_dialog) this.browse_error_dialog = true;
            else this.extend_network_error = true;
            this.browse_error_text = error;
            this.browse_error_dialog = true;
            this.search_loading = false;
          })
    },

    buildNetwork: function() {
      // Set the graph container
      const refContainer = this.$refs.browseNetworkGraph;
      this.parsed_data = parseDOTNetwork(this.dot_data);

      // Find the central node
      this.central_node = this.centralNode(this.parsed_data.nodes);
      let current_nodes = [this.central_node];
      let current_edges = this.parsed_data.edges;

      // Sort edges, add hover info, and create sidebar tree-view
      current_edges.sort((a,b) => a.label.localeCompare(b.label));
      this.addNodeTitle(this.parsed_data.nodes);
      this.addEdgeDocument(this.parsed_data.edges, this.central_node.id);
      this.sidebarProperties(current_edges, this.central_node.id);

      // Build network and add "doubleClick event"
      this.network = {nodes: current_nodes, edges: []};
      browseNetwork = new Network(refContainer, this.network, options);
      browseNetwork.on("doubleClick", this.doubleClickCallback)
      this.searched_documents.push(this.central_node.id)
    },

    doubleClickCallback: function(params) {
      // Trigger to open Extend Network dialog
      if (params.nodes.length !== 0) {
        let node = this.parsed_data.nodes.find(o => o.id === params.nodes[0])
        if (node.shape === "ellipse") {
          this.url_from_network = params.nodes[0];
          this.extend_network_error = false;
          this.extend_network_dialog = true;
        }
      }
    },

    extendNetwork: function() {
      // Parse new data, merge it with current data and de-duplicate
      let new_parsed_data = parseDOTNetwork(this.dot_data);
      this.addEdgeDocument(new_parsed_data.edges, this.url_from_network)
      this.addNodeTitle(new_parsed_data.nodes);
      new_parsed_data.edges.sort((a,b) => a.label.localeCompare(b.label));

      let new_nodes = [... new Set([...this.parsed_data.nodes, ...new_parsed_data.nodes])]
      let new_edges = [... new Set([...this.parsed_data.edges, ...new_parsed_data.edges])]

      this.parsed_data.edges = this.getUniqueItemsByProperties(new_edges, ['to', 'label', 'from'])
      this.parsed_data.nodes = this.getUniqueItemsByProperties(new_nodes, ['id', 'label', 'shape'])

      this.sidebarProperties(new_parsed_data.edges, this.url_from_network)
      this.searched_documents.push(this.url_from_network)
    },

    centralNode: function(urlNodes) {
      // Search for the node which id is equal or most similar to the URI input
      const candidates = urlNodes.filter((node) => node.id.includes(this.url.toString()))
      return candidates.reduce((node1, node2) => node1.id.length <= node2.id.length ? node1 : node2)
    },

    sidebarProperties: function(edges, doc_title) {
      // Creates a structure to handle documents and properties sidebar
      let truncate_title = doc_title.substring(doc_title.lastIndexOf("://")+3)
      let document_container = {"id": this.sidebar_id, "name": truncate_title, "children": []}
      this.selectable_properties.push(document_container)
      let document_index = (this.selectable_properties.length - 1)
      this.sidebar_id += 1

      edges.forEach(edge => {
        let property ={"id":this.sidebar_id ,"name": edge.label, "edges": [edge]};
        let added_property = false

        this.selectable_properties[document_index].children.forEach(prop => {
          if (prop.name === edge.label) {
            prop.edges.push(edge)
            added_property = true
          }
        })
        if (!added_property) {
          this.selectable_properties[document_index].children.push(property)
        }
        this.sidebar_id += 1
      })
      this.selectable_properties[document_index].children.forEach(prop => {
        prop.name = prop.name + " (" + prop.edges.length.toString() + ")"
      })
    },

    addNodeTitle: function(nodes) {
      // Adds title to nodes, in order to handle large literals for mousehover interaction
      nodes.forEach(node => {
        if (!('title' in node)){
          if (node.shape === "ellipse"){
            if (node.id.length > 60) {
              node.label = node.label.substring(0, 30) + "..."
            }
            node.title = node.id
          }
          if (node.shape === "record") {
            let title = ""
            if (node.id.length > 60) {
              title += node.id
              node.label = node.label.substring(0, 30) + "..."
            }
            if ('lang' in node){
              title += "\nlang:" + node.lang
            }
            if ('datatype' in node){
              title += "\ndatatype:" + node.datatype
            }
            if (title.length !== 0) {
              node.title = this.prettyTitle(title)
            }
          }
        }
      })
    },

    addEdgeDocument: function(edges, title) {
      // Add document name to Edges for mousehover interaction
      edges.forEach(edge => {
        if (!('title' in edge)) {
          edge.title = "doc: " + title
        }
      })
    },

    prettyTitle: function(title) {
      // Prettify large literals
      const container = document.createElement("div")
      container.innerText = title
          .match(/.{1,128}(\s|$)/g)
          .map(p => p + "\n")
          .join('')
      return container
    },

    updateNetwork: function(added_edges, removed_edges) {
      // Handler for graph interaction. Updates the graph with new nodes and edges, or removes them
      if (added_edges.length !== 0) {
        added_edges.forEach(prop => {
          prop.edges.forEach(edge => {
            this.network.edges.push(edge)

            let to_node = edge.to
            let from_node = edge.from
            if (!this.network.nodes.some(net_node => net_node.id === to_node)){
              this.network.nodes.push(this.parsed_data.nodes.find(parsed_node => parsed_node.id === to_node))
            }
            if (!this.network.nodes.some(net_node => net_node.id === from_node)){
              this.network.nodes.push(this.parsed_data.nodes.find(parsed_node => parsed_node.id === from_node))
            }
          })
        })
      }
      if (removed_edges.length !== 0) {
        removed_edges.forEach(prop => {
          prop.edges.forEach(edge => {
            this.network.edges = this.network.edges.filter(net_edge => net_edge.id !== edge.id)

            let to_node = edge.to
            let from_node = edge.from
            if (!this.network.edges.some(net_edge => net_edge.to === to_node || net_edge.from === to_node)){
              if (to_node !== this.central_node.id) {
                this.network.nodes = this.network.nodes.filter(net_node => net_node.id !== to_node)
              }
            }
            if (!this.network.edges.some(net_edge => net_edge.to === from_node || net_edge.from === from_node)){
              if (from_node !== this.central_node.id) {
                this.network.nodes = this.network.nodes.filter(net_node => net_node.id !== from_node)
              }
            }
          })
        })
      }

      if (this.network_built){
        browseNetwork.setData(this.network)
        browseNetwork.redraw()
      }
    },

    mergeTTL: function (ttl1,ttl2) {
      // Merge the TTL documents whenever the number of documents increase.
      let prefixes = [];
      let file_1 = ttl1.split("\n");
      let file_2 = ttl2.split("\n");

      prefixes = prefixes.concat(file_1.filter( line => line.includes("@prefix")))
          .concat(file_2.filter( line => line.includes("@prefix")))

      let unique_prefixes = prefixes.filter((value, index, self) => self.indexOf(value) === index)

      let data_1 = file_1.filter(line => !line.includes("@prefix"))
      let data_2 = file_2.filter(line => !line.includes("@prefix"))
      this.ttl_data = unique_prefixes.concat(data_1.concat(data_2)).join("\n")
    },

    isPropValuesEqual: function(subject, target, propNames) {
      return propNames.every(propName => subject[propName] === target[propName]);
    },

    getUniqueItemsByProperties: function(items, propNames) {
      const propNamesArray = Array.from(propNames);

      return items.filter((item, index, array) =>
          index === array.findIndex(foundItem => this.isPropValuesEqual(foundItem, item, propNamesArray))
      );
    },

    /*validURL: function(str) {
      let pattern = new RegExp('^(https?:\\/\\/)?'+ // protocol
      '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|'+ // domain name
      '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
      '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // port and path
      '(\\?[;&a-z\\d%_.~+=-]*)?'+ // query string
      '(\\#[-a-z\\d_]*)?$','i'); // fragment locator
      return !!pattern.test(str);
    }*/
  },

  watch: {
    loader() {
      // Loader icon
      const l = this.loader
      this[l] = !this[l]
      setTimeout(() => (this.search_loading = false), 1000)

      this.loader = null
    },

    selection: {
      handler(val, oldVal) {
        // Documents and properties handler
        let new_edges = val.filter(edges => !oldVal.includes(edges))
        let removed_edges = oldVal.filter (edges => !val.includes(edges))
        this.updateNetwork(new_edges, removed_edges)
      }
    },

  }
}

</script>

<style>
.custom-loader {
  animation: loader 1s infinite;
  display: flex;
}
@-moz-keyframes loader {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
@-webkit-keyframes loader {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
@-o-keyframes loader {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
@keyframes loader {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}

</style>
