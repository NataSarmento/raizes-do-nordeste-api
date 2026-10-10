# Raízes do Nordeste — API Back-End

API REST (Java 17 + Spring Boot 3) da rede de restaurantes **Raízes do Nordeste**: pedidos multicanal, estoque por unidade,
pagamento externo (mock), fidelização com consentimento, autenticação JWT por perfil, auditoria e LGPD.

- **Aluno:** Natã Sarmento da Silva — RU: `<seu RU>`
- **Repositório:** `<link público do repositório>`
- **Swagger local:** http://localhost:8080/swagger-ui.html (JSON em `/v3/api-docs`)
- **Coleção Postman:** `postman/raizes-do-nordeste.postman_collection.json`
- **Relatório (PDF):** `docs/RELATORIO.md` → exportado como `<RU>_Projeto_Back_End.PDF`

## 1. Requisitos
| Item | Versão |
|---|---|
| JDK | 17 ou superior |
| Maven | 3.9+ |
| Banco | H2 em arquivo (`./data/raizes`), modo PostgreSQL — **não precisa instalar nada** |
| Postman | qualquer versão recente (ou Insomnia) |

## 2. Variáveis de ambiente
Copie `.env.example` e exporte as variáveis desejadas. **Todas têm valor padrão**, então é opcional rodar local.

| Variável | Padrão | Descrição |
|---|---|---|
| `PORT` | 8080 | Porta HTTP |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | H2 arquivo / sa / vazio | Conexão com o banco |
| `JWT_SECRET` | (chave de dev) | **Mín. 32 caracteres**; troque em produção |
| `JWT_EXPIRATION_MINUTES` | 60 | Validade do token |

## 3. Instalar, migrar e executar
```bash
mvn clean install          # baixa dependências, compila e roda os testes unitários
mvn spring-boot:run        # sobe a API em http://localhost:8080
```
- **Migrations:** o Flyway executa `src/main/resources/db/migration/V1__schema.sql` automaticamente na subida.
- **Seed:** na primeira execução (banco vazio) o `DataSeeder` cria usuários, unidades, produtos, estoque e a promoção `BEMVINDO10`.
- Para recomeçar do zero: apague a pasta `./data` e suba de novo.

### Usuários do seed
| Perfil | E-mail | Senha |
|---|---|---|
| ADMIN | admin@raizes.com | Admin@123 |
| GERENTE | gerente@raizes.com | Gerente@123 |
| ATENDENTE | atendente@raizes.com | Atendente@123 |
| COZINHA | cozinha@raizes.com | Cozinha@123 |
| CLIENTE | cliente@raizes.com | Cliente@123 |

## 4. Fluxo principal (Fluxo A): Pedido → Pagamento mock → Status
1. `POST /auth/login` → copie `accessToken` → Swagger: botão **Authorize** → `Bearer <token>`.
2. `POST /pedidos` com `canalPedido` (APP, TOTEM, BALCAO, PICKUP, WEB).
3. `POST /pagamentos` com `{"pedidoId": X, "simularResultado": "APROVADO" | "RECUSADO" | "TIMEOUT"}`.
4. `PATCH /pedidos/{id}/status` (COZINHA → `PRONTO`; ATENDENTE → `ENTREGUE`).
5. `GET /pedidos?canalPedido=TOTEM` filtra por canal.

## 5. Como rodar os testes
**Unitários (domínio):** `mvn test`

**Coleção Postman (plano de testes T01–T24):**
1. Suba a API com banco limpo (apague `./data`).
2. Importe a coleção; a variável `baseUrl` já aponta para `http://localhost:8080`.
3. *Collection Runner* → execute **a coleção inteira, na ordem das pastas** (Auth primeiro: ela salva os tokens).
4. Cada requisição tem asserts (`pm.test`); o runner mostra aprovado/reprovado.

## 6. Estrutura
```
src/main/java/br/edu/raizes
├── domain/          entidades, enums, máquina de estados do pedido, exceções
├── application/     serviços (casos de uso) e portas (PagamentoGateway)
├── infrastructure/  repositórios JPA, segurança JWT, gateway mock, seed, config
└── api/             controllers, DTOs (contratos), tratamento global de erros
```
