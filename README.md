# Microservices
- Este projeto é uma atualização do repositório https://github.com/fabriciolfj/microservices.
- Temas mais relevantes da atualização, serão abordados

### Authorization
- Oauth2: uma padrão aberto para delegar a autorização.
- OpenId connect: é um complemento do oauth2, que permite que aplicativos cliente, usem dados do seu usuário, para acessar aplicação alvo.

#### Alguns conceitos envolvendo oauth2
- resource owner: dono do recurso, que no caso, usuário final
- client: aplicação terceira
- resource server: servidor que expoẽ ou possui os recursos
- authorization server: responsável pela emissão do token ao cliente, depois que o proprietario do recurso, ou seja,o usuário final, foi autenticado.

### Grant type(S)
- o oauth2 disponibiliza 4 fluxos de concessão de autorização:
  - authorization code: considerado o mais seguro e também o mais complexo. Esse fluxo de subvenção exige que o usuário interaja com o servidor de autorização, usando um navegador, dando consentimento ao aplicativo do cliente.
    - PKCE: esta integrada no fluxo authorization code, é uma camada a mais de segurança, para o client receber o code (ideal para ambientes menos seguros).
  - implicit grant flow: similar ao authorization code, em vez de receber o code na primeira etapa, o client ja recebe o token para uso. Obs: nesse fluxo não possui token de atualização.
  - password credentials: o usuario compartilha suas credenciais com o client, e este a utiliza para solicitar o token.
  - client credentials: utiliza as credencias do próprio client, para solicitar o token (ideal para comunicação entre apis).
 
### OPENID connect
- é um token extra, que vem junto com o token de autenticação.
- na maioria das vezes, o token de autenticação é codificado em jwt, e quando decodifica, conseguimos ver o token extra.
- nesse token extra contém como id o email do usuário (ou username).
- o token extra é assinado digitalmente
- como o mesmo é assinado digitalmente, o aplicativo client confia nas informações, validando a assinatura digital, utilizando chaves publicas do servidor de autorização.

### Observção
- spring authorization server encontra-se na etapa experimental, no qual foi utilizado nesse projeto.
- o authorization server, será responsável pela geração do token jwt.
- os serviços bateram no authorization server, afim de obter o jwks (chave pública), para autenticar o token jwt.

### Abaixo um exemplo de configuração, para requisitar o token via swagger:
```
@SecurityScheme(
  name = "security_auth", type = SecuritySchemeType.OAUTH2,
  flows = @OAuthFlows(
    clientCredentials = @OAuthFlow(
      authorizationUrl = "${springdoc.oAuthFlow.authorizationUrl}",
      tokenUrl = "${springdoc.oAuthFlow.tokenUrl}",
      scopes = {
        @OAuthScope(name = "product:read", description = "read scope"),
        @OAuthScope(name = "product:write", description = "write scope")
      }
    )
  )
)
public class OpenApiConfig {}

```

### Resilience4j
- Em uma arquitetura de microservices, precisamos manter nossos serviços resilientes, ou seja, que se recuperem de erros que podem ocorrer.
- Existem algumas abordagens, as que se destacam são:
  - circuitbreaker: diante a falhas, uma rota alternativa pode ser chamada, o circuitbreaker chama essa rota quando aberto, semi aberto ele checa tempos em tempos a rota original se esta ok, se sinal positivo, circuito e fechado e volta a rotina normal, se negativo, voltamos a rota alternativa.
  - retry: podemos configurar um número de retentativas (é indicado para rotas idempotentes)
  - time limit: tempo limite de espera, a uma chamada a outro servico por exemplo, caso ultrapasse uma exceção será lançada.

- O resilience4j possui os mecanismos salientados acima e muito mais, alem de funcionar em ambientes reativos e imperativos.

