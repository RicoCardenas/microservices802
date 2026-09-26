package com.ecommerce.auth.infrastructure.driver_adapter;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.model.gateway.UserGateway;
import com.ecommerce.auth.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

        return updatedRows > 0 ? user : null;
    }

    @Transactional
    @Override
    public boolean deleteUserId(String idUser) {
        return userDataJpaRepository.deleteUserId(idUser) > 0;
    }
}
