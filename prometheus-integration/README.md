Prometheus Monitoring for TIBCO BusinessWorks is an open source plug-in for TIBCO BusinessWorks Studio. It is designed to monitor JVM & application's activity & process related statistics via Prometheus.

To enable Prometheus monitoring for applications running in Docker follow the below steps:
1) Add JAR of this plugin in addon folder and build the base image.
2) Expose port 9095 in your Dockerfile.
3) Set BW_PROMETHEUS_ENABLE environment variable to TRUE while running the Docker image of the application.
4) Run your application using below command:
docker run -d -p 9095:9095 -p exposed-port-by-application:8080 -e BW_PROMETHEUS_ENABLE=TRUE <application-name>
5) Add prometheus.yml file and run the Prometheus server using below command:
docker run -p 9090:9090 -v path-to-file/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus:latest --config.file=/etc/prometheus/prometheus.yml

To enable Prometheus monitoring for applications running in PCF follow the below steps:
1) Set BW_PROMETHEUS_ENABLE environment variable to TRUE in the manifest.yml file of the application.
1) Add JAR of this plugin in addon folder and build the buildpack.
2) Push your application to the cloud using newly created buildpack. Once application gets started you would be able to see the Prometheus metrics at applications-routable-url/merics endpoint.
3) Add promregator.yml file and run the Promregator server using below command:
docker run -d --name promregator -p 127.0.0.1:56710:8080 -v path-to-file/promregator.yml:/etc/promregator/promregator.yml -e CF_PASSWORD=<cloud-foundary-password> promregator/promregator:0.4.1
4) Add prometheus.yml file and run the Prometheus server using below command:
docker run --name prometheus -v path-to-file/prometheus.yml:/etc/prometheus/prometheus.yml -p 127.0.0.1:9090:9090 --link promregator prom/prometheus:latest
