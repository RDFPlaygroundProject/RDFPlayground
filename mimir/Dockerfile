FROM python:3 as dev
ENV PYTHONUNBUFFERED 1
RUN mkdir /app
WORKDIR /app/mimir
COPY src/main/reasoner/owlrl ./app/mimir
#RUN python /app/mimir/src/main/reasoner/owlrl/setup.py install

FROM maven:3.3-jdk-8-alpine
WORKDIR /app/mimir
COPY pom.xml /app/mimir
COPY . /app/mimir

RUN mvn install

CMD ["mvn", "spring-boot:run"]