docker run -d --rm -p 3306:3306 --name localmysql -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=app mysql
docker run -d --rm -p 6379:6379 --name localredis redis
