<template>
  <v-container>
    <v-row class="ma-1 mb-1">
      <input class="iri-input" v-model="url" placeholder="Enter your IRI here">
      <v-col class="text-right">
        <v-btn
            @click="searchIri"
        >
          Search
        </v-btn>
      </v-col>
    </v-row>

    <v-row class="ma-2 mb-1">
      <div id="browseNetwork" class="dot-container"></div>
    </v-row>
    <v-row class="ma-2 mt-0 mb-10">
      <v-dialog
          fullscreen
          hide-overlay
          transition="dialog-bottom-transition"
      >
        <template v-slot:activator="{ on, attrs }">
          <v-btn
              color="primary"
              dark
              v-bind="attrs"
              v-on="on"
          >
            <v-icon>mdi-fullscreen</v-icon> View on Fullscreen
          </v-btn>
        </template>
        <v-card>
          <v-toolbar
              dark
              color="primary"
          >
            <v-btn
                icon
                dark
            >
              <v-icon>mdi-close</v-icon>
            </v-btn>
            <v-toolbar-title>Graph Visualization</v-toolbar-title>
            <v-spacer></v-spacer>
            <v-toolbar-title class="hidden-sm-and-down">Save as image using secondary (right) click over the visualization</v-toolbar-title>
          </v-toolbar>
          <vis-network ref="browseNetwork"
                       class="dot-fullscreen-container"
                       :nodes="browse_dot_vis.nodes"
                       :edges="browse_dot_vis.edges"
          />
        </v-card>
      </v-dialog>
    </v-row>
  </v-container>
</template>

<script>

import {Network} from 'vis-network/peer';
import {parseDOTNetwork} from 'vis-network/peer';

const backAPI = "localhost:9060";

//const options = {}

  /*nodes: {
    font: {
      size: 6,
    },
  },
  edges: {
    length: 60,
    font: {
      size: 6,
    },
  },
};*/


// eslint-disable-next-line no-unused-vars
let browseNetwork = null;

export default {


  name: "Browser",
  data: () => ({
    url: "",
    browse_dot_vis: "",
  }),

  methods: {
    searchIri: function () {

      let requestBody = { url: this.url.toString() };
      console.log("Uri safe")

      fetch('http://' + backAPI + '/api/model/browse', {
        method: 'POST',
        headers: new Headers({
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify(requestBody),
      })
        .then(response => {
          if (!response.ok) {
            console.log('Response not 200, instead we got ' + response.status);
          }
          else {
            console.log("hola")
            console.log(response);
            response.json().then(content => {
              console.log(content.browse_error);
              console.log(content.model_dot);

              try {
                const container = document.getElementById("browseNetwork");
                let parsedData = parseDOTNetwork(content.model_dot);
                let data = {
                  nodes: parsedData.nodes,
                  edges: parsedData.edges,
                }
                let options = parsedData.options;
                browseNetwork = new Network(container, data, options)
              } catch (e) {
                  console.log(e.message)
              }
            });
          }
        })
      .catch(error => {
        console.log(error)
      })
    }

  }
}

</script>

<style scoped>

.iri-input {
  border-bottom: 1px solid lightgray;
  border-top: 1px solid lightgray;
  padding-top: 4px;
  padding-bottom: 4px;
  margin-top: 6px;
  width: 100%;
}

.dot-container {
  flex: 1;
  width: 100%;
  height: 418px;
  border: 1px solid #d3d3d3;
}

.dot-fullscreen-container {
  flex: 1;
  height: 900px;
  border: 1px solid #d3d3d3;
}


</style>