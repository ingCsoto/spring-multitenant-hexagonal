package dev.cristhiansoto.catalog.infrastructure.security;

import dev.cristhiansoto.catalog.infrastructure.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code tenant_id} claim of an authenticated JWT and puts it in {@link TenantContext} for the
 * rest of the request. An authenticated caller without a valid tenant gets 403: there is no default tenant.
 */
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_CLAIM = "tenant_id";
    private static final Pattern VALID_TENANT = Pattern.compile("^[a-z0-9][a-z0-9-]{0,62}$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            chain.doFilter(request, response); // unauthenticated: let authorization answer 401
            return;
        }
        String tenant = jwtAuth.getToken().getClaimAsString(TENANT_CLAIM);
        if (tenant == null || !VALID_TENANT.matcher(tenant).matches()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"about:blank","title":"Forbidden","status":403,"detail":"The token has no valid tenant_id claim"}""");
            return;
        }
        try {
            TenantContext.set(tenant);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