#### Resilience4j spring boot
- Resilience4j pode enviar métricas ao actuator do spring e utiliza o oap para isso
- alem de integrar-se também com prometheus.
- Alguns parâmetros de configuração utilizados neste projeto:
  - slidingWindowType: tipo de contagem para abertura do circuitbreaker
  - slidingWindowSite: numero de chamadas com falha
  - failureRateThreshold: percentual com base no parametro acima, exemplo: caso o slidingWindowSite for 5, e o failureRateThreshold 50 (50% de 5), passou de 3 chamadas com falha, o cirtuitbreaker será aberto.
  - automaticTransitionFromOpenToHalfOpenEnabled: abertura para o circuit semi aberto, automaticamente
  - waitDurationInOpenState: tempo em que o circuitbreaker manterá aberto, até que mude para o semi aberto.
  - permittedNumberOfCallsInHalfOpenState: número de chamdas no estado semiaberto, para definir se fecha ou mantem aberto o circuitbreaker.
  - ignoreExceptions: algumas exceções que náo são usadas como falha, para lógica do circuitbreaker (exceptions de negócio pro exemplo).
  - registerHealthIndicator: registrar no actuator
  - allowHealIndicatorToFail: não mudar o indicador de estado de saúde, caso o circuitbreaker esteja aberto ou semi aberto (colocar false).


### Alguns recursos do spring que facilitam seu uso no kubernetes
- graceful shutdown: antes da aplicação se desligar, ela conclui as solicitações pendentes (diante uma configuração de tempo) e deixa de acertar novas solicitações.

```
server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s
```
- liveness e readiness: são endpoints expostos pelo actuator, onde podemos configurar no nosso deployment. Esses endpoints serão utilizados para indicar se o pod terá que ser restartado ou se esta ok e se o mesmo pode receber solicitações.
- exemplo abaixo demonstra que a aplicação somente poderá aceitar solicitações, caso a comunicação com kafka, banco de dados e mongo, estejam funcionando.
```
management.endpoint.health.probes.enabled: true
management.endpoint.health.group.readiness.include: kafka, db, mongo
```

- ingress: é utilizado como um gateway, e no podemos adicionar certificado no mesmo.

### Certificado kubernetes
- neste projeto adotamos o cert-manager, que e um controlador de gerenciamento de certificados para kubernetes.
- utilizaremos certificados auto-assinados no proprio namespace. (não indicado para ambientes de produção, neste caso faça uso de um emissor)
- no script, install-cert-manager.sh, possui os passos ncessários para instalação do certificado.
- para ver como os certificados são criados, na subida dos serviços dentro do kubernetes
```
kubectl get certificates -w --output-watch-events
```
- para ver o vencimento do certificado:
```
kubectl describe cert tls-certificate
``` 

### helm
- gerenciador de pacotes, de código aberto, para o kubernetes.
- neste projeto, faremos adoção de templates comuns, onde seus valores serão substituidos pelos dados do Values.yml ou Chart.yml.
- Para funcionamento de templates, necessitamos da seguinte estrutura de diretórios dentro do projeto:
```
helm
 common
   templates
     _descricao.yml
 componentes
   pasta com nome do microservice
     charts do microservice
 environments
```
- os templates commons, que serão base para os manifestos, deverão iniciar com ` _ `, para o helm não criar manifestos deles.
- _helpers.tpl, possui a lógica para atribuir nomes no manifesto

#### Comandos helm

##### Para ver o manifesto gerado
```
helm dependency update components/product
helm template components/product -s templates/deployment.yml
```
- Existe o arquivo chamado update-helm.sh na raiz do projeto, que atualizará o repositorio do helm para todos os componentes.
- em seguida, podemos executar o comando abaixo para checar como ficou os manifestos de cada um.
```
helm template kubernetes/helm/environments/dev-env/ -- apos atualizacao 
```
- para testar se os manifestos gerados, serão aceitos pelo cluster, podemos executar o comando abaixo:

