FROM alpine:3.14

RUN apk update && apk add --no-cache \
    openjdk8 \
    maven \
    python3 && ln -sf python3 /usr/bin/python
RUN python3 -m ensurepip
RUN pip3 install --no-cache --upgrade pip setuptools
COPY mimir /app/mimir
WORKDIR /app/mimir/src/main/reasoner/owlrl
RUN python3 setup.py install
WORKDIR /app/mimir
RUN mvn install

RUN apk update && apk add --no-cache \
    nodejs \
    npm
COPY odin /app/odin
WORKDIR /app/odin
RUN npm install

RUN apk update && apk add --no-cache \
    nginx
COPY nginx/default.conf /etc/nginx/http.d/

RUN apk update && apk add --no-cache \
    supervisor
RUN mkdir -p /var/log/supervisor
ADD supervisord/supervisord.conf /etc/

EXPOSE 80
CMD ["/usr/bin/supervisord"]