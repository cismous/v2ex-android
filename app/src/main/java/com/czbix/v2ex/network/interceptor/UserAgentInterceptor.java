package com.czbix.v2ex.network.interceptor;

import com.czbix.v2ex.BuildConfig;
import com.google.common.net.HttpHeaders;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class UserAgentInterceptor implements Interceptor {
    public static final String USER_AGENT = "V2EX+/" + BuildConfig.VERSION_NAME;

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request.Builder builder = chain.request().newBuilder();
        builder.header(HttpHeaders.USER_AGENT, USER_AGENT);
        return chain.proceed(builder.build());
    }
}
