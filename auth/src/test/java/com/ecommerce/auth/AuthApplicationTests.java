package com.ecommerce.auth;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.model.UserResult;
import com.ecommerce.auth.domain.model.gateway.UserGateway;
import com.ecommerce.auth.domain.usecase.UseCaseUser;
import com.ecommerce.auth.infrastructure.driver_adapter.EncrypterGatewayImpl;
import com.ecommerce.auth.infrastructure.entry_points.UserController;
import com.ecommerce.auth.infrastructure.mapper.UserMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApplicationTests {

    private final EncrypterGatewayImpl encrypter = new EncrypterGatewayImpl();

    @Test
    void searchesAUserById() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        User user = user("1");
        gateway.users.put(user.getIdUser(), user);
        UseCaseUser useCaseUser = new UseCaseUser(gateway, encrypter);

        assertSame(user, useCaseUser.searchUserId("1").getUser());
    }

    @Test
    void updatesOnlyAnExistingUser() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        gateway.users.put("1", user("1"));
        UseCaseUser useCaseUser = new UseCaseUser(gateway, encrypter);
        User updatedUser = user("1");
        updatedUser.setName("Nombre actualizado");

        assertSame(updatedUser, useCaseUser.updateUser(updatedUser).getUser());
        assertSame(updatedUser, gateway.users.get("1"));
        assertTrue(encrypter.checkPass("password", updatedUser.getPassword()));
        assertTrue(useCaseUser.login(updatedUser.getMail(), "password").isSuccess());
    }

    @Test
    void doesNotUpdateAnUnknownUser() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        UseCaseUser useCaseUser = new UseCaseUser(gateway, encrypter);

        assertFalse(useCaseUser.updateUser(user("1")).isSuccess());
        assertFalse(gateway.updated);
    }

    @Test
    void deletesOnlyAnExistingUser() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        gateway.users.put("1", user("1"));
        UseCaseUser useCaseUser = new UseCaseUser(gateway, encrypter);

        assertTrue(useCaseUser.deleteUserId("1").isSuccess());
        assertFalse(useCaseUser.deleteUserId("1").isSuccess());
    }

    @Test
    void savesAnAdultAndAuthenticatesTheStoredHash() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        UseCaseUser useCaseUser = new UseCaseUser(gateway, encrypter);
        User user = user("1");
        user.setMail(" user@example.com ");

        UserResult result = useCaseUser.saveUser(user);

        assertTrue(result.isSuccess());
        assertEquals("user@example.com", result.getUser().getMail());
        assertNotEquals("password", result.getUser().getPassword());
        assertTrue(useCaseUser.login(" user@example.com ", "password").isSuccess());
        assertFalse(useCaseUser.login("user@example.com", "incorrecta").isSuccess());
        assertFalse(useCaseUser.login("unknown@example.com", "password").isSuccess());
    }

    @Test
    void rejectsAMinorWithoutSavingOrChangingThePassword() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        UseCaseUser useCaseUser = new UseCaseUser(gateway, encrypter);
        User user = user("1");
        user.setAge(17);

        UserResult result = useCaseUser.saveUser(user);

        assertFalse(result.isSuccess());
        assertEquals("El usuario debe ser mayor de edad", result.getMessage());
        assertNull(result.getUser());
        assertTrue(gateway.users.isEmpty());
        assertEquals("password", user.getPassword());
    }

    @Test
    void rejectsDuplicateMailBeforeSavingOrChangingThePassword() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        gateway.users.put("1", user("1"));
        User user = user("2");
        UseCaseUser useCaseUser = new UseCaseUser(gateway, encrypter);

        assertFalse(useCaseUser.saveUser(user).isSuccess());
        assertEquals(1, gateway.users.size());
        assertEquals("password", user.getPassword());
    }

    @Test
    void doesNotUpdateAMinorOrAnotherUsersMail() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        gateway.users.put("1", user("1"));
        User otherUser = user("2");
        otherUser.setMail("other@example.com");
        gateway.users.put("2", otherUser);
        UseCaseUser useCaseUser = new UseCaseUser(gateway, encrypter);
        User update = user("1");
        update.setAge(17);

        assertFalse(useCaseUser.updateUser(update).isSuccess());
        update.setAge(18);
        update.setMail(otherUser.getMail());
        assertFalse(useCaseUser.updateUser(update).isSuccess());
        assertFalse(gateway.updated);
        assertEquals("password", update.getPassword());
    }

    @Test
    void rejectsMissingFieldsAndInvalidIdentifiers() {
        UseCaseUser useCaseUser = new UseCaseUser(new InMemoryUserGateway(), encrypter);
        User user = user("1");
        user.setAge(null);

        assertThrows(IllegalArgumentException.class, () -> useCaseUser.saveUser(null));
        assertThrows(IllegalArgumentException.class, () -> useCaseUser.saveUser(user));
        assertThrows(IllegalArgumentException.class, () -> useCaseUser.login(" ", "password"));
        assertThrows(IllegalArgumentException.class, () -> useCaseUser.searchUserId(" "));
        assertThrows(IllegalArgumentException.class, () -> useCaseUser.deleteUserId(null));
    }

    @Nested
    class HttpRequests {

        private static final String BASE = "/api/ecommerce/user";
        private static final String ADULT = """
                {"name":"Nombre","mail":"user@example.com","password":"password",
                 "rol":"USER","age":18,"phoneNumber":"3000000000"}
                """;
        private final InMemoryUserGateway gateway = new InMemoryUserGateway();
        private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(
                new UseCaseUser(gateway, encrypter), new UserMapper())).build();

        @Test
        void registersAndLogsInWithoutExposingThePassword() throws Exception {
            mvc.perform(post(BASE + "/save").contentType(MediaType.APPLICATION_JSON).content(ADULT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.user.idUser").value("1"))
                    .andExpect(jsonPath("$.user.password").doesNotExist());

            mvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mail\":\"user@example.com\",\"password\":\"password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.user.password").doesNotExist());
        }

        @Test
        void returns200WhenRegistrationIsRejectedByAgeOrDuplicateMail() throws Exception {
            mvc.perform(post(BASE + "/save").contentType(MediaType.APPLICATION_JSON)
                            .content(ADULT.replace("\"age\":18", "\"age\":17")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("El usuario debe ser mayor de edad"));
            assertTrue(gateway.users.isEmpty());

            gateway.users.put("1", user("1"));
            mvc.perform(post(BASE + "/save").contentType(MediaType.APPLICATION_JSON).content(ADULT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Ya existe un usuario registrado con ese correo"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"incorrecta", "password"})
        void returnsTheSameBusinessRejectionForInvalidCredentials(String password) throws Exception {
            if (password.equals("incorrecta")) {
                User user = user("1");
                user.setPassword(encrypter.encrypt("password"));
                gateway.users.put("1", user);
            }
            mvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mail\":\"user@example.com\",\"password\":\"" + password + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"))
                    .andExpect(jsonPath("$.user").isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "null", "{", "{}", "{\"mail\":\"user@example.com\",\"password\":\" \"}"})
        void returns400ForMissingOrMalformedLoginData(String body) throws Exception {
            mvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @ParameterizedTest
        @ValueSource(strings = {"null", "\"texto\""})
        void returns400ForMissingOrInvalidAge(String age) throws Exception {
            mvc.perform(post(BASE + "/save").contentType(MediaType.APPLICATION_JSON)
                            .content(ADULT.replace("\"age\":18", "\"age\":" + age)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void searchesUpdatesAndDeletesUsingTheExistingHttpMethods() throws Exception {
            gateway.users.put("1", user("1"));
            mvc.perform(get(BASE + "/search/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.user.password").doesNotExist());
            mvc.perform(put(BASE + "/update").contentType(MediaType.APPLICATION_JSON)
                            .content(ADULT.replace("{", "{\"idUser\":\"1\",")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.user.password").doesNotExist());
            assertTrue(encrypter.checkPass("password", gateway.users.get("1").getPassword()));
            mvc.perform(delete(BASE + "/delete/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            assertTrue(gateway.users.isEmpty());
        }

        @Test
        void returns200ForAnUnknownUserInSearchUpdateAndDelete() throws Exception {
            mvc.perform(get(BASE + "/search/unknown"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
            mvc.perform(put(BASE + "/update").contentType(MediaType.APPLICATION_JSON)
                            .content(ADULT.replace("{", "{\"idUser\":\"unknown\",")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
            mvc.perform(delete(BASE + "/delete/unknown"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void preserves405And415ForUnsupportedHttpRequests() throws Exception {
            mvc.perform(get(BASE + "/login")).andExpect(status().isMethodNotAllowed());
            mvc.perform(post(BASE + "/login").contentType(MediaType.TEXT_PLAIN).content("texto"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void returns500ForDatabaseAndUnexpectedFailuresWithoutExposingDetails() throws Exception {
            for (RuntimeException failure : new RuntimeException[]{
                    new DataAccessResourceFailureException("jdbc:private-database"),
                    new DataIntegrityViolationException("private-constraint"),
                    new IllegalStateException("private-internal-detail")}) {
                gateway.failure = failure;
                mvc.perform(post(BASE + "/save").contentType(MediaType.APPLICATION_JSON).content(ADULT))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value(
                                "No fue posible procesar la solicitud por un error interno"));
            }
        }
    }

    private static User user(String idUser) {
        return new User(idUser, "Nombre", "user@example.com", "password", "USER", 18, "3000000000");
    }

    private static class InMemoryUserGateway implements UserGateway {

        private final Map<String, User> users = new HashMap<>();
        private boolean updated;
        private RuntimeException failure;

        @Override
        public User saveUser(User user) {
            if (failure != null) {
                throw failure;
            }
            if (user.getIdUser() == null) {
                user.setIdUser("1");
            }
            users.put(user.getIdUser(), user);
            return user;
        }

        @Override
        public User searchUserId(String idUser) {
            return users.get(idUser);
        }

        @Override
        public User searchUserMail(String mail) {
            User match = null;
            for (User user : users.values()) {
                if (user.getMail().equals(mail)) {
                    if (match != null) {
                        return null;
                    }
                    match = user;
                }
            }
            return match;
        }

        @Override
        public boolean emailExists(String mail) {
            return users.values().stream().anyMatch(user -> user.getMail().equals(mail));
        }

        @Override
        public boolean emailExistsForAnotherUser(String mail, String idUser) {
            return users.values().stream()
                    .anyMatch(user -> !user.getIdUser().equals(idUser) && user.getMail().equals(mail));
        }

        @Override
        public User updateUser(User user) {
            if (!users.containsKey(user.getIdUser())) {
                return null;
            }
            updated = true;
            users.put(user.getIdUser(), user);
            return user;
        }

        @Override
        public boolean deleteUserId(String idUser) {
            return users.remove(idUser) != null;
        }
    }
}
