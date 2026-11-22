package br.edu.raizes.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Raízes do Nordeste - API Back-End", version = "1.0.0",
        description = "Pedidos multicanal, estoque por unidade, pagamento mock, fidelidade e LGPD"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    private static final Set<String> SEM_403 = Set.of("GET /usuarios/me", "DELETE /usuarios/me",
            "GET /produtos", "GET /produtos/{id}", "GET /promocoes");

    private static final Set<String> COM_404 = Set.of("POST /pedidos", "GET /pedidos/{id}", "PATCH /pedidos/{id}/status",
            "POST /pedidos/{id}/cancelar", "POST /pagamentos", "GET /pagamentos", "POST /estoque/movimentos",
            "GET /estoque", "GET /unidades/{id}/cardapio", "GET /produtos/{id}", "PUT /produtos/{id}",
            "DELETE /produtos/{id}");

    private static final Set<String> COM_409 = Set.of("POST /pedidos", "POST /pagamentos", "POST /pedidos/{id}/cancelar",
            "PATCH /pedidos/{id}/status", "POST /estoque/movimentos", "POST /auth/register", "POST /usuarios",
            "POST /promocoes");

    /** Documenta em todos os endpoints os códigos de erro e o padrão único de erro (ErroResponse). */
    @Bean
    public OpenApiCustomizer respostasDeErro() {
        return openApi -> {
            if (openApi.getComponents() == null) openApi.setComponents(new Components());
            openApi.getComponents().addSchemas("ErroResponse", new ObjectSchema()
                    .addProperty("timestamp", new StringSchema().format("date-time").example("2026-11-22T15:30:00"))
                    .addProperty("status", new IntegerSchema().example(422))
                    .addProperty("codigo", new StringSchema().example("VALIDACAO"))
                    .addProperty("mensagem", new StringSchema().example("Dados inválidos"))
                    .addProperty("path", new StringSchema().example("/pedidos"))
                    .addProperty("detalhes", new ArraySchema().items(new StringSchema())
                            .example(List.of("canalPedido: não deve ser nulo"))));
            if (openApi.getPaths() == null) return;
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((metodo, op) -> {
                String chave = metodo.name() + " " + path;
                boolean seguro = op.getSecurity() != null && !op.getSecurity().isEmpty();
                boolean corpo = op.getRequestBody() != null;
                boolean params = op.getParameters() != null && !op.getParameters().isEmpty();
                if (corpo || params) adicionar(op, path, 400, "CORPO_INVALIDO", "Requisição malformada ou parâmetro inválido");
                if (seguro || chave.equals("POST /auth/login")) adicionar(op, path, 401, "NAO_AUTENTICADO", "Token ausente/inválido ou credenciais incorretas");
                if (seguro && !SEM_403.contains(chave)) adicionar(op, path, 403, "ACESSO_NEGADO", "Perfil sem permissão");
                if (COM_404.contains(chave)) adicionar(op, path, 404, "NAO_ENCONTRADO", "Recurso não encontrado");
                if (COM_409.contains(chave)) adicionar(op, path, 409, "CONFLITO", "Conflito de regra de negócio (ex.: estoque insuficiente)");
                if (corpo) adicionar(op, path, 422, "VALIDACAO", "Dados inválidos (validação)");
            }));
        };
    }

    private void adicionar(Operation op, String path, int status, String codigo, String descricao) {
        String chave = String.valueOf(status);
        if (op.getResponses() == null || op.getResponses().containsKey(chave)) return;
        Map<String, Object> exemplo = new LinkedHashMap<>();
        exemplo.put("timestamp", "2026-11-22T15:30:00");
        exemplo.put("status", status);
        exemplo.put("codigo", codigo);
        exemplo.put("mensagem", descricao);
        exemplo.put("path", path);
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/ErroResponse");
        op.getResponses().addApiResponse(chave, new ApiResponse().description(descricao)
                .content(new Content().addMediaType("application/json", new MediaType().schema(ref).example(exemplo))));
    }
}
