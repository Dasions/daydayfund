package com.dasion.daydayfund.tool;

import java.io.File;
import java.io.IOException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;

public class WebDriverTool {
	final private String WEBDRIVER_PATH_KEY = "webdriver.chrome.driver";
	
	final private String USER_DATA_DIR_PRE = "user-data-dir=";
	/**
	 * 用户数据文件夹配置
	 */
	private static String userDataDir;
	
	private static String webDriverPath;
	
	static{
		String sourcePath;
		try {
			sourcePath = new File("..").getCanonicalPath();
			setWebDriverPath(sourcePath + "/chromedriver.exe");
			setUserDataDir(sourcePath + "/cache");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public WebDriverTool(String userDataDir, String webDriverPath){
		if(userDataDir != null){
			setUserDataDir(userDataDir);
		}
		
		if(webDriverPath != null){
			setWebDriverPath(webDriverPath);
		}
	}
	
	/**
	 * 获取driver对象，proxyIpAndPort不为null则返回代理driver
	 * @param proxyIpAndPort
	 * @return
	 */
	public WebDriver getWebDriver(String proxyIpAndPort){
		System.setProperty(WEBDRIVER_PATH_KEY, getWebDriverPath());
		ChromeOptions options = new ChromeOptions();
		options.addArguments(USER_DATA_DIR_PRE + getUserDataDir());
		if(proxyIpAndPort != null){
			setProxy(options, proxyIpAndPort);
		}
		return new ChromeDriver(options);
	}
	
	private void setProxy(ChromeOptions cap, String proxyIpAndPort){
		  Proxy proxy=new Proxy();
		  proxy.setHttpProxy(proxyIpAndPort)
		  .setFtpProxy(proxyIpAndPort)
		  .setSslProxy(proxyIpAndPort);
		  cap.setCapability(CapabilityType.ForSeleniumServer.AVOIDING_PROXY, true);
		  cap.setCapability(CapabilityType.ForSeleniumServer.ONLY_PROXYING_SELENIUM_TRAFFIC, true);
		  System.setProperty("http.nonProxyHosts", "localhost");
		  cap.setCapability(CapabilityType.PROXY, proxy);

	}

	public static String getUserDataDir() {
		return userDataDir;
	}

	public static void setUserDataDir(String userDataDir) {
		WebDriverTool.userDataDir = userDataDir;
	}

	public static String getWebDriverPath() {
		return webDriverPath;
	}

	public static void setWebDriverPath(String webDriverPath) {
		WebDriverTool.webDriverPath = webDriverPath;
	}
}
