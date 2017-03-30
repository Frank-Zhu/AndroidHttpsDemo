package com.frankzhu.androidhttpsdemo;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;


/**
 * Author:    ZhuWenWu
 * Version    V1.0
 * Date:      2014/12/15  16:19.
 * Description:
 * Modification  History:
 * Date         	Author        		Version        	Description
 * -----------------------------------------------------------------------------------
 * 2014/12/15        ZhuWenWu            1.0                    1.0
 * Why & What is modified:
 */
public class HttpClientSslHelper {
    private static final String KEY_STORE_TYPE_BKS = "bks";
    private static final String KEY_STORE_TYPE_P12 = "PKCS12";

    /**
     * 记得添加相应的证书到assets目录下面
     */
    public static final String KEY_STORE_CLIENT_PATH = "xxx.p12";//P12文件
    private static final String KEY_STORE_TRUST_PATH = "xxx.truststore";//truststore文件
    public static final String KEY_STORE_PASSWORD = "123456";//P12文件密码
    private static final String KEY_STORE_TRUST_PASSWORD = "123456";//truststore文件密码

    public static final String KEY_CRT_CLIENT_PATH = "xxx.crt";//CRT文件

    public static boolean isServerTrusted = false;

    public static OkHttpClient getSslOkHttpClient(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(getSslContextByCustomTrustManager(context).getSocketFactory())
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        Log.d("HttpClientSslHelper", "hostname = " + hostname);
//                        return isServerTrusted;//如果是全部自己校验逻辑的，需要根据证书状态返回相应的校验结果
                        if ("yourhost".equals(hostname)) {
                            return session.isValid();
                        } else {
                            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                            return hv.verify(hostname, session);
                        }
                    }
                });
        return builder.build();
    }

    private static SSLContext sslContext = null;

    public static SSLContext getSslContext(Context context) {
        if (sslContext == null) {
            try {
                // 服务器端需要验证的客户端证书
                KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE_P12);
                // 客户端信任的服务器端证书
                KeyStore trustStore = KeyStore.getInstance(KEY_STORE_TYPE_BKS);

                InputStream ksIn = context.getResources().getAssets().open(KEY_STORE_CLIENT_PATH);
                InputStream tsIn = context.getResources().getAssets().open(KEY_STORE_TRUST_PATH);
                try {
                    keyStore.load(ksIn, KEY_STORE_PASSWORD.toCharArray());
                    trustStore.load(tsIn, KEY_STORE_TRUST_PASSWORD.toCharArray());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        ksIn.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        tsIn.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                sslContext = SSLContext.getInstance("TLS");
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
                keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sslContext;
    }

    public static SSLContext getSslContextByDefaultTrustManager(Context context) {
        if (sslContext == null) {
            try {
                // 服务器端需要验证的客户端证书
                KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE_P12);

                InputStream ksIn = context.getResources().getAssets().open(KEY_STORE_CLIENT_PATH);
                try {
                    keyStore.load(ksIn, KEY_STORE_PASSWORD.toCharArray());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        ksIn.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                sslContext = SSLContext.getInstance("TLS");
                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
                keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());
                sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sslContext;
    }

    public static SSLContext getSslContextByCustomTrustManager(Context context) {
        if (sslContext == null) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(context.getResources().getAssets().open(KEY_CRT_CLIENT_PATH));
                final Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                } finally {
                    caInput.close();
                }

                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new X509TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                        Log.d("HttpClientSslHelper", "checkClientTrusted --> authType = " + authType);
                        //校验客户端证书
                    }

                    public void checkServerTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                        Log.d("HttpClientSslHelper", "checkServerTrusted --> authType = " + authType);
                        //校验服务器证书
                        for (X509Certificate cert : chain) {
                            cert.checkValidity();
                            try {
                                cert.verify(ca.getPublicKey());
                                isServerTrusted = true;
                            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                                e.printStackTrace();
                                isServerTrusted = false;
                            }
                        }
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sslContext;
    }
}
