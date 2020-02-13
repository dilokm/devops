package com.rohitsalecha.springular.devops.interceptor;

import java.io.IOException;
import java.util.Enumeration;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;


//https://stackoverflow.com/a/39207422
public class LoggableDispatcherServlet extends DispatcherServlet {

	private static final long serialVersionUID = -1814726843560189697L;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }
        HandlerExecutionChain handler = getHandler(request);

        try {
            super.doDispatch(request, response);
        } finally {
            log(request, response, handler);
            updateResponse(response);
        }
    }
    
    private void log(HttpServletRequest requestToCache, HttpServletResponse responseToCache, HandlerExecutionChain handler) {
    	JSONObject jsonObject = new JSONObject(); 
        int status = responseToCache.getStatus();
        String httpMethod = requestToCache.getMethod();
        jsonObject.append("httpStatus",status);
        jsonObject.append("path", requestToCache.getRequestURI());
        jsonObject.append("httpMethod",httpMethod);
        jsonObject.append("clientIP", requestToCache.getRemoteAddr());
        jsonObject.append("javaMethod", handler.toString());
        jsonObject.append("queryString", requestToCache.getQueryString());
        
        Enumeration<String> headerNames = requestToCache.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = requestToCache.getHeader(key);
            //Filter any sensitive information from being sent to logs
            if(key.equalsIgnoreCase("JSESSIONID")) {
            	jsonObject.append(key,"");
            }
            else {
                jsonObject.append(key,value);
            }
        }
        
        Enumeration<String> parameterNames = requestToCache.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = (String) parameterNames.nextElement();
            String value = requestToCache.getParameter(key);
            jsonObject.append(key,value);
        }
        if (!httpMethod.equalsIgnoreCase("GET")) {
        	try {
        		String requestBody = requestToCache.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
	        	jsonObject. append("postRequest",requestBody);
			} catch (IOException e) {
				logger.error(e.getStackTrace().toString());
			}
        }
        logger.info(jsonObject.toString());
    }


    private void updateResponse(HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper =
            WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        responseWrapper.copyBodyToResponse();
    }    
    
}
