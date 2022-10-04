FROM fluent/fluentd:v1.14.0-debian-1.1
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
RUN apt-get install -y freetds-dev
RUN gem -v
RUN ruby -v
RUN ["gem", "install", "fluent-plugin-sql", "--no-document", "-v", "2.3.0"]
RUN ["gem", "install", "tiny_tds", "-v", "2.1.5"]
RUN ["gem", "install", "activerecord-sqlserver-adapter", "-v", "6.1.2.1"]
COPY config /fluentd/etc
