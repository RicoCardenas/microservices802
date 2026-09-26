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

}
