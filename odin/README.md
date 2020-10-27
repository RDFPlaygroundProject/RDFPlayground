# Odin frontend
This project runs on nodejs 13 and Vuetify framework (runs on Vue).

## Project setup
``` sh
# install dependencies
npm install
```

### Compiles, hot-reloads for development and production compilation
``` sh
# Run for development
npm run-script serve

# Compiles and minifies for production
npm run-script build
```

### Lints and fixes files
```
npm run lint
```

## Customize configuration
### Port
For development edit the `serve` script, changing the port option on the line:
``` sh
"serve": "vue-cli-service serve --port 8090",
```

For production, you can take the statics under `dist/` folder and open them as
a static website with any port you want. 

#### ⚠ Warning: Configure CORS on the backend if you change this.

### Change API URL
If you need to change where the API is located, go to
`src/components/HelloWorld.vue` and change `backAPI` const.

### More
For more information see 
[Configuration Reference](https://cli.vuejs.org/config/).

## Future work
- Refactor `HelloWorld.vue` to divide the code on several files.
