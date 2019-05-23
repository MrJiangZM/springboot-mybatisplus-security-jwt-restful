package com.github.missthee.controller.example;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.missthee.tool.datastructure.TreeData;
import com.github.missthee.tool.excel.imports.ExcelImport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.*;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.github.missthee.config.security.jwt.JavaJWT;
import com.github.missthee.db.primary.model.basic.User;
import com.github.missthee.db.primary.model.compute.Compute;
import com.github.missthee.service.interf.basic.UserService;
import com.github.missthee.service.interf.compute.ComputeService;

import com.github.missthee.tool.FileRec;
import com.github.missthee.tool.Res;
import springfox.documentation.annotations.ApiIgnore;

import javax.naming.SizeLimitExceededException;
import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


@ApiIgnore
//权限访问测试
@RestController
@RequestMapping("/test")
public class ExampleController {
    private final UserService userService;
    private final JavaJWT javaJWT;
    private final ComputeService computeService;

    @Autowired
    public ExampleController(UserService userService, JavaJWT javaJWT, ComputeService computeService) {

        this.userService = userService;
        this.javaJWT = javaJWT;
        this.computeService = computeService;
    }

    @PostMapping("error")
    public Res error() throws Exception {
        throw new Exception("A unknown exception");
    }

    @PostMapping("error1")
    public Res error1() throws Exception {
        Integer.parseInt("zz");
        return Res.success();
    }

    //获取当前用户相关信息。
    @PostMapping("infoByHeader")
    public Res<Map<String, Object>> getInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        String userIdByToken = javaJWT.getId(token);//通过token解析获得
        Object userIdBySubject = SecurityUtils.getSubject().getPrincipal();//通过shiro的subject获得
        Map<String, Object> map = new HashMap<String, Object>() {{
            put("userIdByToken", userIdByToken);
            put("userIdBySubject", userIdBySubject);
        }};
        return Res.success(map);
    }

    //groupby测试(非标准扩展方法，不建议使用)。
    @PostMapping("groupBy")
    public Res<List<Compute>> getInfo() {
        List<Compute> computeList = computeService.selectGroupBy();
        return Res.success(computeList);
    }

    @PostMapping("addUser")
    public Res<JSONObject> addUser(@RequestBody JSONObject bJO) {
        String username = bJO.getString("username");
        String password = bJO.getString("password");
        String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
        User user = new User();
        user.setUsername(username);
        String md5Password = new Md5Hash(password, salt, 3).toString();
        user.setPassword(md5Password);
        user.setNickname(username);
        user.setSalt(salt);
        int result = userService.insertOne(user);
        JSONObject jO = new JSONObject() {{
            put("result", result);
            put("user", user);
        }};
        return Res.res(result > 0, jO);
    }

    @PostMapping("alterUser")
    public Res<JSONObject> alterUser(@RequestBody JSONObject bJO) {
        Integer id = bJO.getInteger("id");
        int result = userService.alterOne(id);
        JSONObject jO = new JSONObject() {{
            put("result", result);
        }};
        return Res.res(result > 0, jO);
    }

    @RequestMapping("tree")
    public Res<JSONArray> getTree(@RequestParam("c") Boolean compareSelfId, @RequestParam("r") Integer rootId) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<TreeItem> li = new ArrayList<TreeItem>() {{
            add(new TreeItem(1, "name1", null));
            add(new TreeItem(2, "name2", 1));
            add(new TreeItem(3, "name3", 2));
            add(new TreeItem(4, "name4", 2));
            add(new TreeItem(5, "name5", null));
            add(new TreeItem(6, "name6", 2));
        }};
        JSONArray objects = TreeData.tree(li, rootId, compareSelfId == null ? false : compareSelfId, new HashMap<>());
        return Res.success(objects);
    }

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    private class TreeItem {
        private Integer id;
        private String name;
        private Integer parentId;
    }

    //-----以下为权限测试，若需测试权限功能，需将本controller访问url先加入到shiro的检测路径中。-----
    @RequestMapping("/everyone")
    public Res everyone() {
        return Res.success("WebController：everyone");
    }

    @RequestMapping("/user")
    @RequiresUser//因为订制rememberMe功能，作用同@RequiresAuthentication
    public Res user() {
        return Res.success("WebController：user");
    }

    @RequestMapping("/guest")
    @RequiresGuest
    public Res guest() {
        return Res.success("WebController：guest");
    }

    @RequestMapping("/require_auth")
    @RequiresAuthentication
    public Res requireAuth() {
        return Res.success("WebController：You are authenticated");
    }

    @RequestMapping("/require_role12")
    @RequiresRoles({"role1", "role2"})
    public Res requireRole1() {
        return Res.success("WebController：You are visiting require_role12 [role1&role2]");
    }

    @RequestMapping("/require_role3")
    @RequiresRoles("role3")
    public Res requireRole3() {
        return Res.success("WebController：You are visiting require_role [role3]");
    }

    @RequestMapping("/require_role_permission")
    @RequiresPermissions({"admin:view"})
    public Res requireRolePermission() {
        return Res.success("WebController：You are visiting permission require admin:view");
    }

    @RequestMapping("/require_permission")
    @RequiresPermissions({"view", "edit"})
    public Res requirePermission() {
        return Res.success("WebController：You are visiting permission require edit,view");
    }
    //-------------------------------------------------------------------------------------------------

    //上传文件示例
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Res fileUpload(@RequestParam("file") MultipartFile file) throws FileNotFoundException, SizeLimitExceededException {
        String path = FileRec.fileUpload(file, "uploadTest");
        if (path != null) {
            return Res.success(path, "成功");
        } else {
            return Res.failure("失败");
        }
    }

    //上传文件示例
    @PostMapping(value = "/upload1")
    public Res fileUpload1(MultipartFile file, String customPath) throws FileNotFoundException, SizeLimitExceededException {
        String path = FileRec.fileUpload(file, customPath);
        if (path != null) {
            return Res.success(path, "成功");
        } else {
            return Res.failure("失败");
        }
    }

    //上传excel转为POJO
    @PostMapping(value = "/upload2")
    public Res fileUpload2(MultipartFile file) throws IOException, NoSuchMethodException, InvalidFormatException, IllegalAccessException, InstantiationException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException {
        List<Object> objects = ExcelImport.excel2POJOList(file, User.class, new ArrayList<String>() {{
            add("nickname");
            add("username");
        }});
        return Res.success(objects);
    }
}