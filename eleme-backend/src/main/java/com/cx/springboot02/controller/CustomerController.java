package com.cx.springboot02.controller;


import com.cx.springboot02.common.E.AuthorizeType;
import com.cx.springboot02.common.E.ResultCode;
import com.cx.springboot02.common.JsonResult;
import com.cx.springboot02.common.ResultTool;
import com.cx.springboot02.common.redis.RedisOperator;
import com.cx.springboot02.common.redis.loginCache.LoginCache;
import com.cx.springboot02.common.utils.*;
import com.cx.springboot02.common.utils.Logindatecode.IVerifyCodeGen;
import com.cx.springboot02.common.utils.Logindatecode.SimpleCharVerifyCodeGenImpl;
import com.cx.springboot02.common.utils.Logindatecode.VerifyCode;
import com.cx.springboot02.pojo.Customer;
import com.cx.springboot02.service.impl.CustomerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;


/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 陈翔
 * @since 2022-10-05
 */
@RestController
@RequestMapping("/customer")
@PreAuthorize(values = {AuthorizeType.CUSTOMER})
@Slf4j
public class CustomerController {
    //获取主机端口
    @Value("${server.port}")
    private String POST;
    //静态资源对外暴露的访问路径
    @Value("${file.staticAccessPath}")
    private String staticAccessPath;
    //实际存储路径
    @Value("${file.uploadFolder}")
    private String uploadFolder;
    //图片
    @Value("${file.uploadImage}")
    private String uploadImage;

    @Autowired
    RedisOperator redisOperator;

    @Autowired
    LoginCache loginCache;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    CustomerServiceImpl customerService;
    //登录请求

    @GetMapping("/verifyCode")
    public JsonResult<String> verifyCode(HttpServletRequest request, HttpServletResponse response) {
        IVerifyCodeGen iVerifyCodeGen = new SimpleCharVerifyCodeGenImpl();
        try {
            //设置长宽
            VerifyCode verifyCode = iVerifyCodeGen.generate(80, 28);
            String code = verifyCode.getCode();
            log.info(code);
            //将VerifyCode绑定session
            request.getSession().setAttribute("VerifyCode", code);
            UUID uuid = UUID.randomUUID();
            String newFileName = uuid.toString().replaceAll("-", "").toUpperCase() + ".png";
            System.out.println(newFileName);
            //把图片存入服务器
            FileTool.uploadFiles(verifyCode.getImgBytes(),uploadFolder+uploadImage,newFileName);
            //返回图片url
            return ResultTool.success("http://127.0.0.1:8080/boot/file/image"+"/"+newFileName);
        } catch (IOException e) {
            return ResultTool.fail(ResultCode.COMMON_FAIL);

        }
    }


    static class user{
        private String name;
        private String password;

        @Override
        public String toString() {
            return "user{" +
                    "name='" + name + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }
    @PostMapping("/login")
    @Unobstructed
    public JsonResult<Customer> login(@RequestBody Map<String, String> mp, HttpServletResponse response,HttpServletRequest request) {
        String name = (String) mp.get("name");
        String password = (String) mp.get("password");
        String code = (String) mp.get("code");
        int name1 = Integer.parseInt(mp.get("name"));
        if(StringUtil.isBlank(code) || StringUtil.isBlank(password) || StringUtil.isBlank(name)) {
//            return new JsonResult<>(false, ResultCode.PARAM_IS_BLANK);
            return ResultTool.fail(ResultCode.PARAM_IS_BLANK);
        }
        String  code_session = (String) request.getSession().getAttribute("VerifyCode");
        System.out.println("code_in_session:"+code_session);
        if(!code.equals(code_session)){
            return ResultTool.fail(ResultCode.LOGIN_CODE_ERROR);
        }
        Customer customer = customerService.getCustomerByAAndP(name, password);
        if (customer != null) {
            //删除之前的token
            loginCache.deleteCustomer(customer.getId());
            //生成token
            String token = JwtUtils.createToken(name, password, "customer");
            //存入redis
            loginCache.saveCustomerToken(token, customer);
            //放入请求头
            response.addHeader("Access-Control-Expose-Headers", "token");
            response.addHeader("token", token);
            System.out.println(response.getHeader("token"));
            customer.setPassword("");
            return ResultTool.success(customer);
        } else {
            return ResultTool.fail(ResultCode.USER_CREDENTIALS_ERROR);
        }
    }

    @GetMapping("/{name}")
    public JsonResult<Object> getCustomer(@PathVariable String name, HttpServletRequest request) {
        return ResultTool.success(null);
    }

