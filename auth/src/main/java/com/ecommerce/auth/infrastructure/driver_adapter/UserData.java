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

    @Column(length = 20, nullable = false)
    private String mail;

    @Column(length = 12, nullable = false)
    private String password;

    private String rol;

    private Integer age;

    private String phoneNumber;


}
