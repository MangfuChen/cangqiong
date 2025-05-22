package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;

    private static final String WX_LOGIN="https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Override
    public Result<UserLoginVO> login(UserLoginDTO loginDTO) {
        //向微信服务器获取openId
        String openid = getOpenid(loginDTO.getCode());
        if (openid==null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        
        //查询自己的数据库判断，是否为新用户
        User localUser = userMapper.selectOne(new QueryWrapper<User>().lambda().eq(User::getOpenid, openid));
        if (localUser==null){
            localUser = User.builder().openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(localUser);
        }

        //为用户生成jwt令牌
        Map<String, Object> claims= new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, localUser.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(localUser.getId())
                .openid(localUser.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);
    }

    private String getOpenid(String code) {
        //配置登录参数
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");

        //向微信服务器发送登录请求，获取openId
        String response = HttpClientUtil.doGet(WX_LOGIN, map);
        JSONObject jsonObject = JSON.parseObject(response);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