    @PostMapping("/sendRcode")
    @Unobstructed
    public JsonResult<Object> sendRcode(@RequestBody Map<String, Object> mp, HttpServletResponse response) {
        String email = (String) mp.get("email");
        //生成验证码
        String Rcode = StringUtil.getCharAndNum(4);
        SendEmail.sendRegisterEmail(email, Rcode);
            //存入redis 5*60s
            redisOperator.set("Rcode" + email, Rcode, 60);
            return ResultTool.success(Rcode);
//            return ResultTool.fail(ResultCode.CODE_CANT_SEND);
    }


    @PostMapping("/register")
    @Unobstructed
    public JsonResult<Object> register(@RequestBody Map<String, Object> mp) {
        String name = (String) mp.get("name");
        String password1 = (String) mp.get("password1");
        String password2 = (String) mp.get("password2");
        String email = (String) mp.get("email");
        String code = (String) mp.get("code");
        if(name==null || password1==null || password2==null || email==null || code==null) {
            return ResultTool.fail(ResultCode.FIELD_IS_EMPTY);
        }
        boolean b1 = password1.equals(password2);
        boolean b2 = code.equals(redisOperator.get("Rcode" + email));
        boolean b3 = customerService.selectCustomerByEmail(email).size() == 0;
        if (b1 && b2 && b3) {
            Customer customer = new Customer();
            customer.setName(name);
            customer.setEmail(email);
            customer.setPassword(password1);
            //持久化
            customerService.register(customer);
            return ResultTool.success();
        } else if(b1){
            //密码不一致
            return ResultTool.fail(ResultCode.PASSWORD_NO_ONE);
        }else if(b2){
            //验证码错误
            return ResultTool.fail(ResultCode.CODE_ERROR);
        }else{
            //邮箱已经被使用
            return ResultTool.fail(ResultCode.EMAIL_HAVE_USERD);
        }


    }


    @PostMapping("/sendFcode")
    @Unobstructed
    public JsonResult<Object> sendFcode(@RequestBody Map<String, Object> mp) {
        String email = (String) mp.get("email");
        //生成验证码
        String Rcode = StringUtil.getCharAndNum(4);

        if (SendEmail.sendRegisterEmail(email, Rcode)) {
            //存入redis 5*60s
            redisOperator.set("Fcode" + email, Rcode, 300);
            return ResultTool.success(Rcode);
        } else {
            return ResultTool.fail(ResultCode.CODE_CANT_SEND);
        }
    }


    @PostMapping("/forget")
    @Unobstructed
    public JsonResult<Object> forget(@RequestBody Map<String, Object> mp) {

        String name = (String) mp.get("name");
        String password1 = (String) mp.get("password1");
        String password2 = (String) mp.get("password2");
        String email = (String) mp.get("email");
        String code = (String) mp.get("code");
        if (password1.equals(password2) && code != null && code.equals(redisOperator.get("Fcode" + email))
                && customerService.selectCustomerByEmail(email).size() == 1) {
            Customer customer = new Customer();
            customer.setName(name);
            customer.setEmail(email);
            customer.setPassword(password1);
            //持久化
            customerService.forget(email, password1);
            return ResultTool.success();
        } else {
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        }
    }


    @PostMapping("/image")
    @Unobstructed
    public JsonResult<Object> uploadImage(@RequestParam("file") MultipartFile file,Long id) {
        try {
            if (file.isEmpty()) {
                System.out.println("file为空");
                return ResultTool.fail(ResultCode.COMMON_FAIL);
            }
            //获取文件名
            String oldName = file.getOriginalFilename();
            System.out.println("oldName:"+oldName);
            int lastindex = oldName.lastIndexOf(".");
            if(lastindex==-1){
                //没有后缀名
                System.out.println("无后缀名");
                return ResultTool.fail(ResultCode.COMMON_FAIL);
            }
            String suffix = oldName.substring(lastindex);
            //生成一个随机文件名
            UUID uuid = UUID.randomUUID();
            String newFileName = uuid.toString().replaceAll("-", "").toUpperCase() + suffix;
            System.out.println(newFileName);
            FileTool.uploadFiles(file.getBytes(),uploadFolder+uploadImage,newFileName);
            if(id!=null)customerService.setImage(id,"http://127.0.0.1:8080/boot/file/image"+"/"+newFileName);
            return ResultTool.success("http://127.0.0.1:8080/boot/file/image"+"/"+newFileName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultTool.fail(ResultCode.COMMON_FAIL);
        }
    }




    @PostMapping("/outlogin")
    @Unobstructed
    public JsonResult<Object> outlogin(HttpServletRequest request) {
        Long id = WebUtil.getObjectParameter(request, "id", Long.class);
        return null;
    }




}

