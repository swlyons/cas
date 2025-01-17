package org.apereo.cas.support.oauth.web.response.callback;

import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.web.endpoints.OAuth20ConfigurationContext;

import lombok.RequiredArgsConstructor;
import org.pac4j.core.context.WebContext;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * This is {@link BaseOAuth20AuthorizationResponseBuilder}.
 *
 * @author Misagh Moayyed
 * @since 6.4.0
 */
@RequiredArgsConstructor
public abstract class BaseOAuth20AuthorizationResponseBuilder<T extends OAuth20ConfigurationContext> implements OAuth20AuthorizationResponseBuilder {

    /**
     * Configuration context.
     */
    protected final T configurationContext;
    
    /**
     * Response customizer.
     */
    protected final OAuth20AuthorizationModelAndViewBuilder authorizationModelAndViewBuilder;

    @Override
    public ModelAndView build(final WebContext context,
                              final OAuthRegisteredService registeredService,
                              final String redirectUrl,
                              final Map<String, String> parameters) {
        return authorizationModelAndViewBuilder.build(context, registeredService, redirectUrl, parameters);
    }
}
