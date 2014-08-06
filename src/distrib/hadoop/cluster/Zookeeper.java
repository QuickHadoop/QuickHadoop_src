package distrib.hadoop.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import distrib.hadoop.exception.AuthException;
import distrib.hadoop.host.Host;
import distrib.hadoop.util.Path;
import distrib.hadoop.util.Util;

public class Zookeeper {
	/** 版本信息 */
	protected String ver;
	/** 安装文件 */
	protected String installFile;
	/** 主机存放安装文件的临时目录 */
	protected String tmpPath;
	/** HOME目录 */
	protected String zookeeperHome;
	/** 安装文件的本地目录 */
	protected String localPath;
	/** 配置文件目录 */
	protected String cfgPath;
	/** 配置文件 */
	protected String cfgFile;
	/** zkEnv.sh文件 */
	protected String zkEnv;
	/** 配置文件样例 */
	protected String sampCfgFile;
	/** log4j文件 */
	protected String log4j;
	/** 存放配置命令的列表 */
	protected List<String> cmds = new ArrayList<String>();
	
	/** 注释说明 */
	public static final String COMMENT = "### Comment by zookeeper ###";
	public static final String COMMENT_START = "### Add by zookeeper start ###";
	public static final String COMMENT_END = "### Add by zookeeper end ###";
	
	public static Zookeeper instance;
	
	/**
	 * 构造方法
	 */
	private Zookeeper() {
		ver = "zookeeper-3.4.6";
		installFile = ver + ".tar.gz";
		tmpPath = Path.TMP + "/" + installFile;
		zookeeperHome = Path.HADOOP_DISTR + "/" + ver;
		localPath = Util.getFullPath(installFile);
		cfgPath = zookeeperHome + "/conf/";
		cfgFile = cfgPath + "zoo.cfg";
		sampCfgFile = cfgPath + "zoo_sample.cfg";
		zkEnv = zookeeperHome + "/bin/zkEnv.sh";
		log4j = cfgPath + "log4j.properties";
	}
	
	/**
	 * 获取单例对象
	 * 
	 * @return
	 */
	public static Zookeeper getInstance() {
		if(instance == null) {
			instance = new Zookeeper();
		}
		
		return instance;
	}
	
	/**
	 * 修改系统配置文件
	 * 
	 * @param cmd
	 */
	public String cfgUsrProfile(String cmd) {
		return cmd + " " + Path.USR_PROFILE;
	}

	/**
	 * 组装编辑Zookeeper配置文件的命令行
	 * 
	 * @param cmd	命令
	 * @return
	 */
	protected String cfgFile(String cmd) {
		return cmd + " " + cfgFile;
	}
	
	/**
	 * 组装编辑log4j配置文件的命令行
	 * 
	 * @param cmd	命令
	 * @return
	 */
	protected String cfgLog4j(String cmd) {
		return cmd + " " + log4j;
	}
	
	/**
	 * 组装编辑zkEnv配置文件的命令行
	 * 
	 * @param cmd	命令
	 * @return
	 */
	protected String cfgZkEnv(String cmd) {
		return cmd + " " + zkEnv;
	}

	/**
	 * 配置zoo.cfg文件
	 * 
	 * @param dfsIp
	 * @param userHome
	 * @return
	 */
	public void config(String userHome) {
		cmds.add(cfgFile("sed -i '/" + COMMENT_START + "/,/"
				+ COMMENT_END + "/d'"));

		cmds.add(cfgFile("sed -i 's/^dataDir=/#&/g'"));
		cmds.add(cfgFile("sed -i 's/^dataLogDir=/#&/g'"));
		
		cmds.add(cfgFile("echo '" + COMMENT_START + "' >>"));
		cmds.add(cfgFile("echo 'dataDir=" + userHome + "/" 
				+ Path.ZOOKEEPER_DATA_DIR + "' >>"));
		cmds.add(cfgFile("echo 'dataLogDir=" + userHome + "/" 
				+ Path.ZOOKEEPER_DATA_LOG_DIR + "' >>"));

		List<Host> hosts = Cluster.getInstance().getZooKeeperList();
		for(Host h : hosts) {
			cmds.add(cfgFile("echo 'server." + getServerId(h) + "=" 
					+ h.getHostName() + ":2888:3888' >>"));
		}
		cmds.add(cfgFile("echo '" + COMMENT_END + "' >>"));
	}

	/**
	 * 配置系统路径
	 * 
	 * @param userHome
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configUserProfile(String userHome) {
		String pathEnv = "PATH=$PATH:$ZOOKEEPER_HOME/bin";

		cmds.add(cfgUsrProfile("sed -i '/" + COMMENT_START + "/,/"
				+ COMMENT_END + "/d'"));
		cmds.add(cfgUsrProfile("echo '" + COMMENT_START + "' >>"));
		cmds.add(cfgUsrProfile("echo 'ZOOKEEPER_HOME=" + userHome + "/" + zookeeperHome
				+ "' >>"));
		cmds.add(cfgUsrProfile("echo '" + pathEnv + "' >>"));
		cmds.add(cfgUsrProfile("echo 'export PATH' >>"));
		cmds.add(cfgUsrProfile("echo '" + COMMENT_END + "' >>"));

		cmds.add("export ZOOKEEPER_HOME=" + userHome + "/" + zookeeperHome);
		cmds.add("export " + pathEnv);
	}
	
	/**
	 * 配置日志文件
	 */
	public void configLog() {
		cmds.add(cfgZkEnv("sed -i 's/ZOO_LOG_DIR=.*/ZOO_LOG_DIR=\"hadoop_distr\\/dir_for_zookeeper\\/logs\"/g'"));
		cmds.add(cfgLog4j("sed -i 's/zookeeper.log.dir=.*/zookeeper.log.dir=\"hadoop_distr\\/dir_for_zookeeper\\/logs\"/g'"));
		cmds.add(cfgLog4j("sed -i 's/zookeeper.tracelog.dir=.*/zookeeper.tracelog.dir=\"hadoop_distr\\/dir_for_zookeeper\\/logs\"/g'"));
	}
	
