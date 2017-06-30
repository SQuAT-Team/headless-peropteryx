package io.github.squat_team.performance.peropteryx.environment.uriconverter;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Intercepts URIs to be converted by the Eclipse {@link URIConverter}.
 */
public class URIConverterHandlerImrpoved extends ExtensibleURIConverterImpl {

    private static final Logger log = Logger.getLogger(URIConverterHandlerImrpoved.class.getName());

    private URIConverter delegate;

    private List<URIConverterInterceptorImrpoved> interceptors = new ArrayList<>();

    /**
     * Provide a delegate {@link URIConverter} to handle conversions not handled by the registered interceptors.
     */
    public URIConverterHandlerImrpoved(URIConverter delegate) {
        this.delegate = delegate;
    }

    /**
     * Adds an interceptor to this handlers.
     * <p>
     * Note: Interceptors will be called in the order they are added to the handler. The first interceptor that is able
     * to convert the URI will convert the URI.
     */
    public URIConverterHandlerImrpoved addInterceptor(URIConverterInterceptorImrpoved interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    @Override
    public URI normalize(URI uri) {
        URI normalized = doNormalize(uri);
        log.fine(String.format("Normalize uri '%s' to '%s'", uri, normalized));
        if (!new java.io.File(normalized.toString()).exists()) {
            log.warning("Normalized URI is not a file: " + normalized);
        }
        return normalized;
    }

    private URI doNormalize(URI uri) {
        for (URIConverterInterceptorImrpoved interceptor : this.interceptors) {
            if (interceptor.canConvert(uri)) {
                return interceptor.convert(uri);
            }
        }
        return delegate.normalize(uri);
    }
}
