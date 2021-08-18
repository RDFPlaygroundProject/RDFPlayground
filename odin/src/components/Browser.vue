<template>
  <v-container>
    <v-row class="ma-0">
      <input v-model="url"
             v-bind:style="styleBrowserObject.iriInput"
             placeholder="Insert an URL  here">
    </v-row>

    <v-row class="mt-0">
      <v-col md="10">
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
      <v-col class="text-right" md="2">
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



    <v-row class="ma-2 mt-0 mb-10">
      <v-dialog
          v-model="browse_vis_dialog"
          eager
          fullscreen
          hide-overlay
          transition="dialog-bottom-transition"

      >
<!--        <template v-slot:activator="{ on, attrs }">
          <v-btn
              color="primary"
              dark
              v-bind="attrs"
              v-on="on"
          >
            <v-icon>mdi-fullscreen</v-icon> View on Fullscreen
          </v-btn>
        </template>-->
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
            <v-btn
                @click="resetNetwork"
            >
              Reset
            </v-btn>
          </v-toolbar>
          <v-row class="ma-2 mt-0 mb-10">
            <v-col cols="2">
              <v-btn
                  dark
                  @click="updateNetwork"
              >Add Selected Properties</v-btn>
              <v-treeview
                  v-model="selection"
                  :items="selectable_properties"
                  selectable
                  selection-type="leaf"
                  dense
                  return-object
                  style="max-height: 800px"
                  class="overflow-y-auto"
              ></v-treeview>
            </v-col>
            <v-col cols="10">
              <div ref="browseNetworkGraph"
                   v-bind:style="styleBrowserObject.dotFullscreenContainer">
              </div>
            </v-col>
          </v-row>
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
    url: "",
    browse_error_text: "",
    browse_error_dialog: false,
    browse_vis_dialog: false,
    central_node: "",
    search_loading: false,
    loader: null,
    parsedData: {},
    network: {},
    selectable_properties: [],
    selection: [],
    styleBrowserObject: {
      iriInput: {
        borderBottom: '1px solid lightgray',
        borderTop: '1px solid lightgray',
        paddingTop: '6px',
        paddingBottom: '6px',
        marginTop: '6px',
        width: '100%'
      },
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
    }
  }),

  methods: {
    searchIri: function () {
      // Sends URL from input to API, and creates a graph network with the response

      // Restart global variables
      this.browse_error_dialog = false;
      this.search_loading = true;
      this.parsedData = {};

      // Restart network
      if (browseNetwork !== null) {
        this.resetNetwork();
      }

      // Empty URL case
      if (!this.url) {
        this.browse_error_text = "No URL to search for. Try this! \n" +
            "---> http://dbpedia.org/resource/University_of_Chile <---";
        this.browse_error_dialog = true;
        this.search_loading = false;
        return
      }

      // Package URL as JSON
      let requestBody = { url: this.url.toString() };

      console.log("uri safe")
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
                  try {
                    // Set the graph container and parse the data
                    const refContainer = this.$refs.browseNetworkGraph;

                    this.parsedData = parseDOTNetwork(content.model_dot);
                    console.log(this.parsedData)

                    let current_nodes = this.centralNode(this.parsedData.nodes);
                    console.log(current_nodes)
                    this.central_node = current_nodes.id;
                    let current_edges = this.parsedData.edges

                    console.log(this.central_node)

                    current_edges.sort((a,b) => a.label.localeCompare(b.label));

                    this.selectable_properties = this.sidebarProperties(current_edges);

                    /*console.log(this.parsedData.edges)
                    console.log(this.current_edges)
                    console.log(this.selectable_properties)*/

                    this.network = this.buildNetworkData(current_nodes, []);

                    browseNetwork = new Network(refContainer, this.network, options);

                    this.browse_vis_dialog = true;
                    this.search_loading = false;
                  } catch (e) {
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

    resetNetwork: function () {
      browseNetwork.destroy()
    },

    centralNode: function(urlNodes) {
      const candidates = urlNodes.filter((node) => node.id.includes(this.url.toString()))
      return candidates.reduce((node1, node2) => node1.id.length <= node2.id.length ? node1 : node2)
    },

    nodeEdges: function(edges, node) {
      return edges.filter((edgeNode) => edgeNode.from === node.id || edgeNode.to === node.id)
    },

    sidebarProperties: function(edges) {
      let sidebar_p = []
      let i = 1
      edges.forEach(edge => {
        let new_edge = edge;
        new_edge.name = new_edge.label;

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

    apacheNodes: function(nodes) {
      return nodes.filter((node) => node.id.includes('org.apache'))
    },
    apacheEdges: function(edges) {
      return edges.filter((edge) => edge.to.includes('org.apache') || edge.from.includes('org.apache'))
    },



    buildNetworkData: function(nodes, edges) {
      if (!Array.isArray(nodes)) {
        return {nodes: [nodes], edges: edges}
      }
      if (!Array.isArray(edges)) {
        return {nodes: nodes, edges: [edges]}
      }
      if (!Array.isArray(nodes) && !Array.isArray(edges)) {
        return {nodes: [nodes], edges: [edges]}
      }
      return {nodes: nodes, edges: edges}
    },

    updateNetwork: function(added_edges, removed_edges) {
      if (added_edges.length !== 0) {
        console.log("got here")
        console.log(added_edges)
        added_edges.forEach(prop => {
          prop.edges.forEach(edge => {
            if (!this.network.edges.some(net_edge => edge.id === net_edge.id)){
              this.network.edges.push(edge)
            }
            let to_node = edge.to
            let from_node = edge.from
            if (!this.network.nodes.some(net_node => net_node.id === to_node)){
              this.network.nodes.push(this.parsedData.nodes.find(parsed_node => parsed_node.id === to_node))
            }
            if (!this.network.nodes.some(net_node => net_node.id === from_node)){
              this.network.nodes.push(this.parsedData.nodes.find(parsed_node => parsed_node.id === from_node))
            }
          })
        })
      }
      if (removed_edges.length !== 0) {
        console.log("got here to")
        removed_edges.forEach(prop => {
          prop.edges.forEach(edge => {
            if (this.network.edges.some(net_edge => net_edge.id === edge.id)) {
              this.network.edges = this.network.edges.filter(net_edge => net_edge.id !== edge.id)
            }
            let to_node = edge.to
            let from_node = edge.from
            if (!this.network.edges.some(net_edge => net_edge.to === to_node || net_edge.from === to_node)){
              if (to_node !== this.central_node) {
                this.network.nodes = this.network.nodes.filter(net_node => net_node.id !== to_node)
              }
            }
            if (!this.network.edges.some(net_edge => net_edge.to === from_node || net_edge.from === from_node)){
              if (from_node !== this.central_node) {
                this.network.nodes = this.network.nodes.filter(net_node => net_node.id !== from_node)
              }
            }
          })
        })
      }
      console.log(this.network)
      browseNetwork.setData(this.network)
      browseNetwork.redraw()
    }


  },

  watch: {
    loader() {
      const l = this.loader
      this[l] = !this[l]
      setTimeout(() => (this.search_loading = false), 10000)

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
