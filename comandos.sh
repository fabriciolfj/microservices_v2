curl -k https://writer:secret@localhost:8443/oauth2/token -d grant_type=client_credentials -s | jq .


 docker-compose exec product-composite curl -s http://product-composite:8080/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r