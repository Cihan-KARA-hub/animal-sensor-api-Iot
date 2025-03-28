package com.yelman.cloudserver.api;

import com.yelman.cloudserver.model.Users;
import com.yelman.cloudserver.services.UserServices;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/user")
public class UserController {
    private final UserServices userServices;

    public UserController(UserServices userServices) {
        this.userServices = userServices;
    }


    @PostMapping("")
    public HttpStatus addUser(@RequestBody Users user) {
        boolean empty = userServices.registerUser(user);
        if (empty) {
            return HttpStatus.CREATED;
        } else {
            return HttpStatus.BAD_REQUEST;
        }
    }
}
