package com.sun.jersey.client.apache4;

import java.net.ProxySelector;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.protocol.HttpContext;

public class CustomProxySelectorRoutePlanner extends ProxySelectorRoutePlanner {

	public CustomProxySelectorRoutePlanner(SchemeRegistry schreg, ProxySelector prosel) {
		super(schreg, prosel);
	}

	@Override
	protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
		if (!target.getHostName().contains(".")) {
			return null;
		}
		return super.determineProxy(target, request, context);
	}
}
