./gradlew clean build;

cd /home/spark/repositorios/microservices_v2/spring-cloud/authorization-server/
docker build -t fabricio211/authorization-server . ;

cd /home/spark/repositorios/microservices_v2/microservices/product-service;
docker build -t fabricio211/product-service . ;

cd /home/spark/repositorios/microservices_v2/microservices/product-composite-service;
docker build -t fabricio211/product-composite-service  . ;

cd /home/spark/repositorios/microservices_v2/microservices/recommendation-service/;
docker build -t fabricio211/recommendation-service . ;

cd /home/spark/repositorios/microservices_v2/microservices/review-service;
docker build -t fabricio211/review-service . ;


docker push fabricio211/product-composite-service;
docker push fabricio211/product-service;
docker push fabricio211/recommendation-service;
docker push fabricio211/review-service;
docker push fabricio211/authorization-server;