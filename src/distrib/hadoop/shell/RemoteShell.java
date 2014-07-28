package distrib.hadoop.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import distrib.hadoop.exception.AuthException;
import distrib.hadoop.host.Host;
import distrib.hadoop.util.Path;

/**
 * Զ������
 * 
 * @author guolin
 */
public class RemoteShell {

	/** Զ���豸 */
	private Host host;
	
	/** ִ�������ֵ */
	public static final int OK = 0;
	public static final int FAILED = -10001;

	/**
	 * ����Զ���������
	 */
	public RemoteShell() {
	}

	/**
	 * ����Զ���������
	 * 
	 * @param host
	 *            Զ��Ŀ������
	 */
	public RemoteShell(Host host) {
		this.host = host;
	}
	
	/**
	 * 以root用户执行指定命令
	 * 
	 * @param cmd
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public List<String> excuteSudo(String cmd) throws AuthException,
		IOException {
		if("root".equals(host.getUserName())) {
			return excute(cmd);
		}
		String suCmd = String.format("echo passwd | sudo -S sh -c \"%s\"", cmd);
		return excute(suCmd);
	}
	
	/**
	 * Զ��ִ��һ����������������Ҫ���뽻��ʽ��Ϣ
	 * 
	 * @param cmd
	 *            ����
	 * @param interact
	 *            ����ʽ��Ϣ
	 * @throws AuthException
	 * @throws IOException
	 */
	public List<String> excute(String cmd, String... interact) throws AuthException,
			IOException {
		Session sess = null;
		try {
			if (host == null || cmd == null) {
				throw new RuntimeException("Host or cmd is null when excute cmd.");
			}

			System.out.println("### " + host.getIp() + " Excute cmd: " + cmd);

			Connection conn = host.getConn();
			sess = conn.openSession();
			sess.execCommand(cmd);

			if (interact != null) {
				OutputStream out = sess.getStdin();
				for(String c : interact) {
					out.write(c.getBytes());
					out.write('\n');
				}
				out.close();
			}

			return printStdOut(sess);
		} finally {
			if (sess != null) {
				sess.close();
			}
		}
	}
	
	/**
	 * 以root用户执行指定命令
	 * 
	 * @param cmd
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public List<String> excutePtySudo(String cmd) throws AuthException, IOException {
		if("root".equals(host.getUserName())) {
			return excutePty(cmd);
		}
		String suCmd = String.format("echo passwd | sudo -S sh -c \"%s\"", cmd);
		return excutePty(suCmd);
	}
	
	/**
	 * ʹ�������ն�pty�ķ�ʽԶ��ִ��һ����������������Ҫ���뽻��ʽ��Ϣ
	 * 
	 * @param cmd
	 *            ����
	 * @param interact
	 *            ����ʽ��Ϣ
	 * @throws AuthException
	 * @throws IOException
	 */
	public List<String> excutePty(String... cmd) throws AuthException, IOException {

		Session sess = null;
		try {
			if (host == null || cmd == null) {
				throw new RuntimeException("Host or cmd is null when excute cmd.");
			}

			System.out.println("### " + host.getIp() + " Excute cmd: " + cmd[0]);

			Connection conn = host.getConn();
			sess = conn.openSession();
			sess.requestDumbPTY();
			sess.startShell();

			/* 此处必须睡眠一段时间，否则可能发出的第一条命令不完整 */
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			OutputStream stdin = sess.getStdin();
			for(String c : cmd) {
				stdin.write(c.getBytes());
				stdin.write('\n');
			}
			stdin.write("exit".getBytes());
			stdin.write('\n');
			stdin.close();

			sess.waitForCondition(ChannelCondition.CLOSED
					| ChannelCondition.EOF 
					| ChannelCondition.EXIT_STATUS,
					5000);

			return printStdOut(sess);
		} finally {
			if (sess != null) {
				sess.close();
			}
		}
	}
	
