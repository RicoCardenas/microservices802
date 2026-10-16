package com.ecommerce.auth.infrastructure.entry_points;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.model.UserResult;
import com.ecommerce.auth.domain.usecase.UseCaseUser;
import com.ecommerce.auth.infrastructure.driver_adapter.UserData;
import com.ecommerce.auth.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/ecommerce/user")
@RequiredArgsConstructor
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UseCaseUser useCaseUser;
    private final UserMapper userMapper;

    @PostMapping("/save")
    public ResponseEntity<OperationResponse> saveUser(@RequestBody UserData userData){
        return respond(useCaseUser.saveUser(userMapper.toUser(userData)));
    }

    @PostMapping("/login")
    public ResponseEntity<OperationResponse> login(@RequestBody LoginRequest loginRequest) {
        return respond(useCaseUser.login(loginRequest.mail(), loginRequest.password()));
    }

    @GetMapping("/search/{idUser}")
    public ResponseEntity<OperationResponse> searchUserId(@PathVariable String idUser) {
        return respond(useCaseUser.searchUserId(idUser));
    }

    @PutMapping("/update")
    public ResponseEntity<OperationResponse> updateUser(@RequestBody UserData userData) {
        return respond(useCaseUser.updateUser(userMapper.toUser(userData)));
    }

    @DeleteMapping("/delete/{idUser}")
    public ResponseEntity<OperationResponse> deleteUserId(@PathVariable String idUser) {
        return respond(useCaseUser.deleteUserId(idUser));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<OperationResponse> handleValidationError(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new OperationResponse(false, exception.getMessage(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<OperationResponse> handleInvalidBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new OperationResponse(
                false, "Debe contener todos los atributos por favor, tanto usuario como contraseña", null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<OperationResponse> handleTechnicalError(RuntimeException exception) {
        LOGGER.error("Error técnico al procesar la operación de usuario", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OperationResponse(
                false, "No fue posible procesar la solicitud por un error interno", null));
    }

    private ResponseEntity<OperationResponse> respond(UserResult result) {
        UserResponse user = result.getUser() == null ? null : UserResponse.from(result.getUser());
        return ResponseEntity.ok(new OperationResponse(result.isSuccess(), result.getMessage(), user));
    }

    private record OperationResponse(boolean success, String message, UserResponse user) {
    }
    private record UserResponse(
            String idUser,
            String name,
            String mail,
            String rol,
            Integer age,
            String phoneNumber,
            BigDecimal saldo
    ) {

        private static UserResponse from(User user) {
            return new UserResponse(
                    user.getIdUser(),
                    user.getName(),
                    user.getMail(),
                    user.getRol(),
                    user.getAge(),
                    user.getPhoneNumber(),
                    BigDecimal.valueOf(user.getSaldo(), 2)
            );
        }
    }

    public record LoginRequest(String mail, String password) {
    }
}
