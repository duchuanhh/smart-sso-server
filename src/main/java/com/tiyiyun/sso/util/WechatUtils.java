package com.tiyiyun.sso.util;
 
 
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.springframework.beans.factory.annotation.Value;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.smart.sso.client.entity.WxUserInfoEntity;
import com.tiyiyun.sso.model.WechatTokenModel;

import lombok.extern.slf4j.Slf4j;
 

@Slf4j
public class WechatUtils {
	
	@Value("${wx.appid}")
    private static String appid; //凭证
	
	@Value("${wx.appsecret}")
    private static String appsecret; //凭证密钥
 
    // 凭证获取（GET）
    public final static String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code";
    //userinfo
    public final static String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
 
    /**
              *  获取接口访问凭证
     *
     * @param code app授权后传回
     * @return
     */
    public static WechatTokenModel getToken(String code) {
    	WechatTokenModel token = new WechatTokenModel();
//        long now = new Date().getTime();
//        if (tokenTime != 0 && now - tokenTime < 7000000) {//token有效时间 7e6 毫秒
//            return token;
//        }
        String requestUrl = tokenUrl.replace("APPID", appid).replace("SECRET", appsecret).replace("CODE", code);
        // 发起GET请求获取凭证
        JSONObject jsonObject = httpsRequest(requestUrl, "GET", null);
        if (null != jsonObject) {
            try {
                token.setUnionId(jsonObject.getString("unionid"));
                token.setOpenid(jsonObject.getString("openid"));
                token.setAccessToken(jsonObject.getString("access_token"));
                token.setRefreshToken(jsonObject.getString("refresh_token"));
                token.setExpiresIn(jsonObject.getIntValue("expires_in"));
            } catch (JSONException e) {
                token = null;
                // 获取token失败
                log.error("获取token失败 errcode:{} errmsg:{}", jsonObject.getIntValue("errcode"), jsonObject.getString("errmsg"));
            }
        }
        return token;
    }
 
    public static void main(String args[]) {
        // 获取接口访问凭证
    	// 我这里是直接从前端传入accessToken，openid，所以可以直接拿到，下面是通过code生成
    	WechatTokenModel tokenModel = getToken("code");
        String accessToken = tokenModel.getAccessToken();
        String openid = tokenModel.getOpenid();
 
 
 
//        /**
//         * 获取用户信息
//         */
        WxUserInfoEntity user = getUserInfo(accessToken, openid);
        //做这个测试的时候可以先关注，或者取消关注，控制台会打印出来此用户的openid
        System.out.println("OpenID：" + user.getOpenId());
        System.out.println("昵称：" + user.getNickName());
        System.out.println("性别：" + user.getSex());
        System.out.println("国家：" + user.getCountry());
        System.out.println("省份：" + user.getProvince());
        System.out.println("城市：" + user.getCity());
        System.out.println("头像：" + user.getHeadImgUrl());
        getToken("02315v0w35TA603fsF0w3Q0fov415v0B");
 
 
    }
 
    /**
     * 获取用户信息
     *
     * @param accessToken 接口访问凭证
     * @param openId      用户标识
     * @return WeixinUserInfo
     */
    public static WxUserInfoEntity getUserInfo(String accessToken, String openId) {
 
        WxUserInfoEntity WxUserInfoEntity = null;
        // 拼接请求地址
        String requestUrl = userInfoUrl.replace("ACCESS_TOKEN", accessToken).replace("OPENID", openId);
        // 获取用户信息
        JSONObject jsonObject = httpsRequest(requestUrl, "GET", null);
        if (null != jsonObject) {
            try {
                WxUserInfoEntity = new WxUserInfoEntity();
                // 用户的标识
                WxUserInfoEntity.setOpenId(jsonObject.getString("openid"));
                WxUserInfoEntity.setUnionid(jsonObject.getString("unionid"));
                // 关注状态（1是关注，0是未关注），未关注时获取不到其余信息
                // 用户关注时间
                //WxUserInfoEntity.setSubscribeTime(jsonObject.getString("subscribe_time"));
                // 昵称
                WxUserInfoEntity.setNickName(jsonObject.getString("nickname"));
                // 用户的性别（1是男性，2是女性，0是未知）
               WxUserInfoEntity.setSex(jsonObject.getIntValue("sex"));
                // 用户所在国家
                WxUserInfoEntity.setCountry(jsonObject.getString("country"));
                // 用户所在省份
                WxUserInfoEntity.setProvince(jsonObject.getString("province"));
                // 用户所在城市
                WxUserInfoEntity.setCity(jsonObject.getString("city"));
                // 用户的语言，简体中文为zh_CN
               // WxUserInfoEntity.language;(jsonObject.getString("language"));
                // 用户头像
                WxUserInfoEntity.setHeadImgUrl(jsonObject.getString("headimgurl"));
            } catch (Exception e) {
                int errorCode = jsonObject.getIntValue("errcode");
                String errorMsg = jsonObject.getString("errmsg");
//                System.err.printf("获取用户信息失败 errcode:{} errmsg:{}", errorCode, errorMsg);
            }
        }
        return WxUserInfoEntity;
    }
 
    /**
             * 发送https请求
     *
     * @param requestUrl    请求地址
     * @param requestMethod 请求方式（GET、POST）
     * @param outputStr     提交的数据
     * @return JSONObject(通过JSONObject.get ( key)的方式获取json对象的属性值)
     */
    public static JSONObject httpsRequest(String requestUrl, String requestMethod, String outputStr) {
        JSONObject jsonObject = null;
        try {
            // 创建SSLContext对象，并使用我们指定的信任管理器初始化
            TrustManager[] tm = {new WechatTrustManager()};
            SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
            sslContext.init(null, tm, new java.security.SecureRandom());
            // 从上述SSLContext对象中得到SSLSocketFactory对象
            SSLSocketFactory ssf = sslContext.getSocketFactory();
 
            URL url = new URL(requestUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(ssf);
 
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            // 设置请求方式（GET/POST）
            conn.setRequestMethod(requestMethod);
 
            // 当outputStr不为null时向输出流写数据
            if (null != outputStr) {
                OutputStream outputStream = conn.getOutputStream();
                // 注意编码格式
                outputStream.write(outputStr.getBytes(StandardCharsets.UTF_8));
                outputStream.close();
            }
 
            // 从输入流读取返回内容
            InputStream inputStream = conn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String str = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((str = bufferedReader.readLine()) != null) {
                stringBuilder.append(str);
            }
 
            // 释放资源
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            inputStream = null;
            conn.disconnect();
            jsonObject = JSONObject.parseObject(stringBuilder.toString());
        } catch (ConnectException ce) {
            System.err.printf("连接超时：{}", ce);
        } catch (Exception e) {
            System.err.printf("https请求异常：{}", e);
        }
        return jsonObject;
    }
}
 
 