	/**
	 * ��ȡ��������
	 * 
	 * @param key
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public String getEnv(String key) throws AuthException, IOException {
		return getCmdOutPut("echo $" + key);
	}
	
	/**
	 * ��ȡһ��������������һ��
	 * 
	 * @param cmd
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public String getCmdOutPut(String cmd) throws AuthException, IOException {
		List<String> output = excutePty(cmd);
		for(int i = 0; i < output.size() - 1; i++) {
			String s = output.get(i);
			String n = output.get(i + 1);
			if(s != null && s.contains(cmd) 
					&& n != null && !n.isEmpty() && !n.equals("exit")) {
				return n;
			}
		}
		return null;
	}
	
	/**
	 * 获取命令返回结果，该命令只有一行输出。
	 * 
	 * @param cmd
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public String getCmdRet(String cmd) throws AuthException, IOException {
		List<String> output = excutePty(cmd);
		for(int i = 0; i < output.size() - 1; i++) {
			String s = output.get(i);
			String n = output.get(i + 1);
			if(n != null && !n.isEmpty() && n.contains("exit")) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * ��ȡ�ļ�
	 * 
	 * @param remoteFile
	 * @param localPath
	 * @throws AuthException
	 * @throws IOException
	 */
	public void getFile(String remoteFile, String localPath)
			throws AuthException, IOException {
		
		if (host == null) {
			throw new RuntimeException("Host is null when get file.");
		}

		System.out.println("### " + host.getIp() + " get file "
				+ remoteFile);
		
		Connection conn = host.getConn();
		SCPClient client = new SCPClient(conn);
		client.get(remoteFile, localPath);
	}

	/**
	 * �ϴ��ļ�
	 * 
	 * @param localFile
	 * @param remoteFileName
	 * @param remotePath
	 * @throws AuthException
	 * @throws IOException
	 */
	public void putFile(String localFile, String remoteFileName,
			String remotePath) throws AuthException, IOException {

		if (host == null) {
			throw new RuntimeException("Host is null when put file.");
		}

		System.out.println("### " + host.getIp() + " put file " + localFile
				+ " to " + remotePath + "/" + remoteFileName);

		Connection conn = host.getConn();
		SCPClient client = new SCPClient(conn);
		client.put(localFile, remoteFileName, remotePath, "0600");
	}

	/**
	 * ��ӡ�����������Ϣ
	 * 
	 * @param sess
	 * @throws IOException
	 */
	public List<String> printStdOut(Session sess) throws IOException {
		
		List<String> output = new ArrayList<String>();
		InputStream stdout = new StreamGobbler(sess.getStdout());
		InputStream stderr = new StreamGobbler(sess.getStderr());
		BufferedReader out = new BufferedReader(new InputStreamReader(stdout));
		BufferedReader err = new BufferedReader(new InputStreamReader(stderr));
		while (true) {
			String line = out.readLine();
//			System.out.println(line);
			if (line == null){
				break;				
			}
			output.add(line);
		}

		while (true) {
			String line = err.readLine();
//			System.out.println(line);
			if (line == null){
				break;				
			}
			output.add(line);
		}

		out.close();
		err.close();
		return output;
	}
	
	/**
	 * 配置DNS
	 * 
	 * @param cmd
	 * @throws AuthException
	 * @throws IOException
	 */
	public void cfgDNS(String cmd) throws AuthException, IOException {
		excuteSudo(cmd + " " + Path.ETC_HOSTS);
	}
	
	/**
	 * 判断文件是否存在
	 * 
	 * @param fileName
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public boolean fileExists(String fileName) throws AuthException, IOException {
		List<String> output = excutePty("test -e " + fileName + " && echo yes || echo no");
		for(int i = 0; i < output.size() - 1; i++) {
			String s = output.get(i);
			if(s != null && s.startsWith("yes")) {
				return true;
			}
		}
		return false;
	}
	
	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}
}
