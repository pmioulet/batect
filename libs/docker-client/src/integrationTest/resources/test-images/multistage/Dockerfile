FROM alpine:3.15.4 as stage1
RUN echo "stage1" >> /stage-name

FROM alpine:3.15.4
RUN echo "main-stage" >> /stage-name
RUN echo "This stage should never be executed!" && exit 1
