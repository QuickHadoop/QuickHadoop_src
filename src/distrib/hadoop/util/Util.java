package distrib.hadoop.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class Util {
	
	/** DNS�ո񳤶� */
	public static final int DNS_SPACE_LEN = 25;

	public static final String TEST_IP = "42.121.117.104";
	
	/* Ping服务器端口号 */
	public static final int PING_PORT = 22;
	
	/** 本地ip地址 */
	private static String localIp;
	
	/**
	 * DNS���ո�
	 * 
	 * @param ip
	 * @return
	 */
	public static String dnsSpacer(String ip) {
		StringBuffer buf = new StringBuffer(" ");
		int left = DNS_SPACE_LEN - ip.length();
		
		for(int i = 0; i < left; i++) {
			buf.append(" ");
		}
		
		return buf.toString();
	}
	
	/**
	 * 根据IP地址判断是否该主机可作为集群节点
	 * 
	 * @param ip
	 * @return
	 */
	public static boolean findHost(String ip) {
		try {
			Runtime cmd = Runtime.getRuntime();
			Process p = cmd.exec("ping -n 1 -w 2000 " + ip);
			InputStream is = p.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while((line = br.readLine()) != null) {
				if(line.contains("TTL=64")) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 试探该IP地址是否能连通22号端口
	 * 
	 * @param ip	IP地址
	 * @param msecond	超时时间
	 * @return
	 */
	public static boolean ping(String ip, int msecond) {
		if(!ipIsValid(ip)){
			return false;
		}
		
		try {
			Socket server = new Socket();
			InetSocketAddress address = 
				new InetSocketAddress(ip, PING_PORT);
			server.connect(address, msecond);
			server.close();
		} catch (Exception e) {
			return false;
		} 
		
		return true;
	}
	
	/**
	 * 获取本机IP地址
	 * 
	 * @return
	 */
	public static String getLoaclIp(){
		if(localIp != null) {
			return localIp;
		}

		try {
			Socket server = new Socket();
			InetSocketAddress address = 
				new InetSocketAddress(TEST_IP, 22);
			server.connect(address, 5000);
			localIp = server.getLocalAddress().getHostAddress();
			server.close();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				localIp = InetAddress.getLocalHost().getHostAddress();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		} 
		
		return localIp;
	}
	
	/**
	 * 获取IP地址的主机地址
	 * 
	 * @return
	 */
	public static int getIpHost(String ip) {
		if(!ipIsValid(ip)){
			System.out.println("invalid ip :" + ip);
			return -1;
		}
		
		return Integer.parseInt(ip.substring(ip.lastIndexOf(".") + 1));
	}
	
	/**
	 * 获取IP网络地址
	 * 
	 * @return
	 */
	public static String getIpNet(String ip) {
		if(!ipIsValid(ip)){
			return null;
		}
		
		return ip.substring(0, ip.lastIndexOf(".") + 1);
	}
	
	/**
	 * 检查IP地址字符串的合法性
	 * 
	 * @param ip
	 * @return
	 */
	public static boolean ipIsValid(String ip) {
		if (ip == null) {
			return false;
		}

		ip = ip.trim();
		final String REGX_IP = "((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)";
		if (!ip.matches(REGX_IP)) {
			return false;
		}

		return true;
	}
	
    /**
     * 获取文件全路径
     * 
     * @param path
     * @return
     */
    public static String getFullPath(String path) {
    	ClassLoader cl = Util.class.getClassLoader();
    	URL url = cl.getResource(path);
    	if(url == null) {
    		return null;
    	}
    	return url.getPath();
    }
}
