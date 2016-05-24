package distrib.hadoop.host;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import ch.ethz.ssh2.Connection;
import distrib.hadoop.cluster.Cluster;
import distrib.hadoop.cluster.HBase;
import distrib.hadoop.cluster.Hadoop;
import distrib.hadoop.cluster.Jre;
import distrib.hadoop.cluster.Scala;
import distrib.hadoop.cluster.Spark;
import distrib.hadoop.cluster.Zookeeper;
import distrib.hadoop.exception.AuthException;
import distrib.hadoop.shell.RemoteShell;
import distrib.hadoop.util.Path;
import distrib.hadoop.util.Util;

/**
 * 主机类
 * 
 * @author guolin
 */
public class Host {

	/** 主机基本信息 */
	protected String hostName;
	protected String ip;
	protected String userName;
	protected String passwd;
	
	/** 用户目录 */
	protected String userHome;
	
	/** Shell */
	protected RemoteShell shell;
	
	/** 连接会话 */
	protected Connection conn;

	/** 主机在集群中的属性 */
	protected BooleanProperty isNameNode;
	protected BooleanProperty isDataNode;
	protected BooleanProperty isJournalNode;
	protected BooleanProperty isZookeeper;
	protected BooleanProperty isHBase;
	
	/**
	 * 构造主机
	 * 
	 * @param hostName
	 * @param ip
	 * @param userName
	 * @param passwd
	 */
	public Host(String hostName, String ip, String userName, String passwd) {
		this.hostName = hostName;
		this.ip = ip;
		this.userName = userName;
		this.passwd = passwd;
		this.shell = new RemoteShell(this);
		
		this.isNameNode = new SimpleBooleanProperty(false);
		this.isDataNode = new SimpleBooleanProperty(false);
		this.isJournalNode = new SimpleBooleanProperty(false);
		this.isZookeeper = new SimpleBooleanProperty(false);
		this.isHBase = new SimpleBooleanProperty(false);
	}

