package com.tiyiyun.sso.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * @Author yandongxu
 * @Date Created in 17:50 2018/6/4
 * @Modified by
 */
public class Des3 {

	/*
	 * public static void main(String[] args) throws Exception {
	 * 
	 * 
	 * System.out.println("CBC加密解密"); String str5 =
	 * des3EncodeCBC("telefenpaytes@pay17$#3#$", "13386191", "12345"); String str6 =
	 * des3DecodeCBC("test@telefen.com~!@#$250", "88888888",
	 * "2NOKo9V3EROqXw25Qlgy9g=="); System.out.println(str5);
	 * System.out.println(str6);
	 * 
	 * 
	 * String content = "SWX|rxSpxXtCsVzgs9IFTdq1qNZk|1594085209176"; String key =
	 * "rxSpxXtCsVzgs9IFTdq1qNZk"; System.out.println("content:" + content); String
	 * s1 = des3EncodeCBC(key, "efYbr6E5", content);
	 * System.out.println(s1.length()); System.out.println("加密:" + s1);
	 * System.out.println(des3DecodeCBC(key, "efYbr6E5", s1));
	 * System.out.println(System.currentTimeMillis()); }
	 */

	public static void main(String[] args) throws Exception {
		String str = "Z/TmREGkogxxESk5mWAyvU7epVs0tfXML/crLDAGaHz3TxywSWjY2ZOslJM9vqc7qhlAeaDN6MkBCF5+ivl4FgmRcJM6dYsOcTg12+X0xWscT9tiJDxT/QiHtZVP7dH2oUE+yVZnRDW+8G7FM6nhB0qHp6q8uN8MiEqJtEQuJSbs7j8vjcpIHp65gzbhnYWUyu0IlHquiAvl0KGWDK/hvWAKyDdtH4xnbTV0UjSdfm69hgPEGErkp34dczYOTeOl71WmfaTcdt71qeYlN9b8NozzVTornEsjJMYIkbimibm1MBxO43uVGnUtQHlhlyArQC9u2ugKKeWym1CFcX6VkluDq8a6NsIjmgK62bF4dvUHYIiI8oH7tt9Yqaihp0iU57Pc3B7fOLerDk7uz4Xd4TnnVfxOvQ8tJ46z1ekCoFJia5AcOG8nG9c6H95Z8BASe8i0qSJ/e5jfU8VeLuYKvDYjMFxmEdpA6Mcs3ywT68U=";
		String des3DecodeCBC = Des3.des3DecodeCBC("test@telefen.com~!@#$250", "88888888", str);
		System.out.println(des3DecodeCBC);
	}

	public static String des3EncodeCBC(String key, String keyiv, String data) throws Exception {
		byte[] str5 = des3EncodeCBC(key.getBytes(StandardCharsets.UTF_8), keyiv.getBytes(StandardCharsets.UTF_8),
				data.getBytes(StandardCharsets.UTF_8));
		return Base64.encodeBase64String(str5);
	}

	public static String des3DecodeCBC(String key, String keyiv, String str) throws Exception {
		byte[] data = des3DecodeCBC(key.getBytes(StandardCharsets.UTF_8), keyiv.getBytes(StandardCharsets.UTF_8),
				Base64.decodeBase64(str));
		// return new String(data);/*卡密或含有中文返回 return new String(data,"GBK");*/
		return new String(data, StandardCharsets.UTF_8.name());
	}

	/**
	 * ECB加密,不要IV
	 *
	 * @param key  密钥
	 * @param data 明文
	 * @return Base64编码的密文
	 * @throws Exception
	 */
	public static byte[] des3EncodeECB(byte[] key, byte[] data) throws Exception {
		Key deskey = null;
		DESedeKeySpec spec = new DESedeKeySpec(key);
		SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
		deskey = keyfactory.generateSecret(spec);
		Cipher cipher = Cipher.getInstance("desede" + "/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, deskey);
		byte[] bOut = cipher.doFinal(data);
		return bOut;
	}

	/**
	 * ECB解密,不要IV
	 *
	 * @param key  密钥
	 * @param data Base64编码的密文
	 * @return 明文
	 * @throws Exception
	 */
	public static byte[] ees3DecodeECB(byte[] key, byte[] data) throws Exception {
		Key deskey = null;
		DESedeKeySpec spec = new DESedeKeySpec(key);
		SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
		deskey = keyfactory.generateSecret(spec);
		Cipher cipher = Cipher.getInstance("desede" + "/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, deskey);
		byte[] bOut = cipher.doFinal(data);
		return bOut;
	}

	/**
	 * CBC加密
	 *
	 * @param key   密钥
	 * @param keyiv IV
	 * @param data  明文
	 * @return Base64编码的密文
	 * @throws Exception
	 */
	public static byte[] des3EncodeCBC(byte[] key, byte[] keyiv, byte[] data) throws Exception {
		Key deskey = null;
		DESedeKeySpec spec = new DESedeKeySpec(key);
		SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
		deskey = keyfactory.generateSecret(spec);
		Cipher cipher = Cipher.getInstance("desede" + "/CBC/PKCS5Padding");
		IvParameterSpec ips = new IvParameterSpec(keyiv);
		cipher.init(Cipher.ENCRYPT_MODE, deskey, ips);
		byte[] bOut = cipher.doFinal(data);
		return bOut;
	}

	/**
	 * CBC解密
	 *
	 * @param key   密钥
	 * @param keyiv IV
	 * @param data  Base64编码的密文
	 * @return 明文
	 * @throws Exception
	 */
	public static byte[] des3DecodeCBC(byte[] key, byte[] keyiv, byte[] data) throws Exception {
		Key deskey = null;
		DESedeKeySpec spec = new DESedeKeySpec(key);
		SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
		deskey = keyfactory.generateSecret(spec);
		Cipher cipher = Cipher.getInstance("desede" + "/CBC/PKCS5Padding");
		IvParameterSpec ips = new IvParameterSpec(keyiv);
		cipher.init(Cipher.DECRYPT_MODE, deskey, ips);
		byte[] bOut = cipher.doFinal(data);
		return bOut;
	}
}
