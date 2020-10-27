import Vue from 'vue';
import App from './App.vue';
import vuetify from './plugins/vuetify';
import Vuetify from 'vuetify';
import vis from 'vis';
import { Network } from 'vue-vis-network';

Vue.component('vis-network', Network);

Vue.config.productionTip = false;

Vue.use(Vuetify);
Vue.use(vis);

new Vue({
  vis,
  vuetify,
  render: h => h(App)
}).$mount('#app');
