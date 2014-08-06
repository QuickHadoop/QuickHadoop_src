package distrib.hadoop.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import distrib.hadoop.host.Host;
import distrib.hadoop.util.Path;
import distrib.hadoop.util.Util;

public class HBase {
	/** 版本信息 */
	protected String ver;
	/** 安装文件 */
	protected String installFile;
	/** 主机存放安装文件的临时目录 */
	protected String tmpPath;
	/** HOME目录 */
	protected String hbaseHome;
	/** 安装文件的本地目录 */
	protected String localPath;
	/** 配置文件目录 */
	protected String cfgPath;
	/** 存放配置命令的列表 */
	protected List<String> cmds = new ArrayList<String>();
	
	/** 注释说明 */
	public static final String COMMENT = "### Comment by HBase ###";
	public static final String COMMENT_START = "### Add by HBase start ###";
	public static final String COMMENT_END = "### Add by HBase end ###";
	
	public static HBase instance;
	
	/**
	 * 构造方法
	 */
	private HBase() {
		ver = "hbase-0.98.3-hadoop2";
		installFile = ver + "-bin.tar.gz";
		tmpPath = Path.TMP + "/" + installFile;
		hbaseHome = Path.HADOOP_DISTR + "/" + ver;
		localPath = Util.getFullPath(installFile);
		cfgPath = hbaseHome + "/conf/";
	}
	
