FROM asciidoctor/docker-asciidoctor as buildenv
COPY ./docs /docs
RUN ["/usr/bin/asciidoctor", "-R", "/docs/", "/docs/**/*.adoc"]

FROM saas-crm/nginx-base:latest

COPY --from=buildenv  /docs/ /usr/share/nginx/html
