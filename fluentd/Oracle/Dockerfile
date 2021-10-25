FROM fluent/fluentd:v1.6-debian-1
USER root
RUN apt-get update -y
RUN apt install -y rbenv
RUN apt-get install -y ruby-full
RUN apt-get install -y gem
RUN apt-get install -y ruby-dev
RUN apt-get install -y gcc	
RUN apt-get install -y libffi-dev
RUN apt-get install -y make
RUN apt-get install -y libpq-dev
RUN gem -v
RUN ruby -v
COPY instantclient /usr/src/app/lib/instantclient

ENV CLIENT_FILENAME instantclient-basic-linux.x64-18.5.0.0.0dbru.zip
WORKDIR /opt/oracle/lib
RUN apt-get update
RUN apt-get install libaio1
RUN LIBS="*/*" && \
    unzip /usr/src/app/lib/instantclient/${CLIENT_FILENAME} ${LIBS} && \
    for lib in ${LIBS}; do mv ${lib} /usr/lib; done

ENV CLIENT_FILENAME instantclient-sdk-linux.x64-21.1.0.0.0.zip
WORKDIR /opt/oracle/lib
RUN apt-get update
RUN apt-get install libaio1
RUN LIBS="*/*" && \
    unzip /usr/src/app/lib/instantclient/${CLIENT_FILENAME} ${LIBS} && \
    for lib in ${LIBS}; do mv ${lib} /usr/lib; done    

ENV LD_LIBRARY_PATH=/usr/lib
RUN export LD_BIND_NOW=1
RUN ["gem", "install", "ruby-oci8"]
RUN ["gem", "install", "activerecord-oracle_enhanced-adapter"]
RUN ["gem", "install", "fluent-plugin-sql", "--no-document"]
COPY config /fluentd/etc
