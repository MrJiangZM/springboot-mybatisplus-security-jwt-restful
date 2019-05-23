package com.github.missthee.controller.login;

import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.github.missthee.db.primary.dto.login.LoginDTO;
import com.github.missthee.config.security.jwt.JavaJWT;
import com.github.missthee.db.primary.model.basic.User;
import com.github.missthee.service.interf.basic.UserService;
import com.github.missthee.service.interf.login.LoginService;
import com.github.missthee.tool.Res;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Api(tags = "登录获取用户信息、token获取用户信息")
@RestController("FunLoginController")
public class LoginController {

    private final LoginService loginService;
    private final UserService userService;
    private final JavaJWT javaJWT;

    @Autowired
    public LoginController(LoginService loginService, JavaJWT javaJWT, UserService userService) {
        this.loginService = loginService;
        this.javaJWT = javaJWT;
        this.userService = userService;
    }

    @ApiOperation(value = "登录", notes = "账号密码登录，获取token及用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "账号", required = true, dataType = "string", example = "admin"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "string", example = "123")
    })
    @PostMapping("/login")
    public Res login(HttpServletResponse httpServletResponse, HttpServletRequest httpServletRequest, @RequestBody LoginModel loginModel) throws Exception {
        String username = loginModel.getUsername();
        String password = loginModel.getPassword();
        if (StringUtils.isEmpty(username)) {
            return Res.failure("用户名不能为空");
        }
        if (StringUtils.isEmpty(password)) {
            return Res.failure("密码不能为空");
        }
        Boolean isLongLogin = loginModel.getIsLongLogin();
        User user = userService.selectOneByUsername(username);
        if (user == null) {
            return Res.failure("无此账号");
        }
        if (!user.getPassword().equals(new Md5Hash(password, user.getSalt(), 3).toString())) {
            return Res.failure("密码错误");
        }
        LoginDTO loginDTO = loginService.selectUserByUsername(username);
        httpServletResponse.setHeader("Authorization", javaJWT.createToken(loginDTO.getId(), isLongLogin ? 15 : 2));  //添加token
        return Res.success(new loginRes(loginDTO), "登录成功");
    }


    @ApiOperation(value = "获取用户信息", notes = "通过token获取用户信息，用于token有效期内的自动登录")
    @PostMapping("/info")
    @RequiresAuthentication
    public Res<loginRes> info(@RequestHeader(value = "Authorization", required = false) String token) {
        String id = javaJWT.getId(token);
        LoginDTO loginDTO = loginService.selectUserById(Integer.parseInt(id));
        return Res.success(new loginRes(loginDTO), "登录成功");
    }

    //可建立一个vo包，将类似以下两个，仅用于前端与controller传值的类集中放置
    //login的参数接收类
    @Data
    @ApiModel(description = "接收登录参数")
    private static class LoginModel {
        @ApiModelProperty(value = "账号", required = true, example = "admin")
        private String username;
        @ApiModelProperty(value = "密码", required = true, example = "123")
        private String password;
        private Boolean isLongLogin = false;
    }

    //login的结果返回类
    @Data
    @AllArgsConstructor
    private class loginRes {
        private LoginDTO user;
    }
}
