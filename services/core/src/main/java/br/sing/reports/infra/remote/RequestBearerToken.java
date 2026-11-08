package br.sing.reports.infra.remote;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestBearerToken {

    private RequestBearerToken() {
    }

    public static String current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String header = attrs.getRequest().getHeader("Authorization");
            if (header != null) {
                return header;
            }
        }
        return "";
    }
}