	/**
	 * 获取单例对象
	 * 
	 * @return
	 */
	public static HBase getInstance() {
		if(instance == null) {
			instance = new HBase();
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
	 * 组装编辑HBase配置文件的命令行
	 * 
	 * @param cmd	命令
	 * @param file	文件
	 * @return
	 */
	protected String cfgFile(String cmd, String file) {
		return cmd + " " + cfgPath + file;
	}

	/**
	 * 配置hbase-site.xml
	 * 
	 * @param cmd
	 */
	public String cfgSite(String cmd) {
		return cfgFile(cmd, Path.HBASE_SITE);
	}

	/**
	 * 配置hbase-env.sh
	 * 
	 * @param cmd
	 */
	public String cfgEnv(String cmd) {
		return cfgFile(cmd, Path.HBASE_ENV);
	}

	/**
	 * 配置regionservers
	 * 
	 * @param cmd
	 */
	public String cfgReg(String cmd) {
		return cfgFile(cmd, Path.HBASE_REG);
	}
	
	/**
	 * 配置hadoop-env.sh命令
	 * 
	 * @param userHome
	 */
	public void configEnv(String userHome) {
		Cluster cluster = Cluster.getInstance();
		Hadoop hadoop = cluster.getHadoop();
		if(hadoop == null) {
			return;
		}
		
		cmds.add(cfgEnv("sed -i '/" + COMMENT_START + "/,/"
				+ COMMENT_END + "/d'"));

		cmds.add(cfgEnv("sed -i '/export JAVA_HOME/d'"));
		cmds.add(cfgEnv("sed -i '/export HBASE_HOME/d'"));
//		cmds.add(cfgEnv("sed -i '/export PATH/d'"));
		cmds.add(cfgEnv("sed -i '/export HBASE_HEAPSIZE/d'"));
		cmds.add(cfgEnv("sed -i '/export HBASE_MANAGES_ZK/d'"));

		cmds.add(cfgEnv("echo '" + COMMENT_START + "' >>"));

		String java = Jre.getInstance().getHome();
		cmds.add(cfgEnv("echo 'export JAVA_HOME=" + userHome + "/" + java + "' >>"));
		cmds.add(cfgEnv("echo 'export HBASE_HOME=" + userHome + "/" + hbaseHome + "' >>"));
		cmds.add(cfgEnv("echo 'export PATH=$PATH:" + userHome + "/" + hbaseHome + "/bin' >>"));
		cmds.add(cfgEnv("echo 'export HBASE_HEAPSIZE=2048' >>"));
		cmds.add(cfgEnv("echo 'export HBASE_MANAGES_ZK=false' >>"));

		cmds.add(cfgEnv("echo '" + COMMENT_END + "' >>"));
	}

	/**
	 * 配置hbase-site.xml命令
	 * 
	 * @param JobTrackerIp
	 */
	public void configSite() {
		Cluster cluster = Cluster.getInstance();
		List<Host> hosts = cluster.getZooKeeperList();
		StringBuffer hostStr = new StringBuffer();
		for(Host h : hosts) {
			if(hostStr.length() > 0) {
				hostStr.append(",");
			}
			hostStr.append(h.getIp());
		}
		
		cmds.add(cfgSite("sed -i '/<configuration>/,/<\\/configuration>/d'"));
		cmds.add(cfgSite("echo '<configuration>' >>"));

		cmds.add(cfgSite("echo '  <property>' >>"));
		cmds.add(cfgSite("echo '    <name>hbase.rootdir</name>' >>"));
		cmds.add(cfgSite("echo '    <value>hdfs://" + Hadoop.CLUST_NAME + "/hbase</value>' >>"));
		cmds.add(cfgSite("echo '  </property>' >>"));

		cmds.add(cfgSite("echo '  <property>' >>"));
		cmds.add(cfgSite("echo '    <name>hbase.cluster.distributed</name>' >>"));
		cmds.add(cfgSite("echo '    <value>true</value>' >>"));
		cmds.add(cfgSite("echo '  </property>' >>"));
		
		cmds.add(cfgSite("echo '  <property>' >>"));
		cmds.add(cfgSite("echo '    <name>hbase.zookeeper.quorum</name>' >>"));
		cmds.add(cfgSite("echo '    <value>" + hostStr.toString() + "</value>' >>"));
		cmds.add(cfgSite("echo '  </property>' >>"));
		
		cmds.add(cfgSite("echo '  <property>' >>"));
		cmds.add(cfgSite("echo '    <name>zookeeper.session.timeout</name>' >>"));
		cmds.add(cfgSite("echo '    <value>60000</value>' >>"));
		cmds.add(cfgSite("echo '  </property>' >>"));
		
		cmds.add(cfgSite("echo '  <property>' >>"));
		cmds.add(cfgSite("echo '    <name>hbase.zookeeper.property.clientPort</name>' >>"));
		cmds.add(cfgSite("echo '    <value>2181</value>' >>"));
		cmds.add(cfgSite("echo '  </property>' >>"));

		cmds.add(cfgSite("echo '</configuration>' >>"));
	}

	/**
	 * 配置regionservers
	 */
	public void configReg(List<Host> dataNodes) {
		cmds.add(cfgReg("cat /dev/null >"));
		for(Host s : dataNodes) {
			cmds.add(cfgReg("echo " + s.getIp() + " >>"));		
		}
	}
	
	/**
	 * 拷贝Hadoop的配置文件至HBase配置目录下
	 * 
	 * @param hadoop
	 */
	public void cpHadoopCfg(Hadoop hadoop) {
		if(hadoop == null) {
			return;
		}
		
		cmds.add("\\cp " + hadoop.cfgPath + Path.HADOOP_CORE_SITE + " "  + cfgPath);
		cmds.add("\\cp " + hadoop.cfgPath + Path.HADOOP_HDFS_SITE + " "  + cfgPath);
	}
	
	/**
	 * 组装配置命令
	 */
	public void prepareConfig() {
		Cluster cluster = Cluster.getInstance();
		Host nn = cluster.getNameNode();
		List<Host> dataNodes = cluster.getDataNodeList();
		String userHome = nn.getUserHome();
		Hadoop hadoop = cluster.getHadoop();
		
		configEnv(userHome);
		configSite();
		configReg(dataNodes);
		cpHadoopCfg(hadoop);
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
		return hbaseHome + "/bin/start-hbase.sh";
	}
	
	/**
	 * 停止Zookeeper
	 * 
	 * @return
	 */
	public String stop() {
		return hbaseHome + "/bin/stop-hbase.sh";
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
			hbaseHome = Path.HADOOP_DISTR + "/" + ver;
			cfgPath = hbaseHome + "/conf/";
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
	
	public static void main(String[] args) {
		HBase.getInstance().getFromFile("./config/zookeeper-3.4.6.tar.gz");
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
		return hbaseHome;
	}
	/**
	 * @param home the home to set
	 */
	public void setHome(String home) {
		this.hbaseHome = home;
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
		cfgPath = hbaseHome + "/conf/";
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
}
