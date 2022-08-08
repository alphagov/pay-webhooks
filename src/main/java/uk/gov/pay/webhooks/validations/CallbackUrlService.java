package uk.gov.pay.webhooks.validations;

import com.google.common.net.InternetDomainName;
import org.eclipse.persistence.queries.Call;
import uk.gov.pay.webhooks.app.WebhooksConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;

public class CallbackUrlService {
    private final Set<InternetDomainName> allowedDomains;

    public CallbackUrlService(WebhooksConfig webhooksConfig) {
        this.allowedDomains = webhooksConfig.getLiveDataAllowHosts().stream().map(InternetDomainName::from).collect(toUnmodifiableSet());
    }

    public void validateCallbackUrl(String callbackUrl, Boolean contextIsLive) throws MalformedURLException {
        var url = validateAndGetUrlIsWellFormed(callbackUrl);
        if (Boolean.TRUE.equals(contextIsLive)) {
            validateUrlIsInLiveDomains(url);
        }
    }

    private URL validateAndGetUrlIsWellFormed(String callbackUrl) throws MalformedURLException {
        URL url;
        try {
            url = new URL(callbackUrl);
        } catch (MalformedURLException e) {
            throw new CallbackUrlMalformedException("Callback URL is not a valid URL");
        }

        if (url.getHost().isEmpty()) {
            throw new CallbackUrlMalformedException("Callback URL must contain a host");
        }

        if (!url.getProtocol().equals("https")) {
            throw new CallbackUrlProtocolNotSupported("Callback URL must use HTTPS protocol");
        }

        return url;
    }

    private void validateUrlIsInLiveDomains(URL callbackUrl) {
        if (InternetDomainName.isValid(callbackUrl.getHost())) {
            var domain = InternetDomainName.from(callbackUrl.getHost());
            while (domain.hasParent()) {
                if (allowedDomains.contains(domain)) {
                    return;
                }
                domain = domain.parent();
            }
        }
        throw new DomainNotOnAllowListException(callbackUrl.getHost() + " is not in the allow list");
    }
}
