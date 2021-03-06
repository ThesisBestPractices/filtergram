#!/bin/sh

docker pull hseeberger/scala-sbt
docker run --name build hseeberger/scala-sbt /bin/sh -c "
    apt-get -y --force-yes install git; \
    apt-get -y --force-yes install protobuf-compiler; \
    git clone https://github.com/daxzel/chin-news /root/chin-news; \
    cd /root/chin-news; sbt packageAll \
    "
docker pull daxzel/chin-news
docker create --name resultTemp daxzel/chin-news
docker cp build:/root/chin-news/target/chin_news.zip ./chin_news.zip
docker cp ./chin_news.zip resultTemp:/root/chin_news.zip
docker commit resultTemp daxzel/chin-news

docker run --name result daxzel/chin-news /bin/sh -c "cd /root; rm -r ./app; unzip ./chin_news.zip -d ./app"
docker commit result daxzel/chin-news
docker login -e="$DOCKER_EMAIL" -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker push daxzel/chin-news
