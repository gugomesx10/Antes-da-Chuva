# Antes da Chuva

Aplicativo mobile da Global Solution 2026/2 da FIAP, na disciplina de Mobile Application Development.

O Antes da Chuva ajuda moradores a registrar problemas que podem agravar alagamentos, como bueiros entupidos e entulho nas ruas. A equipe de zeladoria acompanha os chamados e registra o andamento do atendimento.

## Como funciona

- **Cidadão** — cria uma conta, registra ocorrências e acompanha o histórico. Enquanto o chamado estiver aberto, o autor pode editar ou cancelar o relato. O cancelamento fica registrado no histórico.
- **Gestor** — consulta a fila de chamados e atualiza o status com uma observação para o morador. O cadastro pelo app é destinado a cidadãos.
- **Triagem** — o backend usa Spring AI para processar o relato. Quando o resultado está disponível, o detalhe da ocorrência mostra a prioridade, o resumo e a orientação.

O app tem telas de login, cadastro, lista, nova ocorrência, detalhes, edição, perfil e painel do gestor. A tela **Sobre o App** exibe a versão e o hash do commit usado no build.

## Executar o app

É preciso ter Node.js 24, npm e um dispositivo ou emulador com Expo Go compatível com o SDK 58.

Na pasta do projeto, execute os comandos abaixo. A cópia do `.env` é necessária na primeira execução.

```powershell
npm ci
Copy-Item .env.example .env
npm start
```

Escaneie o QR code com o Expo Go ou pressione `a` no terminal para abrir no emulador Android. O `.env.example` já aponta para a API hospedada no Azure. Para usar outra instância, ajuste `EXPO_PUBLIC_API_URL` no `.env` e reinicie o Expo.

Para rodar no navegador, use `npx expo start --web --port 8099`. O login e as demais chamadas pela web dependem de a origem estar em `CORS_ALLOWED_ORIGINS` no backend (o padrão já inclui `http://localhost:8099` e `http://localhost:8081`).

## Backend e contas de teste

O app usa React Native, Expo SDK 58, Expo Router e TypeScript. As chamadas são feitas com Fetch para a API Java, que usa Spring Boot e Spring Security com JWT. Ocorrências e histórico ficam no backend. O AsyncStorage armazena apenas a sessão.

- Backend Java: na raiz deste repositório (pasta acima de `frontend/`)
- Versão web publicada: o build Maven do backend embute o app web no `.jar`, servido na mesma URL da API (`/`). Nesse build, `EXPO_PUBLIC_API_URL=same-origin` faz o app usar caminhos relativos.
- [OpenAPI](https://app-antesdachuva-561082.azurewebsites.net/v3/api-docs)
- [Status da API](https://app-antesdachuva-561082.azurewebsites.net/actuator/health)
- [Contrato da integração](docs/backend-contract.md)

### Contas de demonstração

| Perfil | E-mail | Senha de teste |
| --- | --- | --- |
| Cidadão (Maria) | `maria@antesdachuva.com` | `Cidadao@2026` |
| Cidadão (João) | `joao@antesdachuva.com` | `Cidadao@2026` |
| Gestor | `gestor@antesdachuva.com` | `Gestor@2026` |

Essas são as contas da [carga de demonstração do backend](https://github.com/gugomesx10/Antes-da-Chuva/blob/1e0b7ad19cb1763bfaed167864cb5123ef5315af/src/main/resources/db/migration/V3__seed_test_users.sql). Para testar com uma conta nova de cidadão, use **Criar conta** na tela de login.

## Organização do projeto

| Pasta | Conteúdo |
| --- | --- |
| `src/app` | Telas e rotas do Expo Router |
| `src/components` | Formulários, histórico e componentes visuais |
| `src/context` | Sessão e dados compartilhados entre telas |
| `src/services` | Cliente HTTP, autenticação e ocorrências |
| `src/types` | Tipos da aplicação e do contrato Java |

Para verificar TypeScript, lint e a inicialização do Expo, execute `node scripts/validate.cjs`.

## Vídeo

| Vídeo de demonstração | [Assistir no YouTube](https://youtube.com/shorts/YKgwoY1CVpQ?is=9W469k3YyKDAAUHT) |


