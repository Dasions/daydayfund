package com.dasion.daydayfund.tool;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPTool {
	private static final Logger logger = LoggerFactory.getLogger(IPTool.class);
	private static final String URL_KUAI_DAI_LI= "http://www.kuaidaili.com/free/inha/1/";
	private static final String URL_XI_CI = "http://www.xicidaili.com/nt/";
	private static final String CHINA_SECOND = "秒";
	private static final String COLONS = ":";
	private static final String DIV = "div";
	private static final String TD = "td";
	private static final String TR = "tr";
	private static final String LIST = "list";
	private static final String TITLE = "title";
	private static final String IP_LIST = "ip_list";
	private static final String BLANK = " ";
	private static final String EMPTY = "";
	
	public static synchronized List<String> getIps(int ipNums, String markUrl) {
		int defaultPages = 1;
		List<String> ips = new CopyOnWriteArrayList<String>();
		try {
			while (ips.size() < ipNums && defaultPages < 20) {
				//ips.addAll(getIpXICI(defaultPages, defaultPages + 1));
				ips.addAll(getIpTAOBAO(ipNums));
				testIp(ips, markUrl);
				defaultPages++;
			}
		} catch (IOException e) {
			logger.error("获取代理ip失败, e : {}", e);
		}
		return ips;
	}

	public static void testIp(List<String> ips, String markUrl) {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		tool.setUrl(markUrl);
		List<String> ipsUnUsed = new ArrayList<>();
		ips.forEach(ip -> {
			if(!ip.contains(COLONS)){
				ips.remove(ip);
				try {
					TimeUnit.MILLISECONDS.sleep(new Random().nextInt(10));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}else{
				tool.setRequestConfig(tool.getProxyRequestConfig(ip.split(COLONS)[0], Integer.parseInt(ip.split(COLONS)[1])));
			}

			try {
				tool.getContentByGet();
			} catch (Exception e) {
				ipsUnUsed.add(ip);
			}
		});

		ips.removeAll(ipsUnUsed);
	}

	public static HashMap<String, String> getIpKUAI() throws IOException {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		tool.setUrl(URL_KUAI_DAI_LI);
		String ipHtml = tool.getContentByGet();
		HashMap<String, String> ips = new HashMap<String, String>();
		Document docs = Jsoup.parse(ipHtml);
		Element list = docs.getElementById(LIST);
		Elements trs = list.getElementsByTag(TR);
		for (Element tr : trs) {
			Elements tds = tr.getElementsByTag(TD);
			if (tds.size() > 0) {
				ips.put(tds.get(0).html().trim(), tds.get(1).html().trim());
			}
		}
		return ips;
	}

	public static List<String> getIpXICI(int startPage, int endPages) throws IOException {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		List<String> ips = new ArrayList<String>();
		for (int i = startPage; i < endPages; i++) {
			tool.setUrl(URL_XI_CI + i);
			String ipHtml = tool.getContentByGet();
			Document docs = Jsoup.parse(ipHtml);
			Element list = docs.getElementById(IP_LIST);
			Elements trs = list.getElementsByTag(TR);
			for (Element tr : trs) {
				Elements tds = tr.getElementsByTag(TD);
				if (tds.size() > 0) {
					String sudu = tds.get(6).getElementsByTag(DIV).get(0).attr(TITLE);
					if (sudu.contains(CHINA_SECOND)) {
						sudu = sudu.replace(CHINA_SECOND, EMPTY);
					}

					String sudu2 = tds.get(7).getElementsByTag(DIV).get(0).attr(TITLE);
					if (sudu2.contains(CHINA_SECOND)) {
						sudu2 = sudu2.replace(CHINA_SECOND, EMPTY);
					}

					if (Double.parseDouble(sudu) < 0.6 && Double.parseDouble(sudu2) < 0.1) {
						ips.add(tds.get(1).html().trim() + COLONS + tds.get(2).html().trim());
					}

				}
			}
		}
		return ips;
	}

	public static List<String> getIpTAOBAO(int nums) throws IOException {
		HttpclientTool tool = new HttpclientTool();
		tool.createDefaultConfigClientByHttpClients();
		tool.setDefaultCookieStore();
		tool.setDefaultHeader();
		List<String> ips = new Vector<String>();
		try {
			tool.setUrl("http://103964235385623955.dev.checkerproxy.org/?num=" + nums + "&area_type=1&level=3&style=1");
			String ipHtml = tool.getContentByGet();
			String[] ipList = ipHtml.split(BLANK);
			for (String ip : ipList) {
				ips.add(ip.trim());
			}

		} catch (Exception e) {
			logger.error("获取代理ip失败, e : {}", e);
		}

		return ips;
	}

	public static String getLocalIP() {
		String ip = "";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			logger.error("获取机器实例IP发生异常，{}", e);
		}
		return ip;
	}

}
