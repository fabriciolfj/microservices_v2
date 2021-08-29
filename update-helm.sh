kubectl apply -f kubernetes/hands-on-namespace.yml

for f in kubernetes/helm/components/*; do helm dep up $f; done
for f in kubernetes/helm/environments/*; do helm dep up $f; done
helm dep ls kubernetes/helm/environments/dev-env/


helm install hands-on-dev-env kubernetes/helm/environments/dev-env -n hands-on --wait