package com.tiyiyun.sso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "ali")
public class AliPayProperty {

    /**  支付宝appID  */
    public String appId;

    /**  商户私钥，您的 PKCS8 格式 RSA2 私钥 (使用 支付宝开放平台开发助手 生成的应用私钥)  */
    public String merchantPrivateKey ;

    /**  支付宝公钥,查看地址：https://openhome.alipay.com 对应 appID 下的支付宝公钥。  */
    public String alipayPublicKey;

    /**  接口格式规范   */
    public String format;

    /**  签名方式  */
    public String signType;

    /**  字符编码格式   */
    public String charset;

    /**  支付宝网关  https://openapi.alipay.com/gateway.do 这是正式地址  */
    public String gatewayUrl;
    
    /**  支付宝客户端  */
    public AlipayClient getAlipayClient(){
        AlipayClient alipayClient = new DefaultAlipayClient(
                this.gatewayUrl,
                this.appId,
                this.merchantPrivateKey,
                this.format,
                this.charset,
                this.alipayPublicKey,
                this.signType);
        return alipayClient;
    }
}