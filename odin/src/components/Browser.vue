<template>
  <v-container>
    <v-row class="ma-0">
      <v-text-field
          v-model="url"
          label="Insert an URL here">
      </v-text-field>
    </v-row>

    <v-row class="mt-2">
      <v-col
          class="text-left"
        >
        <v-btn
            color="primary"
            v-if="network_built"
            @click="browse_vis_dialog = true"
        >
          See Graph
        </v-btn>
      </v-col>
      <v-col class="text-right">
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
    </v-row>
    <v-row class="md-0">
      <v-col md="12">
        <v-alert
            v-model="browse_error_dialog"
            dense
            text
            dismissible
            prominent
            type="error"
        >
          {{ this.browse_error_text }}
        </v-alert>
      </v-col>
    </v-row>

    <v-row class="ma-2 mt-0 mb-10">
      <v-dialog
          v-model="browse_vis_dialog"
          eager
          fullscreen
          hide-overlay
          transition="dialog-bottom-transition"
      >
        <v-card>
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
            <!--<v-btn
                @click="resetNetwork"
            >
              Reset
            </v-btn>-->
          </v-toolbar>
          <v-row class="ma-2 mt-0 mb-10">
            <v-col cols="3">
              <v-treeview
                  v-model="selection"
                  :items="selectable_properties"
                  selectable
                  selection-type="leaf"
                  dense
                  return-object
                  v-bind:style="styleBrowserObject.sideProperties"
                  class="overflow-y-auto"
              ></v-treeview>
            </v-col>
            <v-col cols="9">
              <div ref="browseNetworkGraph"
                   v-bind:style="styleBrowserObject.dotFullscreenContainer">
              </div>
            </v-col>
          </v-row>
        </v-card>
      </v-dialog>
      <v-dialog
          v-model="extend_network_dialog"
          width="500"
          height="300"
          persistent
          transition="scale-transition"
      >
        <v-card>
          <v-col class="text-right">
              <v-icon @click="extend_network_dialog = false">mdi-close</v-icon>
          </v-col>
          <v-card-text>
              <div class="text-left">
                You're about to attempt to download a document with name <b>{{url_from_network}}</b>, and extend the current network.
              </div>

          </v-card-text>
          <v-card-actions>
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

