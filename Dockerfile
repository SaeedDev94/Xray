FROM debian:trixie

ENV LANG=C.UTF-8 \
    DEBIAN_FRONTEND=noninteractive

COPY build-xray.sh /build-xray.sh

ENTRYPOINT ["/build-xray.sh"]
