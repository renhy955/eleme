package com.cx.springboot02.common.utils;



import com.cx.springboot02.common.redis.RedisOperator;
import com.cx.springboot02.pojo.Customer;
import com.cx.springboot02.service.impl.CustomerServiceImpl;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtUtils {

    @Autowired
    RedisOperator redisOperator;

    @Autowired
    CustomerServiceImpl iCustomerService;

    /**
     * 01为登录token时效期
     * 01为验证码时效期
     */
    private static final long EXPIRE_TIME01 =30*60*1000;
    private static final long EXPIRE_TIME02=60*1000;
    private static final long EXPIRE_TIME03= 30L*24*60*60*1000;
    /**
     * 加密密钥
     */
    private static final String KEY = "oss";

    /**
     * 生成账号密码的token
     *
     * @return
     */
    public static String createToken(String name,String password,String identity){
        Map<String,Object> header = new HashMap<>();
        Map<String,Object> Claims = new HashMap<>();

        Claims.put("name", name);
        Claims.put(("password"), password);
        Claims.put(("identity"), identity);

        header.put("typ","JWT");
        header.put("alg","HS256");
        //setID:用户ID
        //setExpiration:token过期时间  当前时间+有效时间
        //setSubject:用户名
        //setIssuedAt:token创建时间
        //signWith:加密方式
        JwtBuilder builder = Jwts.builder()
                .setClaims(Claims)
                .setHeader(header)
                .setId(name)
                .setExpiration(new Date(System.currentTimeMillis()+ EXPIRE_TIME03))
                .setSubject(name)
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, KEY.getBytes(StandardCharsets.UTF_8));
        return builder.compact();
    }

    /**
     * 验证token是否有效
     * @param token  请求头中携带的token
     * @return  token验证结果  2-token过期；1-token认证通过；0-token认证失败
     */
    public Customer verify(String token){
        if(token==null){
            return null;
        }
        Claims claims = null;
        try {
            claims = Jwts.parser().setSigningKey(KEY.getBytes(StandardCharsets.UTF_8)).parseClaimsJws(token).getBody();
        }catch (Exception e){
          return null;
        }
        //从token中获取用户name，在服务器的map当查询是否当前用户是否token有效
        String name = claims.get("name", String.class);
        String token2 = redisOperator.get(name);
        if(token2!=null && token2.equals(token)){
            Customer customer = iCustomerService.selectCustomerByName(name);
            return customer;
        }else{
            return null;
        }
    }

    /**
     * 获取jwt-token里面的值
     * @param token
     * @param key
     * @return
     */
    public static String getValue(String token,String key){
        if(token==null){
            return null;
        }
        Claims claims = null;
        try {
            claims = Jwts.parser().setSigningKey(KEY.getBytes(StandardCharsets.UTF_8)).parseClaimsJws(token).getBody();
        }catch (Exception e){
            return null;
        }
        return claims.get(key, String.class);
    }

    public static void main(String[] args) {
//        String jwt = createToken("123","123");
//        System.out.println(jwt);
        String jwt1 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWI" +
                "iOiIxMjMiLCJwYXNzd29yZCI6IjEyMyIsIm5hbWUiOiIxMj" +
                "MiLCJleHAiOjE2NjkxNzEzMTAsImlhdCI6MTY2NjU3OTMxM" +
                "CwianRpIjoiMTIzIn0.9Fl_OgShRxZtz1iW5BUT6IvR4nKgBL01KiCr2U9mcLc";
        String jwt2 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI" +
                "1NiJ9.eyJzdWIiOiIxMjMiLCJwYXNzd29yZCI6IjEyMy" +
                "IsIm5hbWUiOiIxMjMiLCJleHAiOjE2NjkxNjg2ODIsImlhdCI6MT" +
                "Y2NjU3NjY4MiwianRpIjoiMTIzIn0.YXPE98yzwjk" +
                "IkFbzHPp1EAwUB7NOVSZvsaeviMFyPT0";
        String jwt3 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJwYXNzd29yZCI6IjEyMyIsIm5hbWUiOiIxMjMiLCJleHAiOjE2NjkxNjg2ODIsImlhdCI6MTY2NjU3NjY4MiwianRpIjoiMTIzIn0.YXPE98yzwjkIkFbzHPp1EAwUB7NOVSZvsaeviMFyPT0";
        System.out.println(getValue(jwt3,"name"));
        System.out.println(getValue(jwt3,"password"));
        System.out.println(jwt2.equals(jwt3));

    }

    public  Customer checkingByHttp(HttpServletRequest request){
        Long id = -1L;
        try {

            String token = request.getHeader("token");
            if(token==null || token.equals("")) {
                return null;
            }
            Customer customer = verify(token);
            if(customer!=null){
                return customer;
            }else{
                return null;
            }
        } catch (Exception exception) {
            return null;
        }

    }
}
