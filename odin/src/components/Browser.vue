<template>
  <v-container>
    <v-row class="ma-1 mb-1">
      <input class="iri-input" v-model="uri" placeholder="Enter your IRI here">
      <v-col class="text-right">
        <v-btn
            v-on:click="searchIri">
          Search
        </v-btn>
      </v-col>
    </v-row>
    <v-card flat tile class="pb-0 pt-0">
      <v-card-text class="pb-0 pl-0">
        <v-lined-textarea
            v-model="graph_text"
            counter
            rows="15"
        />
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script>


import VLinedTextarea from "@/components/VLinedTextarea";
const backAPI = "localhost:9060";

export default {


  name: "Browser",
  components: {"v-lined-textarea": VLinedTextarea},
  data: () => ({
    graph_text: "",
    graph_text_disabled: "",
    uri: "",
    browse_error_text: "",



  }),
  props: ['checkSyntaxColorResetParent'],

  methods: {
    checkColor: function () {
      this.checkSyntaxColorResetParent();
    },

    searchIri: function () {
      let requestBody = { uri: this.uri.toString() };
      fetch(`http://` + backAPI + `/api/model/browse`, {
        method: 'POST',
        headers: new Headers({
          'Content-type': 'application/json'
        }),
        body: JSON.stringify(requestBody)
      })
      .then(response => {
        if (!response.ok) {
          response.json().then(responseBody => {
            this.browse_error_text = 'response not 200, was' + response.status + ' ' + responseBody.browse_error;

          })
        }
        else {
          response.json().then(content => {
            this.graph_text = content.data_dot;
            this.browse_error_text = content.browse_error

          })
        }
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

</style>