	/**
	 * 登录设备
	 * 
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public boolean login() throws AuthException, IOException {
		conn = new Connection(ip);
		conn.connect();

		boolean authed = conn.authenticateWithPassword(userName, passwd);

		if(!authed) {
			System.err.println("### " + ip + " login failed.");
		} else {
			System.out.println("### " + ip + " login.");
		}
		
		return authed;
	}
	
	/**
	 * 断开设备连接
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void logout() throws AuthException, IOException {
		if(conn != null) {
			conn.close();
			System.out.println("### " + ip + " logout.");
		}
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
	public void putFile(String localFile, 
			String remoteFileName,String remotePath) throws AuthException, IOException {
		shell.putFile( localFile, remoteFileName, remotePath);
	}
	
	/**
	 * 安装java
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void installJava() throws AuthException, IOException {
		Jre jre = Jre.getInstance();
		shell.excute("rm -fr " + jre.getHome());
		shell.putFile(jre.getLocalPath(), jre.getInstallFile(), Path.TMP);
		shell.excute("tar xfz " + jre.getTmpPath() + " -C " + Path.HADOOP_DISTR);
	}
	
	/**
	 * 安装Scala
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void installScala() throws AuthException, IOException {
		Scala scala = Scala.getInstance();
		shell.excute("rm -fr " + scala.getHome());
		shell.putFile(scala.getLocalPath(), scala.getInstallFile(), Path.TMP);
		shell.excute("tar xfz " + scala.getTmpPath() + " -C " + Path.HADOOP_DISTR);
	}
	
	/**
	 * 安装Hadoop
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void installHadoop() throws AuthException, IOException {
		Hadoop hadoop = Cluster.getInstance().getHadoop();
		if(hadoop == null) {
			System.out.println("Hadoop is null while install!");
			return;
		}

		shell.putFile(hadoop.getLocalPath(), hadoop.getInstallFile(), Path.TMP);
		shell.excute("tar xfz " + hadoop.getTmpPath() + " -C " + Path.HADOOP_DISTR);

		shell.excute("mkdir " + Path.HDFS_DIR);
		shell.excute("mkdir " + Path.HDFS_DIR_TMP);
	}
	
	/**
	 * 安装Spark
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void installSpark() throws AuthException, IOException {
		Spark spark = Spark.getInstance();
		if(spark == null) {
			System.out.println("Spark is null while install!");
			return;
		}
		
		shell.putFile(spark.getLocalPath(), spark.getInstallFile(), Path.TMP);
		shell.excute("tar xfz " + spark.getTmpPath() + " -C " + Path.HADOOP_DISTR);
	}

	/**
	 * 安装Zookeeper
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void installZookeeper() throws AuthException, IOException {
		Zookeeper zoo = Cluster.getInstance().getZookeeper();
		if(zoo == null) {
			System.out.println("Zookeeper is null while install!");
			return;
		}

		shell.putFile(zoo.getLocalPath(), zoo.getInstallFile(), Path.TMP);
		shell.excute("tar xfz " + zoo.getTmpPath() + " -C " + Path.HADOOP_DISTR);

		shell.excute("mkdir " + Path.ZOOKEEPER_DIR);
		shell.excute("mkdir " + Path.ZOOKEEPER_DATA_DIR);
		shell.excute("mkdir " + Path.ZOOKEEPER_DATA_LOG_DIR);
		shell.excute("mkdir " + Path.ZOOKEEPER_LOG_DIR);
		
		shell.excute("echo '" + zoo.getServerId(this) + "' > " + Path.ZOOKEEPER_ID);
		shell.excute("\\cp " + zoo.getSampCfgFile() + " "  + zoo.getCfgFile());
	}
	
	/**
	 * 安装HBase
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void installHBase() throws AuthException, IOException {
		HBase hBase = Cluster.getInstance().gethBase();
		if(hBase == null) {
			System.out.println("HBase is null while install!");
			return;
		}
		
		shell.putFile(hBase.getLocalPath(), hBase.getInstallFile(), Path.TMP);
		shell.excute("tar xfz " + hBase.getTmpPath() + " -C " + Path.HADOOP_DISTR);
	}
	
	/**
	 * 创建Hadoop工作目录
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void createHadoopDir() throws AuthException, IOException {
		shell.excute("rm -fr " + Path.HADOOP_DISTR);
		shell.excute("mkdir " + Path.HADOOP_DISTR);
	}
	
	/**
	 * 配置Hadoop
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configHadoop() throws AuthException, IOException {
		Hadoop hadoop = Cluster.getInstance().getHadoop();
		if(hadoop != null) {
			for(String c : hadoop.getCmds()) {
				shell.excute(c);
			}
		}
	}
	
	/**
	 * 配置Spark
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configSpark() throws AuthException, IOException {
		Spark spark = Spark.getInstance();
		if(spark != null) {
			for(String c : spark.getCmds()) {
				shell.excute(c);
			}
		}
	}
	
	/**
	 * 配置Zookeeper
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configZookeeper() throws AuthException, IOException {
		Zookeeper zoo = Cluster.getInstance().getZookeeper();
		if(zoo != null) {
			for(String c : zoo.getCmds()) {
				shell.excute(c);
			}
		}
	}
	
	/**
	 * 配置HBase
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configHBase() throws AuthException, IOException {
		HBase hBase = Cluster.getInstance().gethBase();
		if(hBase != null) {
			for(String c : hBase.getCmds()) {
				shell.excute(c);
			}
		}
	}
	
	/**
	 * 获取绝对路径
	 * 
	 * @param path
	 * @return
	 */
	public String fullPath(String path) {
		return userHome + "/" +  path;
	}
	
	/**
	 * �����ʱĿ¼
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void cleanTmpDir() throws AuthException, IOException {
		shell.excute("rm -fr " + Path.TMP);
	}
	
	/**
	 * ��ȡ�豸�Ļ�����
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void getHostEnv() throws AuthException, IOException {
		setUserHome(shell.getEnv("PWD"));
	}
	
	/**
	 * 获取Hadoop版本
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public String getHadoopVer() throws AuthException, IOException {
		return shell.getCmdOutPut("hadoop version");
	}
	
	/**
	 * ���RSA KEY
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void genRsaKey() throws AuthException, IOException {
		shell.excute("rm -fr " + Path.TMP);
		shell.excute("mkdir " + Path.TMP);
		
		/* �ȳ��Ե�¼�Լ�����ֹ�е������û��~/.sshĿ¼��
		 * ��Ŀ¼�����ֶ��������ᵼ�¹���rsaʧ�ܣ�����¼�Լ�֮����Զ���ɡ� */
		shell.excute("ssh -o StrictHostKeyChecking=no 127.0.0.1");
		
