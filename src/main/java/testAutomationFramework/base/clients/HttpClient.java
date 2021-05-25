package testAutomationFramework.base.clients;



import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.identityconnectors.common.security.GuardedString;
import testAutomationFramework.base.AuthenticationHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class HttpClient implements AutoCloseable{

    private static final Logger LOG = LogManager.getLogger(HttpClient.class);
    public static final int timeOut = 120;
    private final HttpClientContext httpClientContext = HttpClientContext.create();
    private final CloseableHttpClient closeableHttpClient;
    private final GuardedString userName;
    private final GuardedString password;
    private String lastRequestProtocolInformation;
    private int lastRequestStatusCode;
    private String lastRequestStatusReasonPhrase;
    private String lastRequestContentType;

    public HttpClient() {
        Configurator.setLevel("org.apache.http.client.protocol.ResponseProcessCookies", Level.ERROR);
        closeableHttpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setConnectTimeout(timeOut*1000).setConnectionRequestTimeout(timeOut*1000).setSocketTimeout(timeOut*1000).build())
                .setDefaultCookieStore(AuthenticationHelper.getCookieStore()).build();
        if(AuthenticationHelper.atcCredentialsHaveNotBeenSet()){
            userName = null;
            password = null;
        }else {
            userName = AuthenticationHelper.getAtcUserName();
            password = AuthenticationHelper.getAtcPassword();
        }
    }

    private void addBasicAuthHeader(HttpUriRequest httpUriRequest) throws AuthenticationException {
        final  StringBuilder clearUserName = new StringBuilder();
        final StringBuilder clearPassword = new StringBuilder();
        userName.access(clearUserName::append);
        password.access(clearPassword::append);
        httpUriRequest.addHeader(new BasicScheme(StandardCharsets.UTF_8).authenticate(
                new UsernamePasswordCredentials(clearUserName.toString(),clearPassword.toString()),httpUriRequest,httpClientContext
        ));
        clearUserName.delete(0,clearUserName.length()-1);
        clearUserName.setLength(0);
        clearPassword.delete(0,clearPassword.length()-1);
        clearPassword.setLength(0);
    }

    private HttpGet createGetRequest(String targetURL, boolean useBasicAuth) throws AuthenticationException {
        HttpGet httpRequest = new HttpGet(targetURL);
        httpRequest.addHeader("Content-Type","application/json");
        if(useBasicAuth){
            addBasicAuthHeader(httpRequest);
        }
        return httpRequest;
    }

    private HttpPost createPostRequest(String targetURL, boolean useBasicAuth) throws AuthenticationException {
        HttpPost httpRequest = new HttpPost(targetURL);
        if(useBasicAuth){
            addBasicAuthHeader(httpRequest);
        }
        return httpRequest;
    }

    private HttpPut createPutRequest(String targetURL, boolean useBasicAuth) throws AuthenticationException {
        HttpPut httpRequest = new HttpPut(targetURL);
        httpRequest.addHeader("Content-Type","application/json");
        if(useBasicAuth){
            addBasicAuthHeader(httpRequest);
        }
        return httpRequest;
    }

    private CloseableHttpResponse executeHttpRequest(HttpUriRequest httpUriRequest) throws IOException {
        CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpUriRequest,httpClientContext);
        lastRequestStatusCode = httpResponse.getStatusLine().getStatusCode();
        lastRequestStatusReasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
        lastRequestProtocolInformation = httpResponse.getStatusLine().getProtocolVersion().getProtocol()+"/"
                +httpResponse.getStatusLine().getProtocolVersion().getMajor()+"."+httpResponse.getStatusLine().getProtocolVersion().getMinor();
        return httpResponse;
    }

    private String getHttpEntityResultAsJson(HttpResponse httpResponse){
        String result = "";
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            lastRequestContentType = httpEntity.getContentType().toString();
            result = EntityUtils.toString(httpEntity);
            EntityUtils.consume(httpEntity);
        }catch (NullPointerException | IOException exception){}
        return result;
    }

    public String executeHttpGetRequest(String targetURL, boolean useBasicAuth){
        String result = "";
        try {
            HttpGet httpGet = createGetRequest(targetURL,useBasicAuth);
            CloseableHttpResponse closeableHttpResponse = executeHttpRequest(httpGet);
            result = getHttpEntityResultAsJson(closeableHttpResponse);
            closeableHttpResponse.close();
        }catch (IllegalStateException exception){
            LOG.warn("Credentials were already disposed! request cancelled");
        }catch (Exception e){
            LOG.error(e.getMessage() +" : "+targetURL,e);
        }
        return result;
    }

    public File executeHttpGetRequestForFile(String targetURL, Path savePath, boolean useBasicAuth) throws AuthenticationException {
        File resultFile = new File(savePath.toString());
        try {
            HttpGet httpGet = createGetRequest(targetURL, useBasicAuth);
            CloseableHttpResponse closeableHttpResponse = executeHttpRequest(httpGet);
            HttpEntity httpEntity = closeableHttpResponse.getEntity();
            lastRequestContentType = httpEntity.getContentType().toString();
            try (FileOutputStream fileOutputStream = new FileOutputStream(resultFile)) {
                httpEntity.writeTo(fileOutputStream);
                EntityUtils.consume(httpEntity);
            } catch (IOException e) {
                LOG.error("Could not write to file :" + savePath.toString(), e);
            }
        }catch (IllegalStateException exception) {
            LOG.warn("Credentials were already disposed! request cancelled");
        } catch (Exception e) {
            LOG.error(e.getMessage() + " : " + targetURL, e);
        }
        return resultFile;
    }

    public void closeHttpClient(){
        try {
            closeableHttpClient.close();
        }catch (IOException e){
            LOG.error(e.getMessage(),e);
        }
    }

    @Override
    public void close(){
        closeHttpClient();
    }
}
