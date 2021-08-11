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
  - time limit: tempo limite de espera, a uma chamada a outro servico por exemplo.

- O resilience4j possui os mecanismos salientados acima e muito mais, alem de funcionar em ambientes reativos e imperativos.

#### Resilience4j spring boot
- Resilience4j pode enviar métricas ao actuator do spring e utiliza o oap para isso
- alem de integrar-se também com prometheus.
