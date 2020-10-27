## Set up and running
The backend is written on Kotlin using Spring Boot framework, dependencies and
configurations are done using Maven, so you should install that before.

However, given that it also uses BastyZ/OWL-RL instead of RDFLib/OWL-RL, you 
must set up and environment before launching the backend.

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

Having _maven_ installed, write on your terminal:

``` shell script
# install dependencies
mvn install
```

And you can run it with:

``` shell script
# Activate venv before running SpringBoot
source src/main/reasoner/owlrl/venv/bin/activate

mvn spring-boot:run
```

## Configuration
### Port
Go to `src/main/resources` and edit the file `application.yml`, modifying:
``` sh
port : 9060
```

You can modify a lot of things here, see 
[this documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config-yaml)
to learn more.

### CORS
This API only accepts request from accepted origins, this is added like several
mappings, defining controllers, origins, methods and timeouts. Edit the file
`WebConfig.kt` at `src/main/api/` adding or editing existing mappings.

A mapping looks like:
``` Kotlin
// Based on https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-cors-global
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:9080")
    .allowedMethods("GET", "POST")
    .maxAge(3600)
```

### OWL 2 RL / RDFS Reasoners
The backend contains a full copy of
[OWL-RL](https://github.com/BastyZ/OWL-RL)
written on python 3 on the directory `src/main/reasoner/owlrl`, to run the
`script/owlrl` CLI script.

Our OWL-RL wrapper (on `OwlRLWrapper.kt:38`) executes the following configuration 
elements to infer using BastyZ/OWL-RL where **flag** is either -r or -w for RDFS and OWL 
respectively:

```shell script
CMD_NAME flag -f <temp_file_path>
```

Then read the Runtime CLI for the inference result, and converts this result to DOT.

#### Modify OWL-RL location or version

If you want to upgrade it or use another version of OWL-RL you can place it on the
same place and configuring the environment again, or change the
`ReasonerConfig.kt` file on the `src/main/api/` directory, to change where
the runtime looks for an environment, and the script (lines 10 and 11).

``` Kotlin
# Activations script is not being used right now, because the runtime will get a denied access
var ENV_ACTIVATION_SCRIPT = "source src/main/reasoner/owlrl/venv/bin/activate"
var CMD_NAME = "python src/main/reasoner/owlrl/scripts/owlrl"
```

Pointing to the same script on another place you desire.

#### Configure an environment for OWL-RL
This configuration is exactly the same that the one on the initial setup.

Go to `src/main/reasoner/owlrl` and create a virtualenv (we use _venv_) according to
[venv documentation](https://docs.python.org/3/library/venv.html):
```shell script
# Create venv environment
python3 -m venv venv

# Activate on Windows systems (See configuration section for more info)
venv/Scripts/activate.bat
# for Linux based systems (default configuration)
source venv/bin/activate

# Install BastyZ/OWL-RL on the current environment
python setup.py install

```

## Directories
This project is divided on the following way:
```
src/main
 |
 + -- api # API controllers and code
 |    |
 |    + -- model # api/model endpoints
 |    |
 |    + -- owl # api/owl endpoints
 |    |
 |    + -- shape # api/shape endpoints
 |    |
 |    + -- tdb # api/tdb endpoints
 |    |
 |    + -- Application.kt # App main()
 |
 + -- dot # DOT writter extension for Jena
 |
 + -- reasoner # OWL-RL sources copy, wrapper and temp directory
 |
 + -- shex # ShEx Wrapper, map extension and temp directory
 |
 + -- Utils.kt # Auxiliary functions
```


