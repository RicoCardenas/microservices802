package com.ecommerce.auth;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.model.gateway.UserGateway;
import com.ecommerce.auth.domain.usecase.UseCaseUser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthApplicationTests {

    @Test
    void searchesAUserById() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        User user = user("1");
        gateway.users.put(user.getIdUser(), user);
        UseCaseUser useCaseUser = new UseCaseUser(gateway);

        assertSame(user, useCaseUser.searchUserId("1"));
    }

    @Test
    void updatesOnlyAnExistingUser() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        gateway.users.put("1", user("1"));
        UseCaseUser useCaseUser = new UseCaseUser(gateway);
        User updatedUser = user("1");
        updatedUser.setName("Nombre actualizado");

        assertSame(updatedUser, useCaseUser.updateUser(updatedUser));
        assertSame(updatedUser, gateway.users.get("1"));
    }

    @Test
    void doesNotUpdateAnUnknownUser() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        UseCaseUser useCaseUser = new UseCaseUser(gateway);

        assertNull(useCaseUser.updateUser(user("1")));
        assertFalse(gateway.updated);
    }

    @Test
    void deletesOnlyAnExistingUser() {
        InMemoryUserGateway gateway = new InMemoryUserGateway();
        gateway.users.put("1", user("1"));
        UseCaseUser useCaseUser = new UseCaseUser(gateway);

        assertTrue(useCaseUser.deleteUserId("1"));
        assertFalse(useCaseUser.deleteUserId("1"));
    }

    private static User user(String idUser) {
        return new User(idUser, "Nombre", "user@example.com", "password", "USER", 18, "3000000000");
    }

    private static class InMemoryUserGateway implements UserGateway {

        private final Map<String, User> users = new HashMap<>();
        private boolean updated;

        @Override
        public User saveUser(User user) {
            users.put(user.getIdUser(), user);
            return user;
        }

        @Override
        public User searchUserId(String idUser) {
            return users.get(idUser);
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
