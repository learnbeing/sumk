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
package org.yx.conf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import org.yx.db.annotation.ColumnType;
import org.yx.log.Log;
import org.yx.rpc.LocalhostUtil;

public class AppInfo {
	public static final String CLASSPATH_ALL_URL_PREFIX = "classpath*:";
	public static final String CLASSPATH_URL_PREFIX = "classpath:";
	private static String appId = "sumk";

	public static int httpSessionTimeout = 3600;

	/**
	 * 默认情况下，DB操作是根据数据库中的主键，还是redis中的主键。
	 */
	public static ColumnType modifyByColumnType = ColumnType.ID_DB;

	private static List<Observer> observers = new ArrayList<>();

	public static synchronized void addObserver(Observer ob) {
		if (observers.contains(ob)) {
			return;
		}
		observers.add(ob);
	}

	public static final PropertiesInfo info = new PropertiesInfo("app.properties") {

		private Integer intValue(String key) {
			String temp = get(key);
			if (temp != null) {
				try {
					return Integer.valueOf(temp);
				} catch (Exception e) {
					Log.get(AppInfo.class).error(key + "=" + temp + ", is not valid Integer,ignore it");
				}
			}
			return null;
		}

		@Override
		public void deal(InputStream in) throws Exception {
			super.deal(in);
			String id = get("sumk.appId");
			if (id != null) {
				AppInfo.appId = id;
			}

			Integer temp = intValue("http.session.timeout");
			if (temp != null) {
				AppInfo.httpSessionTimeout = temp;
			}

			observers.forEach(ob -> {
				ob.update(null, null);
			});
		}

	};

	public static String getZKUrl() {
		return info.get("sumk.zkurl");
	}

	public static String getIp() {
		String ip = info.get("sumk.ip");
		if (ip != null) {
			return ip;
		}
		try {
			return LocalhostUtil.getLocalIP();
		} catch (Exception e) {
			Log.printStack(e);
		}
		return "0.0.0.0";
	}

	public static String getAppId() {
		return appId;
	}

	public static String get(String name) {
		return info.get(name);
	}

	public static String get(String name, String defaultValue) {
		return info.get(name, defaultValue);
	}

	public static int getInt(String name, int defaultValue) {
		String value = info.get(name);
		if (value == null || value.length() == 0) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(name);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static String systemCharset() {
		return System.getProperty("sumk.app.charset", "UTF-8");
	}

	public static boolean getBoolean(String name, boolean defaultValue) {
		String value = info.get(name);
		if (value == null || value.length() == 0) {
			return defaultValue;
		}
		value = value.toLowerCase();
		return "1".equals(value) || "true".equals(value);
	}

}
