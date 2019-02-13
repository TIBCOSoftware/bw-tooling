#Copyright © 2019. TIBCO Software Inc.
FROM debian:jessie-slim
MAINTAINER TIBCO Software Inc.
LABEL io.openshift.s2i.scripts-url="image:///usr/libexec/s2i" 
COPY ./s2i/bin/ /usr/libexec/s2i
RUN chmod 777 /usr/libexec/s2i/*
EXPOSE 8080
CMD ["/usr/libexec/s2i/usage"]
