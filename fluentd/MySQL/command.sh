# To Build Fluent Docker image
docker build -t fluent:mysql .

# To run the Fluent  image the following command can be used 
# but in this command the fluent config file has been passed by using a volume
#docker run --rm -p 24224:24224 -v /home/testmachine/fluent/mysql/config:/fluentd/etc -e FLUENTD_CONF=fluentd.conf  --name fluent_mysql fluent:mysql

# To run the Fluent  image the following command can be used 
# but in this command the fluent config has been included as
# a part of the fluent:mysql image.
docker run --rm -p 24224:24224 -e FLUENTD_CONF=fluentd.conf --name fluent_mysql fluent:mysql

