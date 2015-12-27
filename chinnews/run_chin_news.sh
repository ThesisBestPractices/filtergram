#!/bin/sh

docker rm `docker ps --no-trunc -aq`
docker pull daxzel/chin-news
CONTAINER_ID=$(docker create daxzel/chin-news)
echo "Container id $CONTAINER_ID"
docker cp ./application.conf $CONTAINER_ID:/root/app/application.conf
sudo docker commit $CONTAINER_ID daxzel/temp
docker-compose up
docker rm $CONTAINER_ID