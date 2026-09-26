package com.ecommerce.auth.infrastructure.entry_points;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.usecase.UseCaseUser;
import com.ecommerce.auth.infrastructure.driver_adapter.UserData;
import com.ecommerce.auth.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ecommerce/user")
@RequiredArgsConstructor

public class UserController {

    private final UseCaseUser useCaseUser;
    private final UserMapper userMapper;

    @PostMapping("/save")
    public ResponseEntity<UserResponse> saveUser(@RequestBody UserData userData){
        User user = userMapper.toUser(userData);
        User validatedUserSaved = useCaseUser.saveUser(user);

        if (validatedUserSaved != null && validatedUserSaved.getIdUser() != null) {
            return new ResponseEntity<>(UserResponse.from(validatedUserSaved), HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }


    @GetMapping("/search/{idUser}")
    public ResponseEntity<UserResponse> searchUserId(@PathVariable String idUser) {
        User user = useCaseUser.searchUserId(idUser);

        if (user != null) {
            return new ResponseEntity<>(UserResponse.from(user), HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateUser(@RequestBody UserData userData) {
        User user = userMapper.toUser(userData);
        User updatedUser = useCaseUser.updateUser(user);

        if (updatedUser != null) {
            return new ResponseEntity<>(UserResponse.from(updatedUser), HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/delete/{idUser}")
    public ResponseEntity<Void> deleteUserId(@PathVariable String idUser) {
        if (useCaseUser.deleteUserId(idUser)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    private record UserResponse(
            String idUser,
            String name,
            String mail,
            String rol,
            Integer age,
            String phoneNumber
    ) {

        private static UserResponse from(User user) {
            return new UserResponse(
                    user.getIdUser(),
                    user.getName(),
                    user.getMail(),
                    user.getRol(),
                    user.getAge(),
                    user.getPhoneNumber()
            );
        }
    }
}
