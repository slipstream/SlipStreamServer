See [SlipStream](https://github.com/slipstream/SlipStream).


## Deploying CIMI with Docker

Add the following into docker-compose.yml:
```yaml
version: '3'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:5.5.0
    container_name: elasticsearch
    ports:
     - "9200:9200"
     - "9300:9300"
    expose:
     - "9200"
     - "9300"
    environment:
     - cluster.name=elasticsearch
     - xpack.security.enabled=false
     - discovery.type=single-node
     - "ES_JAVA_OPTS=-Xms2048m -Xmx2048m"
  rc:
    image: sixsq/ring-container:3.46-SNAPSHOT
    ports:
     - "5000:5000"
    volumes:
     - ringcontainer:/opt/slipstream/ring-container
     - ringcontainerexample:/opt/slipstream/ring-example
  cimi:
    image: sixsq/cimi:3.46-SNAPSHOT
    ports:
     - "8201:8201"
    volumes:
     - ringcontainer:/opt/slipstream/ring-container
     - ringcontainerexample:/opt/slipstream/ring-example
     - resources:/opt/slipstream/cimi/lib
  cimi-resources:
    image: sixsq/cimi-resources:3.46-SNAPSHOT
    volumes:
     - resources:/opt/slipstream/cimi/lib

volumes:
  ringcontainer: {}
  ringcontainerexample: {}
  resources: {}
```

And run `docker-compose up` 