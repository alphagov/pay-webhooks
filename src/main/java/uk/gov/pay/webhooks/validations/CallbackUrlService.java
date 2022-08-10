package uk.gov.pay.webhooks.validations;

import com.google.common.net.InternetDomainName;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.webhooks.app.WebhooksConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;

public class CallbackUrlService {
    private final Set<InternetDomainName> allowedDomains;

    @Inject
    public CallbackUrlService(WebhooksConfig webhooksConfig) {
        this.allowedDomains = webhooksConfig.getLiveDataAllowDomains().stream().map(InternetDomainName::from).collect(toUnmodifiableSet());
    }

    public void validateCallbackUrl(String callbackUrl, Boolean contextIsLive) {
        var url = validateAndGetUrlIsWellFormed(callbackUrl);
        if (Boolean.TRUE.equals(contextIsLive)) {
            validateUrlIsInLiveDomains(url);
        }
    }

    private URL validateAndGetUrlIsWellFormed(String callbackUrl) {
        URL url;
        try {
            url = new URL(callbackUrl);
        } catch (MalformedURLException e) {
            throw new CallbackUrlMalformedException("Callback URL is not a valid URL");
        }

        if (StringUtils.isEmpty(url.getHost())) {
            throw new CallbackUrlMalformedException("Callback URL must contain a host");
        }

        if (!"https".equals(url.getProtocol())) {
            throw new CallbackUrlProtocolNotSupported("Callback URL must use HTTPS protocol");
        }

        if (!InternetDomainName.isValid(url.getHost())) {
            throw new CallbackUrlMalformedException("Callback URL host must be a domain name");
        }

        return url;
    }

    private void validateUrlIsInLiveDomains(URL callbackUrl) {
        var domain = InternetDomainName.from(callbackUrl.getHost());
        while (domain.hasParent()) {
            if (allowedDomains.contains(domain)) {
                return;
            }
            domain = domain.parent();
        }
        throw new CallbackUrlDomainNotOnAllowListException(callbackUrl.getHost() + " is not in the allow list");
    }
}
