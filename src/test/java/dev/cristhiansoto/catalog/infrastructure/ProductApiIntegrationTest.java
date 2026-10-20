package dev.cristhiansoto.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end through the real filter chain, Hibernate and a real Postgres. The point of this class is
 * the isolation contract: one tenant can never see or touch another tenant's rows.
 */
@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:0/unused")
@AutoConfigureMockMvc
@Testcontainers
class ProductApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM products");
    }

    @Test
    void aTenantOnlySeesItsOwnProducts() throws Exception {
        String acmeId = create(writer("acme"), "Coffee", 1500);
        create(writer("globex"), "Coffee", 2000); // same name, other tenant: allowed

        mvc.perform(get("/api/products").with(reader("acme")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(acmeId))
                .andExpect(jsonPath("$[0].priceCents").value(1500));

        mvc.perform(get("/api/products/" + acmeId).with(reader("globex")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aTenantCannotUpdateOrDeleteAnotherTenantsProduct() throws Exception {
        String acmeId = create(writer("acme"), "Coffee", 1500);

        mvc.perform(put("/api/products/" + acmeId).with(writer("globex"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"priceCents\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/products/" + acmeId).with(writer("globex")))
                .andExpect(status().isNotFound());

        Map<String, Object> row = jdbc.queryForMap("SELECT tenant_id, price_cents FROM products WHERE id = ?::uuid", acmeId);
        assertThat(row).containsEntry("tenant_id", "acme").containsEntry("price_cents", 1500L);
    }

    @Test
    void newRowsAreStampedWithTheCallersTenant() throws Exception {
        String id = create(writer("acme"), "Tea", 900);
        assertThat(jdbc.queryForObject("SELECT tenant_id FROM products WHERE id = ?::uuid", String.class, id))
                .isEqualTo("acme");
    }

    @Test
    void ownerCanUpdateAndDelete() throws Exception {
        String id = create(writer("acme"), "Tea", 900);
        mvc.perform(put("/api/products/" + id).with(writer("acme"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Green tea\",\"priceCents\":1100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Green tea"))
                .andExpect(jsonPath("$.priceCents").value(1100));
        mvc.perform(delete("/api/products/" + id).with(writer("acme"))).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/" + id).with(reader("acme"))).andExpect(status().isNotFound());
    }

    @Test
    void requestsWithoutTokenAreUnauthorized() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokensWithoutAValidTenantAreForbidden() throws Exception {
        mvc.perform(get("/api/products").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(get("/api/products").with(jwt().jwt(j -> j.claim("tenant_id", "ACME; DROP"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void readersCannotWrite() throws Exception {
        mvc.perform(post("/api/products").with(reader("acme"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Coffee\",\"priceCents\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateNameInTheSameTenantIsAConflict() throws Exception {
        create(writer("acme"), "Coffee", 1500);
        mvc.perform(post("/api/products").with(writer("acme"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Coffee\",\"priceCents\":1}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void invalidBodiesAreRejectedAsProblemDetails() throws Exception {
        mvc.perform(post("/api/products").with(writer("acme"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"priceCents\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private String create(JwtRequestPostProcessor who, String name, long priceCents) throws Exception {
        String body = json.writeValueAsString(Map.of("name", name, "priceCents", priceCents));
        String response = mvc.perform(post("/api/products").with(who).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }

    private static JwtRequestPostProcessor reader(String tenant) {
        return jwt().jwt(j -> j.claim("tenant_id", tenant));
    }

    private static JwtRequestPostProcessor writer(String tenant) {
        return jwt().jwt(j -> j.claim("tenant_id", tenant))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_catalog-writer")));
    }
}