const backAPI = "localhost:9060";
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
    loader: null,
    parsed_data: {},
    network: {},
    network_built: false,
    search_loading: false,
    selectable_properties: [],
    selection: [],
    url: "",
    url_from_network: "",
    styleBrowserObject: {
      dotContainer: {
        flex: 1,
        width: '100%',
        height: '418px',
        border: '1px solid #d3d3d3'
      },

      dotFullscreenContainer: {
        flex: 1,
        width: '100%',
        height: '840px',
        border: '1px solid #d3d3d3',
      },

      sideProperties: {
        height: '840px',
        border: '1px solid #d3d3d3'
      },
    }
  }),

  methods: {
    searchIri: function () {
      // Sends URL from input to API, and creates a graph network with the response
      let requestBody;

      if (!this.network_built || !this.browse_vis_dialog){
        // Restart variables
        this.browse_error_dialog = false;
        this.parsed_data = {};
        this.search_loading = true;

        // Restart network
        if (this.network_built) {
          browseNetwork.destroy();
          this.network_built = false;
        }

        // Validate URL through REGEX

        if(!this.validURL(this.url)) {
          this.browse_error_text = "Not a valid URL. Try this instead \n" +
              "---> http://dbpedia.org/resource/University_of_Chile <---";
          this.browse_error_dialog = true;
          this.search_loading = false;
          return
        }

        // Package URL as JSON
        requestBody = { url: this.url.toString() };
      }
      if (this.network_built && this.browse_vis_dialog) {
        requestBody = { url: this.url_from_network.toString() };
      }

      console.log("uri safe")
      console.log(requestBody)
      // Send URL to API
      fetch('http://' + backAPI + '/api/model/browse', {
        method: 'POST',
        headers: new Headers({
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify(requestBody),
      })
          .then(response => {
            if (!response.ok) {
              // If response's not ok, then pop up the error
              this.browse_error_dialog = true;
              this.browse_error_text = response.status;
              this.search_loading = false;
              console.log('Response not 200, instead we got ' + response.status);
            }
            else {
              console.log("Responses inc");
              response.json().then(content => {
                if (content.model_dot === "" && content.browse_error !== "") {
                  // Check if model is empty and display the error from the API
                  this.browse_error_text = content.browse_error;
                  this.browse_error_dialog = true;
                  this.search_loading = false;
                }
                else {
                  this.dot_data = content.model_dot
                  try {
                    if (this.network_built && this.browse_vis_dialog) {
                      console.log("extend_network")
                      this.extendNetwork()
                    }
                    if (!this.network_built || !this.browse_vis_dialog) {
                      console.log("build network")
                      this.buildNetwork()
                    }
                  }
                  catch (e) {
                    this.browse_error_text = e;
                    this.browse_error_dialog = true;
                    this.search_loading = false;
                  }
                }
              });
            }
          })
          .catch(error => {
            this.browse_error_text = error;
            this.browse_error_dialog = true;
          })
    },

    buildNetwork: function() {
      // Set the graph container
      const refContainer = this.$refs.browseNetworkGraph;
      this.parsed_data = parseDOTNetwork(this.dot_data);

      this.central_node = this.centralNode(this.parsed_data.nodes);

      let current_nodes = [this.central_node];
      let current_edges = this.parsed_data.edges;

      current_edges.sort((a,b) => a.label.localeCompare(b.label));
      this.selectable_properties = this.sidebarProperties(current_edges);

      this.network = {'nodes': current_nodes, 'edges': []};
      browseNetwork = new Network(refContainer, this.network, options);

      this.browse_vis_dialog = true;
      browseNetwork.on("doubleClick", this.doubleClickCallback)

      this.search_loading = false;
      this.network_built = true;
    },

    doubleClickCallback: function(params) {
      if (params.nodes.length !== 0) {
        this.url_from_network = params.nodes[0];
        this.extend_network_dialog = true;
      }
    },

    extendNetwork: function() {
      console.log(this.parsed_data.edges.length, "pd edges length")
      console.log(this.parsed_data.nodes.length, "pd nodes length")

      let new_parsed_data = parseDOTNetwork(this.dot_data);

      let new_nodes = [... new Set([...this.parsed_data.nodes, ...new_parsed_data.nodes])]
      let new_edges = [... new Set([...this.parsed_data.edges, ...new_parsed_data.edges])]

      console.log(new_edges.length, "new set edges length")
      console.log(new_nodes.length, "new set nodes length")

      this.parsed_data.edges = this.getUniqueItemsByProperties(new_edges, ['to', 'label', 'from'])
      console.log(this.parsed_data.edges.length, "new parsed data edges length")
      this.parsed_data.nodes = this.getUniqueItemsByProperties(new_nodes, ['id', 'label', 'shape'])
      console.log(this.parsed_data.nodes.length, "new parsed data nodes length")


      this.extend_network_dialog = false;
    },


    centralNode: function(urlNodes) {
      const candidates = urlNodes.filter((node) => node.id.includes(this.url.toString()))
      return candidates.reduce((node1, node2) => node1.id.length <= node2.id.length ? node1 : node2)
    },

    sidebarProperties: function(edges) {
      let sidebar_p = []
      let i = 1
      edges.forEach(edge => {
        let new_edge = edge;

        let property ={"id":i ,"name": edge.label, "edges": [new_edge]};
        let added_property = false

        sidebar_p.forEach(prop => {
          if (prop.name === new_edge.label) {
            prop.edges.push(new_edge)
            added_property = true
          }
        })
        if (!added_property) {
          sidebar_p.push(property)
        }
        i += 1
      })
      sidebar_p.forEach(prop => {
        prop.name = prop.name + " (" + prop.edges.length.toString() + ")"
      })
      return sidebar_p
    },

    /*apacheNodes: function(nodes) {
      return nodes.filter((node) => node.id.includes('org.apache'))
    },
    apacheEdges: function(edges) {
      return edges.filter((edge) => edge.to.includes('org.apache') || edge.from.includes('org.apache'))
    },*/

    updateNetwork: function(added_edges, removed_edges) {
      if (added_edges.length !== 0) {
        console.log("adding edges and nodes")
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
        console.log("removing edges and nodes")
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
      console.log(this.network)
      browseNetwork.setData(this.network)
      browseNetwork.redraw()
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

    validURL: function(str) {
      let pattern = new RegExp('^(https?:\\/\\/)?'+ // protocol
      '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|'+ // domain name
      '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
      '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // port and path
      '(\\?[;&a-z\\d%_.~+=-]*)?'+ // query string
      '(\\#[-a-z\\d_]*)?$','i'); // fragment locator
      return !!pattern.test(str);
    }
  },

  watch: {
    loader() {
      const l = this.loader
      this[l] = !this[l]
      setTimeout(() => (this.search_loading = false), 1000)

      this.loader = null
    },

    selection: {
      handler(val, oldVal) {
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
