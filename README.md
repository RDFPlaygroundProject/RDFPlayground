# RDF Playground
This project allows web users to write RDF as Turtle, check its syntax, 
visualize the data as a graph, and use SPARQL, RDFS, OWL, SHACL and ShEx.

The project is intended to provide many functionalities in one system, 
and is intended to be used for small examples (e.g., for teaching or demos).

You can see a [demo here](http://rdfplayground.dcc.uchile.cl/). Note that the 
demo is not intended to be a production system. If you intend to use RDF Playground
regularly (e.g., for a class), we would recommend to install and run it locally in a 
server you control. There are two options: direct or Docker.

# Installation and running (direct)

Ensure you've cloned or downloaded this repository on the machine you want to run RDF Playground. You will need to install and run the back-end (Mimir) and the front-end (Odin) for RDF Playground to run.

### Back end (mimir folder)
The backend is written in Kotlin using the Spring Boot framework; dependencies and 
configurations are done using Maven. The code also has [a fork](https://github.com/BastyZ/OWL-RL) of the (Python) [OWL-RL reasoner](https://github.com/RDFLib/OWL-RL) 
inside, so you have to create and environment for this code before executing the backend.

⚠ For this to work you should use Python 3.5+ and Pip

Go to `src/main/reasoner/owlrl` and create a virtualenv (we use _venv_) according to
[venv documentation](https://docs.python.org/3/library/venv.html) and install BastyZ/OWL-RL:
```shell script
# Create venv environment
python3 -m venv venv

# Activate
source venv/bin/activate

# Install BastyZ/OWL-RL on the current environment
python setup.py install
```

Then, having _maven_ installed, you go to `mimir/` directory where `pom.xml` is and 
write in your terminal:

``` sh
# install dependencies
mvn install
```

And you can run it with:

``` sh
# Activate venv before running SpringBoot
source src/main/reasoner/owlrl/venv/bin/activate

mvn spring-boot:run
```

### Front end (odin folder)
The frontend is written in Javascript + HTML using Vuetify framework and nodejs
13; having that and npm installed you can go to the `odin/` directory and write:

``` sh
# install dependencies
npm install
```

And you can run it with:
``` sh
# run for development
npm run-script serve

# create a production build
npm run-script build
```
This last command produces a production-ready bundle in the `dist/` directory,
see https://cli.vuejs.org/guide/cli-service.html#vue-cli-service-build for more
information.

For more information on the serve command see 
https://cli.vuejs.org/guide/cli-service.html#vue-cli-service-serve.

### Configuration
For configurations on the backend and frontend see their respective README
files.

# Installation and running (Docker)

Ensure you've cloned or downloaded this repository on the machine you want to run RDF Playground.

The repository also provides a `Dockerfile` that facilitates installing and running RDF Playground in Docker (credit to [@fvillena](https://github.com/fvillena)).

Ensure that Docker is installed and running ([see instructions](https://docs.docker.com/engine/install/)).

The following commands assume a Unix-like environment. **Some commands may need `sudo` depending on how you've installed and run Docker.**

With Docker running, go to the RDFPlayground main folder (the one containing `Dockerfile`):

```sh
cd /path/to/RDFPlayground
```

Next we build a Docker image:

```sh
docker build -t rdf-playground .
```

This might take some time. The final Docker image should be around 1GB. Once installed you can run

```sh
sudo docker image list
```

Where you should see the `rdf-playground` image listed.

Next we need to run a Docker container for RDF Playground. Here we assume that we want to map between port `80` of the container to port `80` of the `localhost` of the machine running Docker (the first `80` is the port of the machine; the second `80` is the port of the container). We also assume that we want to restart RDF Playground if for any reason it is not running with the `always` option ([see more options](https://docs.docker.com/config/containers/start-containers-automatically/)).

```sh
docker run -dp 127.0.0.1:80:80 --restart always rdf-playground
```

You should now see the container running with:

```sh
docker ps
```

If this worked, you should be able to call (in the case of port `80` on the machine, otherwise adding `:1234` without space if using port `1234` on the machine):

```sh
wget localhost
```

This should download a HTML file, which you can preview with:

```sh
more index.html
```

It may indicate that Javascript is not running, but this should not be a problem once you load the page from the browser. 

Now RDF Playground should be running on the selected port of your machine. (You may still need to configure external Web access via a static I.P. or hostname to the selected port on your machine.)

Copyright © 2020 Bastián Inostroza, Licensed under the Apache License, Version 2.0.
