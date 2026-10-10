import json, uuid
BASE = "{{baseUrl}}"

def tests(*lines):
    return {"listen": "test", "script": {"type": "text/javascript", "exec": list(lines)}}

def req(name, method, path, body=None, token=None, tst=None, headers=None, desc=""):
    h = [{"key": "Content-Type", "value": "application/json"}] if body is not None else []
    for k, v in (headers or {}).items():
        h.append({"key": k, "value": v})
    r = {"method": method, "header": h, "url": {"raw": BASE + path, "host": ["{{baseUrl}}"], "path": [p for p in path.split("?")[0].split("/") if p],
         "query": [{"key": q.split("=")[0], "value": q.split("=")[1]} for q in (path.split("?")[1].split("&") if "?" in path else [])]},
         "description": desc}
    if token:
        r["auth"] = {"type": "bearer", "bearer": [{"key": "token", "value": "{{%s}}" % token, "type": "string"}]}
    else:
        r["auth"] = {"type": "noauth"}
    if body is not None:
        r["body"] = {"mode": "raw", "raw": json.dumps(body, indent=2, ensure_ascii=False), "options": {"raw": {"language": "json"}}}
    item = {"name": name, "request": r}
    if tst: item["event"] = [tst]
    return item

def login(name, email, senha, var):
    return req(name, "POST", "/auth/login", {"email": email, "senha": senha},
               tst=tests('pm.test("200 e accessToken", () => { pm.response.to.have.status(200); pm.expect(pm.response.json().accessToken).to.be.a("string"); });',
                         'pm.collectionVariables.set("%s", pm.response.json().accessToken);' % var))

def pedido_body(canal="TOTEM", prod=1, qtd=2, unidade=1, extra=None):
    b = {"canalPedido": canal, "unidadeId": unidade, "itens": [{"produtoId": prod, "quantidade": qtd}], "formaPagamento": "MOCK"}
    if extra: b.update(extra)
    return b

def criar_pedido(name, var, body, token="tokenCliente"):
    return req(name, "POST", "/pedidos", body, token, headers={"Idempotency-Key": "{{$guid}}"},
               tst=tests('pm.test("201 criado", () => pm.response.to.have.status(201));',
                         'pm.collectionVariables.set("%s", pm.response.json().id);' % var))

def status_is(code, *extra):
    return tests('pm.test("status %d", () => pm.response.to.have.status(%d));' % (code, code), *extra)

def padrao_erro():
    return 'pm.test("padrão de erro", () => { const j = pm.response.json(); pm.expect(j).to.have.keys("timestamp","status","codigo","mensagem","path"); });'

pastas = []

pastas.append(("Auth", [
    login("T01 - Login CLIENTE válido (200 + accessToken)", "cliente@raizes.com", "Cliente@123", "tokenCliente"),
    login("Setup - Login ATENDENTE", "atendente@raizes.com", "Atendente@123", "tokenAtendente"),
    login("Setup - Login COZINHA", "cozinha@raizes.com", "Cozinha@123", "tokenCozinha"),
    login("Setup - Login GERENTE", "gerente@raizes.com", "Gerente@123", "tokenGerente"),
    login("Setup - Login ADMIN", "admin@raizes.com", "Admin@123", "tokenAdmin"),
    req("T24 - Login com senha errada (401)", "POST", "/auth/login", {"email": "cliente@raizes.com", "senha": "errada123"},
        tst=status_is(401, padrao_erro())),
    req("Cadastro de cliente com consentimento LGPD (201)", "POST", "/auth/register",
        {"nome": "Maria {{$randomInt}}", "email": "maria{{$timestamp}}@email.com", "senha": "Senha@123", "consentimentoLgpd": True, "consentimentoFidelidade": False},
        tst=status_is(201)),
    req("Cadastro SEM consentimento LGPD (422)", "POST", "/auth/register",
        {"nome": "Sem Consentimento", "email": "x{{$timestamp}}@email.com", "senha": "Senha@123", "consentimentoLgpd": False},
        tst=status_is(422, padrao_erro())),
]))

