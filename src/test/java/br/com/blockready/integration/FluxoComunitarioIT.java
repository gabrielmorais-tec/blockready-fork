package br.com.blockready.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fluxos ponta a ponta contra PostgreSQL/PostGIS reais: autenticação, permissões por perfil,
 * recursos, disponibilidade, organizações e solicitações. Requer Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "DB_PASSWORD=teste",
        "blockready.bootstrap-admin.email=admin@teste.local",
        "blockready.bootstrap-admin.password=Admin-Teste-123"
})
class FluxoComunitarioIT {

    private static final String SENHA = "Senha-Teste-123";
    private static final String ADMIN_EMAIL = "admin@teste.local";
    private static final String ADMIN_SENHA = "Admin-Teste-123";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // ---------- helpers ----------

    private static String emailUnico(String prefixo) { return prefixo + "-" + UUID.randomUUID() + "@teste.local"; }

    private static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) { m.put((String) kv[i], kv[i + 1]); }
        return m;
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
            Object corpo, MockHttpSession sessao) throws Exception {
        req = req.with(csrf());
        if (corpo != null) { req = req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)); }
        if (sessao != null) { req = req.session(sessao); }
        return mvc.perform(req);
    }

    private JsonNode corpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private MockHttpSession login(String email, String senha) throws Exception {
        ResultActions r = enviar(post("/api/v1/auth/login"), mapa("email", email, "senha", senha), null)
                .andExpect(status().isOk());
        return (MockHttpSession) r.andReturn().getRequest().getSession(false);
    }

    private JsonNode registrarMorador(String email) throws Exception {
        return corpo(enviar(post("/api/v1/auth/register"),
                mapa("nome", "Morador Teste", "email", email, "senha", SENHA, "perfil", "MORADOR"), null)
                .andExpect(status().isCreated()));
    }

    private JsonNode registrarOrganizacao(String email, String nomeOrg) throws Exception {
        Map<String, Object> org = mapa("nome", nomeOrg, "tipo", "ONG", "responsavelNome", "Responsável Teste",
                "contatoEmail", "contato@ong.test");
        return corpo(enviar(post("/api/v1/auth/register"),
                mapa("nome", "Resp " + nomeOrg, "email", email, "senha", SENHA, "perfil", "ORGANIZACAO",
                        "organizacao", org), null).andExpect(status().isCreated()));
    }

    private MockHttpSession criarGestorELogar(MockHttpSession admin) throws Exception {
        String email = emailUnico("gestor");
        enviar(post("/api/v1/usuarios"), mapa("nome", "Gestor Teste", "email", email, "senha", SENHA,
                "perfil", "GESTOR"), admin).andExpect(status().isCreated());
        return login(email, SENHA);
    }

    private Map<String, Object> recurso(String nome) {
        return mapa("nome", nome, "categoria", "AGUA", "descricao", "Ponto de água", "endereco", "Rua A, 100",
                "latitude", -23.4538, "longitude", -46.5333, "capacidadeTotal", 1000, "capacidadeDisponivel", 800,
                "unidadeMedida", "litros", "horarioFuncionamento", "24h", "acessivel", true,
                "detalhes", mapa("potavel", true, "tipoFonte", "poço"));
    }

    private Map<String, Object> necessidade() {
        return mapa("categoria", "AGUA", "descricao", "Sem abastecimento", "quantidade", 40, "unidade", "litros",
                "endereco", "Rua das Flores, 10", "latitude", -23.45, "longitude", -46.53, "prioridade", "ALTA");
    }

    // ---------- autenticação ----------

    @Test
    void cadastroLoginEUsuarioAtual() throws Exception {
        String email = emailUnico("morador");
        registrarMorador(email);

        MockHttpSession sessao = login(email, SENHA);
        JsonNode me = corpo(mvc.perform(get("/api/v1/auth/me").session(sessao)).andExpect(status().isOk()));
        assertThat(me.get("perfil").asText()).isEqualTo("MORADOR");
        assertThat(me.has("senhaHash")).isFalse();

        enviar(post("/api/v1/auth/login"), mapa("email", email, "senha", "senha-errada"), null)
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/recursos")).andExpect(status().isUnauthorized());
        enviar(post("/api/v1/auth/register"), mapa("nome", "Outro", "email", email, "senha", SENHA, "perfil", "MORADOR"), null)
                .andExpect(status().isConflict());
        enviar(post("/api/v1/auth/register"), mapa("nome", "Hacker", "email", emailUnico("h"), "senha", SENHA,
                "perfil", "ADMIN"), null).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void requisicaoSemTokenCsrfEhRejeitada() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(mapa("email", ADMIN_EMAIL, "senha", ADMIN_SENHA))))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutEncerraASessao() throws Exception {
        String email = emailUnico("morador");
        registrarMorador(email);
        MockHttpSession sessao = login(email, SENHA);

        enviar(post("/api/v1/auth/logout"), null, sessao).andExpect(status().isNoContent());
        assertThat(sessao.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    // ---------- permissões ----------

    @Test
    void moradorNaoExecutaOperacoesAdministrativas() throws Exception {
        String email = emailUnico("morador");
        registrarMorador(email);
        MockHttpSession morador = login(email, SENHA);

        mvc.perform(get("/api/v1/auditoria").session(morador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").session(morador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/organizacoes").session(morador)).andExpect(status().isForbidden());
        enviar(post("/api/v1/recursos"), recurso("Poço"), morador).andExpect(status().isForbidden());
        enviar(post("/api/v1/recursos/1/validar"), null, morador).andExpect(status().isForbidden());
    }

    @Test
    void administradorConsultaAuditoriaEGestorNao() throws Exception {
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_SENHA);
        mvc.perform(get("/api/v1/auditoria").session(admin)).andExpect(status().isOk());

        MockHttpSession gestor = criarGestorELogar(admin);
        mvc.perform(get("/api/v1/auditoria").session(gestor)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").session(gestor)).andExpect(status().isForbidden());
    }

    // ---------- recursos e organizações ----------

    @Test
    void organizacaoAdministraApenasOsSeusRecursos() throws Exception {
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_SENHA);
        MockHttpSession gestor = criarGestorELogar(admin);

        String email1 = emailUnico("org1");
        String email2 = emailUnico("org2");
        JsonNode u1 = registrarOrganizacao(email1, "ONG Um " + UUID.randomUUID());
        JsonNode u2 = registrarOrganizacao(email2, "ONG Dois " + UUID.randomUUID());
        MockHttpSession org1 = login(email1, SENHA);
        MockHttpSession org2 = login(email2, SENHA);

        // Organização pendente não cadastra recursos.
        enviar(post("/api/v1/recursos"), recurso("Poço"), org1).andExpect(status().isUnprocessableEntity());

        enviar(post("/api/v1/organizacoes/" + u1.get("organizacaoId").asLong() + "/aprovar"), null, gestor)
                .andExpect(status().isOk());
        enviar(post("/api/v1/organizacoes/" + u2.get("organizacaoId").asLong() + "/aprovar"), null, gestor)
                .andExpect(status().isOk());

        JsonNode criado = corpo(enviar(post("/api/v1/recursos"), recurso("Poço da ONG Um"), org1)
                .andExpect(status().isCreated()));
        long id = criado.get("id").asLong();
        assertThat(criado.get("status").asText()).isEqualTo("PENDENTE_VALIDACAO");
        assertThat(criado.get("confirmado").asBoolean()).isFalse();

        // Outra organização não altera nem desativa.
        enviar(put("/api/v1/recursos/" + id), recurso("Invasão"), org2).andExpect(status().isForbidden());
        enviar(patch("/api/v1/recursos/" + id + "/disponibilidade"), mapa("capacidadeDisponivel", 1), org2)
                .andExpect(status().isForbidden());
        enviar(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/recursos/" + id), null, org2)
                .andExpect(status().isForbidden());

        // A dona atualiza a disponibilidade e o histórico é registrado.
        JsonNode atualizado = corpo(enviar(patch("/api/v1/recursos/" + id + "/disponibilidade"),
                mapa("capacidadeDisponivel", 300, "observacao", "Distribuição da manhã"), org1)
                .andExpect(status().isOk()));
        assertThat(atualizado.get("capacidadeDisponivel").decimalValue()).isEqualByComparingTo("300");
        JsonNode historico = corpo(mvc.perform(get("/api/v1/recursos/" + id + "/disponibilidades").session(org1))
                .andExpect(status().isOk()));
        assertThat(historico.get("total").asLong()).isEqualTo(2);

        // Disponibilidade maior que a capacidade total é rejeitada.
        enviar(patch("/api/v1/recursos/" + id + "/disponibilidade"), mapa("capacidadeDisponivel", 5000), org1)
                .andExpect(status().isUnprocessableEntity());

        // Pendente não aparece para morador; depois de validado, aparece como confirmado.
        String emailMorador = emailUnico("morador");
        registrarMorador(emailMorador);
        MockHttpSession morador = login(emailMorador, SENHA);
        mvc.perform(get("/api/v1/recursos/" + id).session(morador)).andExpect(status().isNotFound());

        JsonNode validado = corpo(enviar(post("/api/v1/recursos/" + id + "/validar"), null, gestor)
                .andExpect(status().isOk()));
        assertThat(validado.get("confirmado").asBoolean()).isTrue();

        JsonNode visto = corpo(mvc.perform(get("/api/v1/recursos/" + id).session(morador)).andExpect(status().isOk()));
        assertThat(visto.get("confirmado").asBoolean()).isTrue();
        assertThat(visto.get("validadoPorNome").isNull()).isTrue(); // dado de pessoa não é exposto ao morador

        // Filtros por categoria e por localização (PostGIS).
        JsonNode porRaio = corpo(mvc.perform(get("/api/v1/recursos").param("categoria", "AGUA")
                .param("lat", "-23.4538").param("lon", "-46.5333").param("raioKm", "5").session(morador))
                .andExpect(status().isOk()));
        assertThat(porRaio.get("itens").findValuesAsText("id")).contains(String.valueOf(id));
        JsonNode longe = corpo(mvc.perform(get("/api/v1/recursos").param("lat", "-3.1").param("lon", "-60.0")
                .param("raioKm", "5").session(morador)).andExpect(status().isOk()));
        assertThat(longe.get("itens").findValuesAsText("id")).doesNotContain(String.valueOf(id));

        // Desativar preserva o registro e o histórico.
        JsonNode inativo = corpo(enviar(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/recursos/" + id), null, org1).andExpect(status().isOk()));
        assertThat(inativo.get("status").asText()).isEqualTo("INATIVO");
        mvc.perform(get("/api/v1/recursos/" + id + "/historico").session(org1)).andExpect(status().isOk());
    }

    // ---------- necessidades e solicitações ----------

    @Test
    void moradorRegistraNecessidadeEAcompanhaSolicitacao() throws Exception {
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_SENHA);
        MockHttpSession gestor = criarGestorELogar(admin);

        String email = emailUnico("morador");
        registrarMorador(email);
        MockHttpSession morador = login(email, SENHA);
        String outro = emailUnico("morador");
        registrarMorador(outro);
        MockHttpSession outroMorador = login(outro, SENHA);

        JsonNode criada = corpo(enviar(post("/api/v1/necessidades"), necessidade(), morador)
                .andExpect(status().isCreated()));
        long id = criada.get("id").asLong();
        assertThat(criada.get("status").asText()).isEqualTo("ABERTA");

        JsonNode minhas = corpo(mvc.perform(get("/api/v1/solicitacoes").session(morador)).andExpect(status().isOk()));
        assertThat(minhas.get("itens").findValuesAsText("id")).contains(String.valueOf(id));
        mvc.perform(get("/api/v1/solicitacoes/" + id).session(outroMorador)).andExpect(status().isNotFound());
        enviar(post("/api/v1/necessidades"), necessidade(), gestor).andExpect(status().isForbidden());

        // Fluxo de status com validação das transições.
        enviar(patch("/api/v1/solicitacoes/" + id + "/status"), mapa("status", "ATENDIDA"), gestor)
                .andExpect(status().isUnprocessableEntity());
        enviar(patch("/api/v1/solicitacoes/" + id + "/status"), mapa("status", "EM_ANALISE"), gestor)
                .andExpect(status().isOk());
        enviar(patch("/api/v1/solicitacoes/" + id + "/status"), mapa("status", "EM_ATENDIMENTO"), gestor)
                .andExpect(status().isOk());
        enviar(patch("/api/v1/solicitacoes/" + id + "/status"), mapa("status", "ATENDIDA"), gestor)
                .andExpect(status().isUnprocessableEntity()); // sem registro do atendimento
        JsonNode atendida = corpo(enviar(patch("/api/v1/solicitacoes/" + id + "/status"),
                mapa("status", "ATENDIDA", "registroAtendimento", "40 litros entregues"), gestor)
                .andExpect(status().isOk()));
        assertThat(atendida.get("status").asText()).isEqualTo("ATENDIDA");

        // Morador só pode cancelar; histórico completo.
        enviar(patch("/api/v1/solicitacoes/" + id + "/status"), mapa("status", "EM_ANALISE"), morador)
                .andExpect(status().isForbidden());
        JsonNode historico = corpo(mvc.perform(get("/api/v1/solicitacoes/" + id + "/historico").session(morador))
                .andExpect(status().isOk()));
        assertThat(historico.size()).isEqualTo(4);

        // Dashboard lê do banco.
        JsonNode dash = corpo(mvc.perform(get("/api/v1/dashboard").session(morador)).andExpect(status().isOk()));
        assertThat(dash.get("solicitacoesAtendidas").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void voluntarioCadastraSuaDisponibilidade() throws Exception {
        String email = emailUnico("morador");
        registrarMorador(email);
        MockHttpSession sessao = login(email, SENHA);

        mvc.perform(get("/api/v1/voluntarios/me").session(sessao)).andExpect(status().isNotFound());
        enviar(put("/api/v1/voluntarios/me"), mapa("habilidades", "Primeiros socorros", "disponibilidade", "Fins de semana",
                "ativo", true), sessao).andExpect(status().isOk());
        mvc.perform(get("/api/v1/voluntarios/me").session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/voluntarios").session(sessao)).andExpect(status().isForbidden());
    }
}