```
helm install --dry run --debug microservices kubernetes/helm/environments/dev-env
```
- para instalação, incluindo o namespace, utilize o comando:
```
helm install microservice-v2 kubernetes/helm/environments/dev-env -n microservices-v2 --create-namespace
```
- para voltar uma versão
```
helm rollback microservices-v2  -n microservices-v2 -wait
```
- Alguns comandos para ajudar no acompanhamento do deploy
```
kubectl wait --timeout=600s --for=condition=ready pod --all
kubectl get pods -o json | jq .items[].spec.containers[].image
kubectl exec -it 
kubectl exec -it deploy/config-server  -- curl http://localhost/actuator/health/readiness -s | jq .
```

### Service mesh
- é uma camada de infraestrutura que observa e controla a comuniucação entre os microservices.
- os recursos de uma malha de serviço, são:
  -  observabilidade
  -  segurança
  -  aplicação de políticas
  -  resiliência
  -  gerenciamento de tráfico


#### Funcionamento de uma malha de serviço (service mesh)
- cada pod possui um proxy, onde toda a informação trafegada.
- o proxy é configurado em tempo de excução, por um plano de controle do service mesh, usando apis expostas pelo proxy
- o plano de controle coleta dados de telemetria
- possui um plano de dados, para lidar com o tráfico externo

#### Implementação
- Uma implementação popular, referente ao service mesh, é o istio
- o istio é apoio por diversos subprojetos, como:
  - kiali: provém observabilidade do service mesh
  - tracing: manipula e visualiza informações distribuídas de rastreamento.
  - prometheus: armazena os dados de métricas.
  - grafana: visualiza as métricas e outros dados relacionados, coletados pelo prometheus.
- istio injeta um novo recipiente ao pod do microservice, conhecido como sidecar, onde auxilia na coleta de informações.

##### Objetos istio utilizados nesse projeto.
- gateway: utilizado para lidar com o tráfego de entrada ou saida da malha de serviço, para isso ele depende de um serviço virtual, que encaminha a um service kubernetes.
- virtual service: é utilizado para definir regras de roteamento na malha de serviço, como por exemplo: direcionar tráfego de entrada de um gateway a um serviço kubernetes.
- destination rule: utilizado para definir políticas e regras de tráfego que são roteados para um serviço específico.
  - obs: a diferença entre um destinationrule e um virtualservice, virutalservice é utilizado para configurar um roteamento a um serviço, e o destination rule é configurado paraa lidar com o trafego de um serviço selecionado (criptografar os dados por exemplo).
- peerAuthentication: responsável pela autenticação entre serviços, dentro da malha.
- requestauthentication: responsável pela autenticação do usuário final, onde suporte tokens jwt.
- authorizationPolicy: usado para fornecer controle de acesso no istio.

##### Instalação istio
- verificando se o cluster está pronto para instalação:
```
istioctl experimental precheck
```
- instalando com base num perfil demo
```
istioctl install --skip-confirmation \
 --set profile=demo \
 --set meshConfig.accessLogFile=/dev/stdout \
 --set meshConfig.accessLogEncoding=JSON
```
- aguardando
``` 
kubectl -n istio-system wait --timeout=600s --for=condition=available deployment --all
```
- instalando os demais recursos
```
istio_version=$(istioctl version --short --remote=false) 
echo "Installing integrations for Istio v$istio_version"  
kubectl apply -n istio-system -f https://raw.githubusercontent.com/istio/istio/${istio_version}/samples/addons/kiali.yaml  
kubectl apply -n istio-system -f https://raw.githubusercontent.com/istio/istio/${istio_version}/samples/addons/jaeger.yaml  
kubectl apply -n istio-system -f https://raw.githubusercontent.com/istio/istio/${istio_version}/samples/addons/prometheus.yaml  
kubectl apply -n istio-system -f https://raw.githubusercontent.com/istio/istio/${istio_version}/samples/addons/grafana.yaml
```