	public void prepareConfig() {
		Cluster cluster = Cluster.getInstance();
		Host nn = cluster.getNameNode();
		String userHome = nn.getUserHome();
		
		configUserProfile(userHome);
		config(userHome);
		configLog();
	}
	
	/**
	 * 获取ID号
	 * 
	 * @param h
	 * @return
	 */
	public int getServerId(Host h) {
		Cluster cluster = Cluster.getInstance();
		List<Host> hosts = cluster.getZooKeeperList();
		return hosts.indexOf(h);
	}
	
	/**
	 * 启动Zookeeper
	 * 
	 * @return
	 */
	public String start() {
		return zookeeperHome + "/bin/zkServer.sh start";
	}
	
	/**
	 * 停止Zookeeper
	 * 
	 * @return
	 */
	public String stop() {
		return zookeeperHome + "/bin/zkServer.sh stop";
	}
	
	/**
	 * 根据指定安装文件得到Zookeeper版本信息
	 * 
	 * @param file
	 * @return
	 */
	public void getFromFile(String file) {
		if(file == null) {
			System.out.println("file name is null!");
			return;
		}
		
		File input = new File(file);
		InputStream is = null;
		CompressorInputStream in = null;
		TarArchiveInputStream tin = null;
		try {
			is = new FileInputStream(input);
			in = new GzipCompressorInputStream(is, true);
			tin = new TarArchiveInputStream(in);
			TarArchiveEntry entry = tin.getNextTarEntry();
			if (!entry.isDirectory()) {
				System.out.println("file error!");
				return;
			}
			
			String entryName = entry.getName();
			String dir = entryName.substring(0, entryName.indexOf("/"));
			
			ver = dir;
			localPath = file;
			installFile = file.substring(file.lastIndexOf("/") + 1);
			tmpPath = Path.TMP + "/" + installFile;
			zookeeperHome = Path.HADOOP_DISTR + "/" + ver;
			cfgPath = zookeeperHome + "/conf/";
			cfgFile = cfgPath + "zoo.cfg";
			sampCfgFile = cfgPath + "zoo_sample.cfg";
			zkEnv = zookeeperHome + "/bin/zkEnv.sh";
			log4j = cfgPath + "log4j.properties";
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				in.close();
				tin.close();
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * @return the ver
	 */
	public String getVer() {
		return ver;
	}
	/**
	 * @param ver the ver to set
	 */
	public void setVer(String ver) {
		this.ver = ver;
	}
	/**
	 * @return the installFile
	 */
	public String getInstallFile() {
		return installFile;
	}
	/**
	 * @param installFile the installFile to set
	 */
	public void setInstallFile(String installFile) {
		this.installFile = installFile;
	}
	/**
	 * @return the tmpPath
	 */
	public String getTmpPath() {
		return tmpPath;
	}
	/**
	 * @param tmpPath the tmpPath to set
	 */
	public void setTmpPath(String tmpPath) {
		this.tmpPath = tmpPath;
	}
	/**
	 * @return the home
	 */
	public String getHome() {
		return zookeeperHome;
	}
	/**
	 * @param home the home to set
	 */
	public void setHome(String home) {
		this.zookeeperHome = home;
	}
	/**
	 * @return the localPath
	 */
	public String getLocalPath() {
		return localPath;
	}
	/**
	 * @param localPath the localPath to set
	 */
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
	/**
	 * @return the cfgPath
	 */
	public String getCfgPath() {
		return cfgPath;
	}
	/**
	 * @param cfgPath the cfgPath to set
	 */
	public void setCfgPath(String cfgPath) {
		this.cfgPath = cfgPath;
	}
	
	/**
	 * 设置路径
	 */
	public void setCfgPath() {
		cfgPath = zookeeperHome + "/conf/";
	}
	
	/**
	 * @return the cmds
	 */
	public List<String> getCmds() {
		return cmds;
	}
	/**
	 * @param cmds the cmds to set
	 */
	public void setCmds(List<String> cmds) {
		this.cmds = cmds;
	}
	
	public String getFormatCmd() {
		return zookeeperHome + "/bin/hadoop namenode -format";
	}

	/**
	 * @return the zookeeperHome
	 */
	public String getZookeeperHome() {
		return zookeeperHome;
	}

	/**
	 * @param zookeeperHome the zookeeperHome to set
	 */
	public void setZookeeperHome(String zookeeperHome) {
		this.zookeeperHome = zookeeperHome;
	}

	/**
	 * @return the cfgFile
	 */
	public String getCfgFile() {
		return cfgFile;
	}

	/**
	 * @param cfgFile the cfgFile to set
	 */
	public void setCfgFile(String cfgFile) {
		this.cfgFile = cfgFile;
	}

	/**
	 * @return the sampCfgFile
	 */
	public String getSampCfgFile() {
		return sampCfgFile;
	}

	/**
	 * @param sampCfgFile the sampCfgFile to set
	 */
	public void setSampCfgFile(String sampCfgFile) {
		this.sampCfgFile = sampCfgFile;
	}
}
