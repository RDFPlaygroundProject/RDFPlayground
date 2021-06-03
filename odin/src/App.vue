<template>
  <v-app>
    <meta charset="utf-8">
    <v-app-bar
      app
      color="primary"
      dark
    >
      <div class="d-flex align-center align-content-center">
        <v-img
          alt="RDF Playground Logo"
          class="shrink mr-2"
          contain
          src="./assets/RDFPlayground.svg"
          transition="scale-transition"
          width="40"
        />

        <h1>RDF Playground</h1>

      </div>

      <v-spacer></v-spacer>

      <!-- h2 class="d-none d-sm-flex">Universidad de Chile</h2-->
      <v-img
        alt="University of Chile's Logo"
        src="./assets/logo-universidad-de-chile.svg"
        class="shrink d-none d-sm-flex"
        contain
        transition="scale-transition"
        width="120"
      />
    </v-app-bar>

    <v-content>
      <HelloWorld/>
    </v-content>
  </v-app>
</template>

<script>
import HelloWorld from './components/HelloWorld/HelloWorld.vue';
document.title = "RDF Playground";

export default {
  name: 'App',

  components: {
    HelloWorld,
  },

  data: () => ({
    //
  }),
};
</script>
