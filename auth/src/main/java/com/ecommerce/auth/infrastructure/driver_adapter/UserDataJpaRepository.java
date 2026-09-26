package com.ecommerce.auth.infrastructure.driver_adapter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDataJpaRepository extends JpaRepository<UserData, String> {

    @Modifying
    @Query("""
            update UserData user
            set user.name = :name,
                user.mail = :mail,
                user.password = :password,
                user.rol = :rol,
                user.age = :age,
                user.phoneNumber = :phoneNumber
            where user.idUser = :idUser
            """)
    int updateUser(
            @Param("idUser") String idUser,
            @Param("name") String name,
            @Param("mail") String mail,
            @Param("password") String password,
            @Param("rol") String rol,
            @Param("age") Integer age,
            @Param("phoneNumber") String phoneNumber
    );

    @Modifying
    @Query("delete from UserData user where user.idUser = :idUser")
    int deleteUserId(@Param("idUser") String idUser);
}
