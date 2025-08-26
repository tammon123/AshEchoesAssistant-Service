package info.qianqiu.ashechoes.utils.http;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@NoArgsConstructor
public class ResttemplateLogClientInterceptor implements ClientHttpRequestInterceptor {

    private boolean reqLog;
    private boolean resLog;

    ResttemplateLogClientInterceptor(boolean reqLog, boolean resLog) {
        this.reqLog = reqLog;
        this.resLog = resLog;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        tranceRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        traceResponse(response);
        return response;
    }

    private void tranceRequest(HttpRequest request, byte[] body) throws UnsupportedEncodingException {
        if (this.reqLog)
            log.debug("{};bd:{},hd:{}", request.getURI(),
                    new String(body, StandardCharsets.UTF_8), request.getHeaders());
        else
            log.debug("{};bd:{},hd:{}", request.getURI(),
                    "ignore", request.getHeaders());
    }

    private void traceResponse(ClientHttpResponse httpResponse) throws IOException {
        if (this.resLog) {
            StringBuilder inputStringBuilder = new StringBuilder();
            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(httpResponse.getBody(), StandardCharsets.UTF_8));
            String line = bufferedReader.readLine();
            while (line != null) {
                inputStringBuilder.append(line);
                line = bufferedReader.readLine();
            }
            log.debug("bd:{};sc:{};hd:{};", inputStringBuilder, httpResponse.getStatusCode(), httpResponse.getHeaders());
        }
    }
}
