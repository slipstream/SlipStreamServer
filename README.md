See [SlipStream](https://github.com/slipstream/SlipStream).


## Deploying CIMI with Docker

You'll need to provide CIMI over HTTPS. For this we use the 
Traefik reverse proxy which can automatically request and renew 
Let's Encrypt certificates, given that you have a FQDN.

For local testing however, self signed certificates can be used.

1. create the folder components/traefik/cert

`mkdir -p components/traefik/cert`

2. generate the self signed certificates

`cd components/traefik/cert && openssl req  -nodes -new -x509  -keyout server.pem -out server.cr`

3. create the Traefik config for self signed certs

```bash
cat >components/traefik/traefik.toml <<EOF
defaultEntryPoints = ["http", "https"]
    [entryPoints]
        [entryPoints.http]
        address = ":80"
            [entryPoints.http.redirect]
                entryPoint = "https"
        [entryPoints.https]
        address = ":443"
            [entryPoints.https.tls]
            [[entryPoints.https.tls.certificates]]
                CertFile = "/ssl/server.crt"
                KeyFile = "/ssl/server.pem"
EOF
```

4. if using LE certificates, then replace 3. by (make sure you replace $x$)

```bash
cat >components/traefik/traefik.toml <<EOF
defaultEntryPoints = ["http", "https"]
    [entryPoints]
        [entryPoints.http]
        address = ":80"
            [entryPoints.http.redirect]
                entryPoint = "https"
        [entryPoints.https]
        address = ":443"
            [entryPoints.https.tls]

[acme]
email = "$youremail@yourdomain$"
storage = "acme.json"
entryPoint = "https"
[[acme.domains]]
   main = "$fqdn$"
EOF
```

Finally, add the following into docker-compose.yml (Kibana is optional, and the sixsq/* container versions should be updated):
```yaml
version: '3'
services:
  proxy:
    image: traefik
    restart: unless-stopped
    command: --web --docker --docker.exposedByDefault=false --loglevel=info
    volumes:
     - /var/run/docker.sock:/var/run/docker.sock:ro
     - ./components/traefik/traefik.toml:/traefik.toml
     - ./components/traefik/cert:/ssl:ro
    ports:
     - "80:80"
     - "443:443"
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:5.5.0
    container_name: elasticsearch
    expose:
     - "9200"
     - "9300"
    environment:
     - cluster.name=elasticsearch
     - xpack.security.enabled=false
     - discovery.type=single-node
     - "ES_JAVA_OPTS=-Xms2048m -Xmx2048m"
  kibana:
    image: docker.elastic.co/kibana/kibana:5.5.0
    container_name: kibana
    environment:
      - ELASTICSEARCH_URL=http://elasticsearch:9200
    restart: always
    ports:
     - "5601:5601"
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
    labels:
     - "traefik.enable=true"
     - "traefik.backend=cimi"
     - "traefik.frontend.rule=PathPrefix:/,/"
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