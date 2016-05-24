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
import distrib.hadoop.util.RetNo;
import distrib.hadoop.util.Util;

public class Spark {
	/** 版本信息 */
	protected String ver;
	/** 安装文件 */
	protected String installFile;
	/** 主机存放安装文件的临时目录 */
	protected String tmpPath;
	/** HOME目录 */
	protected String sparkHome;
	/** 安装文件的本地目录 */
	protected String localPath;
	/** 配置文件目录 */
	protected String cfgPath;
	/** 存放配置命令的列表 */
	protected List<String> cmds = new ArrayList<String>();
	
	/** 注释说明 */
	public static final String COMMENT = "### Comment by Spark ###";
	public static final String COMMENT_START = "### Add by Spark start ###";
	public static final String COMMENT_END = "### Add by Spark end ###";
	
	public static Spark instance;
	
	/**
	 * 构造方法
	 */
	private Spark() {
		ver = "spark-1.5.2-bin-hadoop2.6";
		installFile = ver + "tgz";
		tmpPath = Path.TMP + "/" + installFile;
		sparkHome = Path.HADOOP_DISTR + "/" + ver;
		localPath = Util.getFullPath(installFile);
		cfgPath = sparkHome + "/conf/";
	}
	
	/**
	 * 获取单例对象
	 * 
	 * @return
	 */
	public static Spark getInstance() {
		if(instance == null) {
			instance = new Spark();
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
	 * 组装编辑Spark配置文件的命令行
	 * 
	 * @param cmd	命令
	 * @param file	文件
	 * @return
	 */
	protected String cfgFile(String cmd, String file) {
		return cmd + " " + cfgPath + file;
	}

	/**
	 * 配置spark-env.sh
	 * 
	 * @param cmd
	 */
	public String cfgEnv(String cmd) {
		return cfgFile(cmd, Path.SPARK_ENV);
	}

	/**
	 * 配置slaves
	 * 
	 * @param cmd
	 */
	public String cfgSlaves(String cmd) {
		return cfgFile(cmd, Path.SPARK_SLAVE);
	}
	
	/**
	 * 配置用户的profile文件，添加环境变量
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configUserProfile(String userHome) {
		cmds.add(cfgEnv("sed -i '/" + COMMENT_START + "/,/"
				+ COMMENT_END + "/d'"));

		cmds.add(cfgEnv("sed -i '/export SCALA_HOME/d'"));
		cmds.add(cfgEnv("sed -i '/export SPARK_HOME/d'"));

		cmds.add(cfgEnv("echo '" + COMMENT_START + "' >>"));

		String scalaHome = Scala.getInstance().getHome();
		cmds.add(cfgEnv("echo 'export SCALA_HOME=" + userHome + "/" + scalaHome + "' >>"));
		cmds.add(cfgEnv("echo 'export SPARK_HOME=" + userHome + "/" + sparkHome + "' >>"));
		cmds.add(cfgEnv("echo 'export PATH=$PATH:" + userHome + "/" + sparkHome + "/bin' >>"));

		cmds.add(cfgEnv("echo '" + COMMENT_END + "' >>"));		
	}
	
	/**
	 * 配置hadoop-env.sh命令
	 * 
	 * @param userHome
	 */
	public void configEnv(Cluster cluster) {
		cmds.add(cfgEnv("cat /dev/null >"));

		cmds.add(cfgEnv("echo '" + COMMENT_START + "' >>"));

		String userHome = cluster.getNameNode().getUserHome();
		String scalaHome = Scala.getInstance().getHome();
		String jreHome = Jre.getInstance().getHome();
		String hadoopCfg = cluster.getHadoop().getCfgPath();
		String nnIp = cluster.getNameNode().getIp();
		
		cmds.add(cfgEnv("echo 'export JAVA_HOME=" + userHome + "/" + jreHome + "' >>"));
		cmds.add(cfgEnv("echo 'export SCALA_HOME=" + userHome + "/" + scalaHome + "' >>"));
		cmds.add(cfgEnv("echo 'export SPARK_MASTER_IP=" + nnIp + "' >>"));
		cmds.add(cfgEnv("echo 'export SPARK_WORKER_MEMORY=512m' >>"));
		cmds.add(cfgEnv("echo 'export HADOOP_CONF_DIR=" + hadoopCfg + "' >>"));
		cmds.add(cfgEnv("echo 'export SPARK_DAEMON_JAVA_OPTS=-Dspark.deploy.recoveryMode=ZOOKEEPER -Dspark.deploy.zookeeper.url=" + cluster.getNameNode().getHostName() + ":2181," + cluster.getSecNameNode().getHostName() + ":2181 -Dspark.deploy.zookeeper.dir=spark' >>"));

		cmds.add(cfgEnv("echo '" + COMMENT_END + "' >>"));
	}

	/**
	 * 配置slaves
	 */
	public void cfgSlaves(List<Host> hosts) {
		cmds.add(cfgSlaves("cat /dev/null >"));
		cmds.add(cfgEnv("echo '" + COMMENT_START + "' >>"));
		for(Host s : hosts) {
			cmds.add(cfgSlaves("echo " + s.getHostName() + " >>"));		
		}
		cmds.add(cfgEnv("echo '" + COMMENT_END + "' >>"));
	}
	
	/**
	 * 组装配置命令
	 */
	public void prepareConfig() {
		Cluster cluster = Cluster.getInstance();
		Host nn = cluster.getNameNode();
		String userHome = nn.getUserHome();
		
		configEnv(cluster);
		cfgSlaves(cluster.getDataNodeList());
		configUserProfile(userHome);
	}
	
	/**
	 * 启动Zookeeper
	 * 
	 * @return
	 */
	public String start() {
		return sparkHome + "/sbin/start-all.sh";
	}
	
	/**
	 * 停止Zookeeper
	 * 
	 * @return
	 */
	public String stop() {
		return sparkHome + "/sbin/stop-all.sh";
	}
	
	/**
	 * 根据指定安装文件得到Spark版本信息
	 * 
	 * @param file
	 * @return
	 */
	public int getFromFile(String file) {
		File input = new File(file);
		if(!input.exists()) {
			System.out.println("The Spark install file is not exist!");
			return RetNo.FILE_NOT_EXIST;
		}
		
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
				return RetNo.FILE_IO_ERROR;
			}
			
			String entryName = entry.getName();
			String dir = entryName.substring(0, entryName.indexOf("/"));
			
			ver = dir;
			localPath = file;
			installFile = input.getName();
			tmpPath = Path.TMP + "/" + installFile;
			sparkHome = Path.HADOOP_DISTR + "/" + ver;
			cfgPath = sparkHome + "/conf/";
		} catch (Exception e) {
			e.printStackTrace();
			return RetNo.FILE_IO_ERROR;
		} finally {
			try {
				is.close();
				in.close();
				tin.close();
			} catch (Exception e) {
			}
		}
		
		return RetNo.OK;
	}
	
	public static void main(String[] args) {
		Spark.getInstance().getFromFile("./config/spark-1.5.2-bin-hadoop2.6.tgz");
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
		return sparkHome;
	}
	/**
	 * @param home the home to set
	 */
	public void setHome(String home) {
		this.sparkHome = home;
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
		cfgPath = sparkHome + "/conf/";
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
