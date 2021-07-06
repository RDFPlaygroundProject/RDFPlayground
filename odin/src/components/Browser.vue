<template>
  <v-container>
    <v-row class="ma-0">
      <input v-model="url"
             v-bind:style="styleBrowserObject.iriInput"
             placeholder="Insert an URL  here">
    </v-row>

    <v-row class="ma-0 mb-2">
      <v-col md="10">
        <v-alert
            v-model="browse_error_dialog"
            dense
            dismissible
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
              Implement manipulation of the graph here
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
    browse_dot_vis: "",
    browse_error_text: "",
    browse_error_dialog: false,
    browse_vis_dialog: false,
    central_node: "",
    search_loading: false,
    loader: null,
    nodeList: [],
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
      this.browse_error_dialog = false

      if (!this.url) {
        this.browse_error_text = "No URL to search for. Try this! \n" +
            "---> http://dbpedia.org/resource/University_of_Chile <---"
        this.browse_error_dialog = true
        return
      }

      let requestBody = { url: this.url.toString() };
      this.search_loading = true
      console.log("uri safe")

      if (browseNetwork !== null) {
        this.resetNetwork();
      }

      fetch('http://' + backAPI + '/api/model/browse', {
        method: 'POST',
        headers: new Headers({
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify(requestBody),
      })
          .then(response => {
            if (!response.ok) {
              this.browse_error_text = response.status
              this.search_loading = false
              console.log('Response not 200, instead we got ' + response.status);
            }
            else {
              console.log("Responses inc")
              response.json().then(content => {
                this.browse_dot_vis = content.model_dot;
                this.browse_error_text = content.browse_error;

                if (content.model_dot === "" && content.browse_error !== "") {
                  this.browse_error_dialog = true
                  this.search_loading = false
                }
                else {
                  try {
                    const refContainer = this.$refs.browseNetworkGraph;
                    let parsedData = parseDOTNetwork(content.model_dot);
                    let ttlData = content.model_ttl
                    console.log(ttlData)

                    this.browse_vis_dialog = true;
                    this.search_loading = false

                    let central_node = this.centralNode(parsedData.nodes)
                    let edgesFN = this.nodeEdges(parsedData.edges, central_node)
                    // let nfe = this.nodesFromEdges(edgesFN, parsedData.nodes, false)

                    let net = this.buildNetworkData(central_node, edgesFN)
                    console.log(this.apacheNodes(parsedData.nodes))
                    console.log(this.apacheEdges(parsedData.edges))

                    browseNetwork = new Network(refContainer, net, options);

                  } catch (e) {
                    this.browse_error_text = e
                    console.log(e)
                  }
                }
              });
            }
          })
          .catch(error => {
            this.browse_error_text = error
            console.log(error)
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

    apacheNodes: function(nodes) {
      return nodes.filter((node) => node.id.includes('org.apache'))
    },
    apacheEdges: function(edges) {
      return edges.filter((edge) => edge.to.includes('org.apache') || edge.from.includes('org.apache'))
    },
/*
    edgesToNode: function(edges, node) {
      return edges.filter((edgeNode) => edgeNode.to === node.id)
    },*/

    nodesFromEdges: function(edges, nodes, from_to) {
      if (!from_to){
        const fromNodesID = edges.map(
            (edge) => edge.to
        )
        return nodes.filter(
            (node) => fromNodesID.includes(node.id)
        )
      }


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
    }

  },

  watch: {
    loader() {
      const l = this.loader
      this[l] = !this[l]
      setTimeout(() => (this.search_loading = false), 10000)

      this.loader = null
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
