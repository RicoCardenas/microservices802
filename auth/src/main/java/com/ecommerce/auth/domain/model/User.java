package com.ecommerce.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter

public class User {

    private String idUser;
    private String name;
    private String mail;
    private String password;
    private String rol;
    private Integer age;
    private String phoneNumber;
    // Valores monetarios internos en centavos.
    private long saldo;

    public User(String idUser, String name, String mail, String password, String rol, Integer age, String phoneNumber) {
        this(idUser, name, mail, password, rol, age, phoneNumber, 0);
    }

}
