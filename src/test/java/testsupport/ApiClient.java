package testsupport;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simuliert einen Browser für die API-Tests: hält die Sitzung (Session-Cookie)
 * und sendet bei schreibenden Anfragen das CSRF-Token mit – genau wie das
 * echte Frontend in api.js.
 */
public final class ApiClient {

    private final MockMvc mvc;
    private final MockHttpSession session = new MockHttpSession();
    private Cookie csrfCookie;

    public ApiClient(MockMvc mvc) {
        this.mvc = mvc;
    }

    /** Anonymer Besucher. */
    public static ApiClient anonymous(MockMvc mvc) {
        return new ApiClient(mvc);
    }

    /** Meldet sich über den echten Login-Endpunkt an. */
    public static ApiClient loggedIn(MockMvc mvc, String username, String password) throws Exception {
        ApiClient client = new ApiClient(mvc);
        client.post("/api/login", credentials(username, password))
                .andExpect(status().isOk());
        return client;
    }

    public static String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    public ResultActions get(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).session(session));
    }

    public ResultActions post(String url, String json) throws Exception {
        return mvc.perform(withCsrf(MockMvcRequestBuilders.post(url)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions post(String url) throws Exception {
        return mvc.perform(withCsrf(MockMvcRequestBuilders.post(url)));
    }

    public ResultActions patch(String url, String json) throws Exception {
        return mvc.perform(withCsrf(MockMvcRequestBuilders.patch(url)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions delete(String url) throws Exception {
        return mvc.perform(withCsrf(MockMvcRequestBuilders.delete(url)));
    }

    /** Schreibende Anfrage ohne CSRF-Token, z. B. von einer fremden Webseite. */
    public ResultActions postWithoutCsrf(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        if (csrfCookie == null) {
            csrfCookie = mvc.perform(MockMvcRequestBuilders.get("/api/auth/csrf").session(session))
                    .andReturn().getResponse().getCookie("XSRF-TOKEN");
        }
        return request.session(session)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue());
    }
}
