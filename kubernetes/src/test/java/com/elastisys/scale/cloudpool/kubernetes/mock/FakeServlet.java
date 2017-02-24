package com.elastisys.scale.cloudpool.kubernetes.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.commons.util.io.IoUtils;
import com.google.common.base.Charsets;

/**
 * A fake {@link Servlet}, intended for testing purposes, which registers all
 * incoming requests and produces the same prepared response to each requests.
 */
public class FakeServlet extends HttpServlet {
    private final static Logger LOG = LoggerFactory.getLogger(FakeServlet.class);

    /** Observed requests. */
    private final List<HttpRequest> requests = new ArrayList<>();

    /** The response that will be produced on every received call. */
    private HttpResponse response;

    /**
     * Create a {@link FakeServlet} that will always respond with the given
     * response.
     * 
     * @param preparedResponse
     */
    public FakeServlet(HttpResponse preparedResponse) {
        this.response = preparedResponse;
    }

    /**
     * Sets the response that will be produced for coming requests.
     *
     * @param nextResponse
     */
    public void setResponse(HttpResponse nextResponse) {
        this.response = nextResponse;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        registerRequest(req);

        resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        resp.setStatus(this.response.getStatusCode());
        ServletOutputStream out = resp.getOutputStream();
        if (this.response.getResponseBody() != null) {
            out.print(this.response.getResponseBody());
        }
        out.flush();
        out.close();
    }

    /**
     * Returns all observed incoming requests.
     *
     * @return
     */
    public List<HttpRequest> getRequests() {
        return this.requests;
    }

    private void registerRequest(HttpServletRequest req) throws IOException {
        String body = IoUtils.toString(req.getInputStream(), Charsets.UTF_8);
        Map<String, String> headers = headerMap(req);
        HttpRequest request = new HttpRequest(req.getRequestURI(), req.getMethod(), body, headers);
        this.requests.add(request);
        LOG.debug("observed request: {}", request);
    }

    private Map<String, String> headerMap(HttpServletRequest req) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            headers.put(header, req.getHeader(header));
        }
        return headers;
    }
}
