package uk.gov.pay.webhooks.message;

import org.apache.http.client.methods.HttpPost;

import java.net.URI;

public class HttpPostFactory {

    public HttpPost newHttpPost(URI uri) {
        return new HttpPost(uri);
    }

}
