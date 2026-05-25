FROM debian:trixie

ENV LANG=C.UTF-8 \
    DEBIAN_FRONTEND=noninteractive

COPY build.sh /build.sh

ENTRYPOINT ["/build.sh"]
