FROM alpine:3.15.4

ARG CACHE_BUSTING_ID
COPY test.sh /test-$CACHE_BUSTING_ID.sh
RUN /test-$CACHE_BUSTING_ID.sh

HEALTHCHECK --interval=0.1s CMD echo -n "Hello from the healthcheck"
