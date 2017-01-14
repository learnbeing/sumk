/**
 * Copyright (C) 2016 - 2017 youtongluan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yx.http.start;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.yx.bean.Plugin;
import org.yx.common.StartContext;
import org.yx.conf.AppInfo;
import org.yx.http.ServletInfo;
import org.yx.http.filter.HttpLoginWrapper;
import org.yx.http.filter.LoginServlet;
import org.yx.log.Log;
import org.yx.util.StringUtils;

public class HttpServer implements Plugin {

	private final int port;
	private Server server;
	private boolean started = false;

	public HttpServer(int port) {
		super();
		this.port = port;
	}

	public synchronized void start() {
		if (started) {
			return;
		}
		try {
			QueuedThreadPool pool = new QueuedThreadPool(AppInfo.getInt("http.pool.maxThreads", 200),
					AppInfo.getInt("http.pool.minThreads", 8), AppInfo.getInt("http.pool.idleTimeout", 60000),
					new LinkedBlockingDeque<Runnable>(AppInfo.getInt("http.pool.queues", 1000)));
			server = new Server(pool);
			ServerConnector connector = new ServerConnector(server, null, null, null,
					AppInfo.getInt("http.connector.acceptors", 0), AppInfo.getInt("http.connector.selectors", 5),
					new HttpConnectionFactory());
			Log.get("HttpServer").info("listen port：" + port);
			String host = AppInfo.get("http.host");
			if (host != null && host.length() > 0) {
				connector.setHost(host);
			}
			connector.setPort(port);
			connector.setReuseAddress(true);

			server.setConnectors(new Connector[] { connector });
			ServletContextHandler context = new ServletContextHandler();
			context.setContextPath(AppInfo.get("http.web.root", "/intf"));
			@SuppressWarnings("unchecked")
			List<ServletInfo> servlets = (List<ServletInfo>) StartContext.inst.get(ServletInfo.class);
			for (ServletInfo info : servlets) {
				context.addServlet(info.getServletClz(), info.getPath());
			}

			Object path = StartContext.inst.get(LoginServlet.class);
			if (path != null && String.class.isInstance(path)) {
				String loginPath = (String) path;
				if (!loginPath.startsWith("/")) {
					loginPath = "/" + loginPath;
				}
				Log.get("http").info("login path:{}", context.getContextPath() + loginPath);
				context.addServlet(HttpLoginWrapper.class, loginPath);
			}
			String resourcePath = AppInfo.get("http.resource");
			if (StringUtils.isNotEmpty(resourcePath)) {
				ResourceHandler resourceHandler = new ResourceHandler();
				resourceHandler.setResourceBase(resourcePath);
				context.insertHandler(resourceHandler);
			}
			server.setHandler(context);
			server.start();
			started = true;
		} catch (Exception e) {
			Log.printStack(e);
			System.exit(-1);
		}

	}

	@Override
	public void stop() {
		if (server != null) {
			try {
				server.stop();
				this.started = false;
			} catch (Exception e) {
				Log.printStack("http", e);
			}
		}
	}

}