pastas.append(("Unidades e Cardápio", [
    req("T02 - Listar unidades (200, público)", "GET", "/unidades?page=1&limit=10", tst=status_is(200, 'pm.test("2 unidades", () => pm.expect(pm.response.json().conteudo.length).to.be.at.least(2));')),
    req("T03 - Cardápio da unidade 1 (200)", "GET", "/unidades/1/cardapio?page=1&limit=10",
        tst=status_is(200, 'pm.test("itens com disponivel", () => pm.expect(pm.response.json().conteudo[0]).to.have.property("disponivel"));')),
]))

pastas.append(("Pedidos - fluxo principal", [
    criar_pedido("T04 - Criar pedido canal TOTEM com promoção (201)", "pedidoId",
                 pedido_body(extra={"codigoPromocao": "BEMVINDO10"})),
    req("T05 - Pagamento mock APROVADO (201 + pedido EM_PREPARO)", "POST", "/pagamentos", {"pedidoId": "{{pedidoId}}", "simularResultado": "APROVADO"}, "tokenCliente",
        tst=status_is(201, 'pm.test("aprovado e EM_PREPARO", () => { const j = pm.response.json(); pm.expect(j.status).to.eql("APROVADO"); pm.expect(j.statusPedido).to.eql("EM_PREPARO"); });')),
    req("T06 - Cozinha marca PRONTO (200)", "PATCH", "/pedidos/{{pedidoId}}/status", {"status": "PRONTO"}, "tokenCozinha",
        tst=status_is(200, 'pm.test("PRONTO", () => pm.expect(pm.response.json().status).to.eql("PRONTO"));')),
    req("T07 - Atendente marca ENTREGUE (200)", "PATCH", "/pedidos/{{pedidoId}}/status", {"status": "ENTREGUE"}, "tokenAtendente",
        tst=status_is(200, 'pm.test("ENTREGUE", () => pm.expect(pm.response.json().status).to.eql("ENTREGUE"));')),
    req("T08 - Listar pedidos filtrando canalPedido=TOTEM (200)", "GET", "/pedidos?canalPedido=TOTEM&page=1&limit=10", token="tokenAtendente",
        tst=status_is(200, 'pm.test("todos TOTEM", () => pm.response.json().conteudo.forEach(p => pm.expect(p.canalPedido).to.eql("TOTEM")));')),
    req("Consultar pedido por id (200)", "GET", "/pedidos/{{pedidoId}}", token="tokenCliente", tst=status_is(200)),
]))

pastas.append(("Fidelidade", [
    req("T09 - Saldo de pontos após pagamento (200, pontos > 0)", "GET", "/fidelidade/saldo", token="tokenCliente",
        tst=status_is(200, 'pm.test("pontos acumulados", () => pm.expect(pm.response.json().pontos).to.be.above(0));')),
    req("Histórico de pontos (200)", "GET", "/fidelidade/historico?page=1&limit=10", token="tokenCliente", tst=status_is(200)),
]))

pastas.append(("Estoque e Produtos", [
    req("T10 - Entrada de estoque pelo GERENTE (201)", "POST", "/estoque/movimentos",
        {"unidadeId": 1, "produtoId": 1, "tipo": "ENTRADA", "quantidade": 10, "motivo": "Reposição semanal"}, "tokenGerente", tst=status_is(201)),
    req("Consultar saldo de estoque da unidade 1 (200)", "GET", "/estoque?unidadeId=1&page=1&limit=10", token="tokenGerente", tst=status_is(200)),
    req("Criar produto (GERENTE) (201)", "POST", "/produtos", {"nome": "Cuscuz Nordestino", "descricao": "Com ovo e manteiga", "categoria": "Pratos", "preco": 15.5}, "tokenGerente", tst=status_is(201)),
]))

pastas.append(("Auditoria", [
    req("T11 - Auditoria registra criação de pedido (200)", "GET", "/auditoria?acao=PEDIDO_CRIADO&page=1&limit=5", token="tokenAdmin",
        tst=status_is(200, 'pm.test("há registros", () => pm.expect(pm.response.json().conteudo.length).to.be.above(0));')),
]))

