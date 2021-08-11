curl -k https://writer:secret@localhost:8443/oauth2/token -d grant_type=client_credentials -s | jq .