		shell.excute("ssh-keygen -t rsa", Path.RSA + "\n", "\n", "\n");
		shell.excute("cat " + Path.RSA_PUB + ">> " + Path.SSH_AUTH);
		shell.excute("\\cp " + Path.RSA + " " + Path.SSH_DIR);
		shell.excute("\\cp " + Path.RSA_PUB + " " + Path.SSH_DIR);
		
		shell.excute("chmod 700 " + Path.SSH_DIR);
		shell.excute("chmod 600 " + Path.SSH_AUTH);	
	}
	
	/**
	 * 关闭防火墙
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void closeFireWall() throws AuthException,
	IOException {
		shell.excute("service iptables stop");		
	}
	
	/**
	 * 删除集群，恢复到初始状态
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void restore() throws AuthException, IOException {
		shell.excute("rm -fr " + Path.HADOOP_DISTR);
		shell.excute("rm -fr " + Path.TMP);
		shell.excute("sed -i '/" + Hadoop.COMMENT_START + "/,/" + Hadoop.COMMENT_END + "/d' " + Path.USR_PROFILE);
		shell.excute("sed -i '/" + Zookeeper.COMMENT_START + "/,/" + Zookeeper.COMMENT_END + "/d' " + Path.USR_PROFILE);
				
		restoreHostName();
		
		shell.cfgDNS("sed -i '/" + Hadoop.COMMENT_START + "/,/" + Hadoop.COMMENT_END + "/d'");
	}

	/**
	 * 恢复主机名称
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	private void restoreHostName() throws AuthException, IOException {
		String ver = shell.getCmdOutPut("cat /etc/issue");
		String cfg = "/etc/sysconfig/network";
		if(ver.contains("CentOS") || ver.contains("Fedora")) {
			cfg = "/etc/sysconfig/network";
			restoreHostName(cfg);
		} else if(ver.contains("Ubuntu") || ver.contains("Debian")) {
			cfg = "/etc/hostname";
			restoreHostName(cfg);
		} else if(ver.contains("Slackware")) {
			cfg = "/etc/conf.d/hostname";
			restoreHostName( cfg);
			shell.excute("/etc/init.d/hostname restart");
		} else if(ver.contains("Arch")) {
			cfg = "/etc/rc.conf";
			restoreHostName(cfg);
		} else {
			cfg = "/etc/sysconfig/network";
			restoreHostName(cfg);
		}
	}

	/**
	 * 恢复主机名称
	 * 
	 * @param cfg
	 * @throws AuthException
	 * @throws IOException
	 */
	private void restoreHostName(String cfg) throws AuthException, IOException {
		shell.excuteSudo("sed -i '/" + Hadoop.COMMENT_START + "/,/" + Hadoop.COMMENT_END + "/d' " + cfg);
		shell.excuteSudo("sed -i 's/" + Hadoop.COMMENT + "//g' " + cfg);
	}
	
	/**
	 * 配置主机名称
	 * 
	 * @param host
	 * @param name
	 * @throws AuthException
	 * @throws IOException
	 */
	public void cfgHostName() throws AuthException, IOException {
		String ver = shell.getCmdOutPut("cat /etc/issue");
		if(ver == null) {
			throw new IOException();
		}
		String cfg = "/etc/sysconfig/network";
		if(ver.contains("CentOS") || ver.contains("Fedora")) {
			cfg = "/etc/sysconfig/network";
			configHostName(cfg);
		} else if(ver.contains("Ubuntu") || ver.contains("Debian")) {
			cfg = "/etc/hostname";
			String oldName = shell.getCmdOutPut("hostname");
			shell.excuteSudo("echo '" + Hadoop.COMMENT + oldName + "' > " + cfg);
			shell.excuteSudo("echo '" + Hadoop.COMMENT_START + "' >> " + cfg);
			shell.excuteSudo("echo '" + hostName + "' >> " + cfg);
			shell.excuteSudo("echo '" + Hadoop.COMMENT_END + "' >> " + cfg);
			shell.excutePtySudo("hostname " + hostName);
		} else if(ver.contains("Slackware")) {
			cfg = "/etc/conf.d/hostname";
			configHostName(cfg);
			shell.excuteSudo("/etc/init.d/hostname restart");
		} else if(ver.contains("Arch")) {
			cfg = "/etc/rc.conf";
			configHostName(cfg);
		} else {
			cfg = "/etc/sysconfig/network";
			configHostName(cfg);
		}
	}

	/**
	 * 默认的修改主机名函数
	 * 
	 * @param cfg
	 * @throws AuthException
	 * @throws IOException
	 */
	private void configHostName(String cfg) throws AuthException, IOException {
		shell.excuteSudo("sed -i '/" + Hadoop.COMMENT_START + "/,/" + Hadoop.COMMENT_END + "/d' " + cfg);
		shell.excuteSudo("sed -i 's/^HOSTNAME=/" + Hadoop.COMMENT + "HOSTNAME=/g' " + cfg);
		shell.excuteSudo("echo '" + Hadoop.COMMENT_START + "' >> " + cfg);
		shell.excuteSudo("echo 'HOSTNAME=" + hostName + "' >> " + cfg);
		shell.excuteSudo("echo '" + Hadoop.COMMENT_END + "' >> " + cfg);
		
		shell.excutePtySudo("hostname " + hostName);
	}
	
	/**
	 * 获取masters
	 * 
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public List<String> getMasters(Hadoop hadoop) throws AuthException, IOException {
		List<String> result = new ArrayList<String>();
		List<String> output = shell.excutePty("cat " + hadoop.getCfgPath() + Path.HADOOP_MASTER);
		for(String s : output) {
			if(Util.ipIsValid(s)){
				result.add(s);
			}
		}
		return result;
	}
	
	/**
	 * 获取slaves
	 * 
	 * @return
	 * @throws AuthException
	 * @throws IOException
	 */
	public List<String> getSlaves(Hadoop hadoop) throws AuthException, IOException {
		List<String> result = new ArrayList<String>();
		List<String> output = shell.excutePty("cat " + hadoop.getCfgPath() + Path.HADOOP_SLAVES);
		for(String s : output) {
			if(Util.ipIsValid(s)){
				result.add(s);
			}
		}
		return result;
	}
	
	/**
	 * 配置DNS，需插入文件顶部
	 * 
	 * @param host
	 * @param name
	 * @throws AuthException
	 * @throws IOException
	 */
	public void cfgDNS(List<Host> hosts) throws AuthException, IOException {
		shell.cfgDNS("sed -i '/" + Hadoop.COMMENT_START + "/,/" + Hadoop.COMMENT_END + "/d'");
		shell.cfgDNS("sed -i '1 i" + Hadoop.COMMENT_START + "' ");
		int line = 2;
		for(Host h : hosts) {
			shell.cfgDNS("sed -i '" + line +  " i" + h.getDnsStr() + "' ");
			line++;
		}
		shell.cfgDNS("sed -i '" + line +  " i" + Hadoop.COMMENT_END + "' ");
	}
	
	/**
	 * 获取文件大小
	 * 
	 * @param file
	 * @return
	 */
	public long getFileSize(String file) throws AuthException, IOException {
		long result = 0;
		String d = shell.getCmdOutPut("ls -l " + file + " | awk '{print $5}'");
		try {
			if(d != null) {
				result = Long.parseLong(d);				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public void backUpFile(String fileName) throws AuthException, IOException {
		shell.excute("mv -f " + fileName + " " + fileName + ".bak");
	}
	
	/**
	 * ���һ������ ip     hostname���ַ�
	 * 
	 * @return
	 */
	public String getDnsStr() {
		return ip + Util.dnsSpacer(ip) + hostName;
	}
	
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public String getUserHome() {
		return userHome;
	}

	public void setUserHome(String userHome) {
		this.userHome = userHome;
	}

	public RemoteShell getShell() {
		return shell;
	}

	public void setShell(RemoteShell shell) {
		this.shell = shell;
	}
	

	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public BooleanProperty isNameNodeProperty() {
		return isNameNode;
	}

	public BooleanProperty isDataNodeProperty() {
		return isDataNode;
	}

	public BooleanProperty isJournalNodeProperty() {
		return isJournalNode;
	}

	public BooleanProperty isZookeeperProperty() {
		return isZookeeper;
	}

	public BooleanProperty isHBaseProperty() {
		return isHBase;
	}
}

