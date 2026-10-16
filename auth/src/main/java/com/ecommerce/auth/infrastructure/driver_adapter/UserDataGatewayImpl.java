package com.ecommerce.auth.infrastructure.driver_adapter;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.model.gateway.UserGateway;
import com.ecommerce.auth.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class UserDataGatewayImpl implements UserGateway {

    private final UserDataJpaRepository userDataJpaRepository;
    private final UserMapper userMapper;

    @Override
    public User saveUser(User user) {
        UserData userMapping = userMapper.toUserData(user);

        return userMapper.toUser(userDataJpaRepository.save(userMapping));
    }

    @Override
    public User searchUserId(String idUser) {
        return userDataJpaRepository.findById(idUser)
                .map(userMapper::toUser)
                .orElse(null);
    }

    @Override
    public User searchUserMail(String mail) {
        List<UserData> users = userDataJpaRepository.findAllByMail(mail);
        if (users.size() != 1) {
            return null;
        }

        return userMapper.toUser(users.get(0));
    }

    @Override
    public boolean emailExists(String mail) {
        return userDataJpaRepository.existsByMail(mail);
    }

    @Override
    public boolean emailExistsForAnotherUser(String mail, String idUser) {
        return userDataJpaRepository.existsByMailAndIdUserNot(mail, idUser);
    }

    @Transactional
    @Override
    public User updateUser(User user) {
        UserData userMapping = userMapper.toUserData(user);
        int updatedRows = userDataJpaRepository.updateUser(
                userMapping.getIdUser(),
                userMapping.getName(),
                userMapping.getMail(),
                userMapping.getPassword(),
                userMapping.getRol(),
                userMapping.getAge(),
                userMapping.getPhoneNumber()
        );

        return updatedRows > 0 ? searchUserId(user.getIdUser()) : null;
    }

    @Transactional
    @Override
    public boolean deleteUserId(String idUser) {
        return userDataJpaRepository.deleteUserId(idUser) > 0;
    }
}
