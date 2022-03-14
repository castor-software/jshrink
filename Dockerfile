FROM maven:3.8.4-jdk-8

ENV OSNAME=linux
ENV JDK=/usr/local/openjdk-8

RUN apt update && apt install -y git make gcc;

COPY jshrink jshrink
COPY resources resources
COPY experiment_resources experiment_resources

RUN cd experiment_resources/jshrink-mtrace/jmtrace && make && cd ../../../;

ENTRYPOINT [ "bash" ]