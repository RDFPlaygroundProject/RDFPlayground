FROM ubuntu:18.04

RUN apt update && apt-get install -y python3 python3-pip default-jdk maven
RUN pip3 install virtualenv
RUN mkdir /app
WORKDIR /app/mimir
COPY . /app/mimir
RUN cd src/main/reasoner/owlrl && python setup.py install

COPY pom.xml /app/mimir
RUN mvn install

CMD /bin/bash -c "source venv/bin/activate" ; mvn spring-boot:run