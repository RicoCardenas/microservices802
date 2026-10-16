package com.ecommerce.auth.infrastructure.driver_adapter;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "users")

public class UserData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String idUser;

    private String name;

    @Column(length = 40, nullable = false, unique = true)
    private String mail;

    @Column(length = 60, nullable = false)
    private String password;

    private String rol;

    private Integer age;

    private String phoneNumber;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long saldo;

    public UserData(String idUser, String name, String mail, String password, String rol, Integer age, String phoneNumber) {
        this(idUser, name, mail, password, rol, age, phoneNumber, 0);
    }


}