pastas.append(("Erros e regras negativas", [
    req("T12 - Listar pedidos sem token (401)", "GET", "/pedidos", tst=status_is(401, padrao_erro())),
    req("T13 - CLIENTE tenta movimentar estoque (403)", "POST", "/estoque/movimentos",
        {"unidadeId": 1, "produtoId": 1, "tipo": "ENTRADA", "quantidade": 1}, "tokenCliente", tst=status_is(403, padrao_erro())),
    req("T14 - Pedido sem canalPedido (422)", "POST", "/pedidos",
        {"unidadeId": 1, "itens": [{"produtoId": 1, "quantidade": 1}], "formaPagamento": "MOCK"}, "tokenCliente", tst=status_is(422, padrao_erro())),
    req("T15 - canalPedido inválido (422)", "POST", "/pedidos", pedido_body(canal="TELEFONE"), "tokenCliente", tst=status_is(422, padrao_erro())),
    req("T16 - Produto inexistente (404)", "POST", "/pedidos", pedido_body(prod=9999), "tokenCliente", tst=status_is(404, padrao_erro())),
    req("T17 - Estoque insuficiente (409)", "POST", "/pedidos", pedido_body(qtd=9999), "tokenCliente", tst=status_is(409, padrao_erro())),
    req("T19 - Quantidade negativa (422)", "POST", "/pedidos", pedido_body(qtd=-1), "tokenCliente", tst=status_is(422, padrao_erro())),
    req("T20 - Filtro canalPedido inválido na listagem (400)", "GET", "/pedidos?canalPedido=XYZ", token="tokenAtendente", tst=status_is(400, padrao_erro())),
    req("T21 - Produto não vendido na unidade 2 (404)", "POST", "/pedidos", pedido_body(prod=5, unidade=2), "tokenCliente", tst=status_is(404, padrao_erro())),
    req("Cozinha tenta marcar ENTREGUE (403)", "PATCH", "/pedidos/{{pedidoId}}/status", {"status": "ENTREGUE"}, "tokenCozinha", tst=status_is(403, padrao_erro())),
    req("Transição inválida ENTREGUE -> PRONTO (409)", "PATCH", "/pedidos/{{pedidoId}}/status", {"status": "PRONTO"}, "tokenAtendente", tst=status_is(409, padrao_erro())),
]))

pastas.append(("Pagamento mock - falhas", [
    criar_pedido("Setup - Criar pedido B (APP)", "pedidoB", pedido_body(canal="APP", prod=3, qtd=1)),
    req("T18 - Pagamento RECUSADO (402) -> pedido CANCELADO", "POST", "/pagamentos", {"pedidoId": "{{pedidoB}}", "simularResultado": "RECUSADO"}, "tokenCliente",
        tst=status_is(402, 'pm.test("recusado e pedido cancelado", () => { const j = pm.response.json(); pm.expect(j.status).to.eql("RECUSADO"); pm.expect(j.statusPedido).to.eql("CANCELADO"); });')),
    criar_pedido("Setup - Criar pedido C (WEB)", "pedidoC", pedido_body(canal="WEB", prod=4, qtd=1)),
    req("T22 - Gateway indisponível (202) -> pedido mantido", "POST", "/pagamentos", {"pedidoId": "{{pedidoC}}", "simularResultado": "TIMEOUT"}, "tokenCliente",
        tst=status_is(202, 'pm.test("PENDENTE e aguardando", () => { const j = pm.response.json(); pm.expect(j.status).to.eql("PENDENTE"); pm.expect(j.statusPedido).to.eql("AGUARDANDO_PAGAMENTO"); });')),
    req("T23 - Pagar pedido já cancelado (409)", "POST", "/pagamentos", {"pedidoId": "{{pedidoB}}"}, "tokenCliente", tst=status_is(409, padrao_erro())),
]))

col = {"info": {"_postman_id": str(uuid.uuid4()), "name": "Raízes do Nordeste - API Back-End",
                "description": "Execute a coleção inteira (Collection Runner) na ordem. Requer a API em http://localhost:8080 com o seed padrão.",
                "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
       "item": [{"name": n, "item": its} for n, its in pastas],
       "variable": [{"key": "baseUrl", "value": "http://localhost:8080"}]}
json.dump(col, open("postman/raizes-do-nordeste.postman_collection.json", "w"), indent=2, ensure_ascii=False)
print(sum(len(i) for _, i in pastas), "requisições")
