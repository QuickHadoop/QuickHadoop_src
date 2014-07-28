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

public abstract class Hadoop {
	/** 版本信息 */
	protected String ver;
	/** 安装文件 */
	protected String installFile;
	/** 主机存放安装文件的临时目录 */
	protected String tmpPath;
	/** HOME目录 */
	protected String hadoopHome;
	/** 安装文件的本地目录 */
	protected String localPath;
	/** 配置文件目录 */
	protected String cfgPath;
	/** 存放配置命令的列表 */
	protected List<String> cmds = new ArrayList<>();
	
	/** 最大复制副本个数 */
	public static final int REP_MAX = 3;
	
	/** Hadoop版本 */
	public static final String V1 = "Hadoop 1";
	public static final String V2 = "Hadoop 2";
	
	/** 集群名称 */
	public static final String CLUST_NAME = "hadoopcluster";
	
	/** 注释说明 */
	public static final String COMMENT = "### Comment by hadoop ###";
	public static final String COMMENT_START = "### Add by hadoop start ###";
	public static final String COMMENT_END = "### Add by hadoop end ###";
	
	/**
	 * 修改系统配置文件
	 * 
	 * @param cmd
	 */
	public String cfgUsrProfile(String cmd) {
		return cmd + " " + Path.USR_PROFILE;
	}

	/**
	 * 组装编辑Hadoop配置文件的命令行
	 * 
	 * @param cmd	命令
	 * @param file	文件
	 * @return
	 */
	protected String cfgFile(String cmd, String file) {
		return cmd + " " + cfgPath + file;
	}
	
	/**
	 * 配置mapred-site.xml
	 * 
	 * @param cmd
	 */
	public String cfgMapredSite(String cmd) {
		return cfgFile(cmd, Path.HADOOP_MAPRED_SITE);
	}

	/**
	 * 配置hadoop-env.sh
	 * 
	 * @param cmd
	 */
	public String cfgHadoopEnv(String cmd) {
		return cfgFile(cmd, Path.HADOOP_ENV);
	}

	/**
	 * 配置core-site.xml
	 * 
	 * @param cmd
	 */
	public String cfgCoreSite(String cmd) {
		return cfgFile(cmd, Path.HADOOP_CORE_SITE);
	}

	/**
	 * 配置hdfs-site.xml
	 * 
	 * @param cmd
	 */
	public String cfgHdfsSite(String cmd) {
		return cfgFile(cmd, Path.HADOOP_HDFS_SITE);
	}

	/**
	 * 配置master
	 * 
	 * @param cmd
	 */
	public String cfgMaster(String cmd) {
		return cfgFile(cmd, Path.HADOOP_MASTER);
	}
	
	/**
	 * 配置slaves
	 * 
	 * @param cmd
	 */
	public String cfgSlaves(String cmd) {
		return cfgFile(cmd, Path.HADOOP_SLAVES);
	}

	/**
	 * 配置master
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configMaster(List<Host> masters) {
		cmds.add(cfgMaster("cat /dev/null >"));
		for(Host m : masters) {
			cmds.add(cfgMaster("echo " + m.getIp() + " >>"));					
		}
	}
	
	/**
	 * 配置slaves
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configSlave(List<Host> slaves) {
		cmds.add(cfgSlaves("cat /dev/null >"));
		for(Host s : slaves) {
			cmds.add(cfgSlaves("echo " + s.getIp() + " >>"));		
		}
	}
	
	/**
	 * 获取复制副本的个数
	 * 
	 * @return
	 */
	public int getRepCount() {
		int count = Cluster.getInstance().getDataNodeList().size();
		return count < REP_MAX ?  count : REP_MAX;
	}
	
	/**
	 * 配置总入口函数
	 */
	public abstract void prepareConfig();
	
	/**
	 * 根据版本号创建对应的Hadoop对象
	 * 
	 * @param version
	 * @return
	 */
	public static Hadoop get(String version) {
		if(version.startsWith(V1)) {
			return new HadoopV1();
		}
		if(version.startsWith(V2)) {
			return new HadoopV2();
		}
		return null;
	}
	
	/**
	 * 根据指定安装文件得到Hadoop版本
	 * 
	 * @param file
	 * @return
	 */
	public static Hadoop getFromFile(String file) {
		if(file == null) {
			return null;
		}
		
		File input = new File(file);
		InputStream is = null;
		CompressorInputStream in = null;
		TarArchiveInputStream tin = null;
		Hadoop hadoop = null;
		try {
			is = new FileInputStream(input);
			in = new GzipCompressorInputStream(is, true);
			tin = new TarArchiveInputStream(in);
			TarArchiveEntry entry = tin.getNextTarEntry();
			if (!entry.isDirectory()) {
				System.out.println("file error!");
				return null;
			}
			
			String dir = entry.getName().replaceAll("/", "");
			while (entry != null) {
			   String name = entry.getName();
			   if(entry.isDirectory() && name != null) {
				   if(name.contains(dir + "/conf/")) {
					   hadoop = new HadoopV1();
					   break;
				   }
				   if(name.contains(dir + "/etc/hadoop/")) {
					   hadoop = new HadoopV2();
					   break;
				   }
			   }
			   entry = tin.getNextTarEntry();
			}
			hadoop.setLocalPath(file);
			hadoop.setVer(dir);
			hadoop.setInstallFile(dir + ".tar.gz");
			hadoop.setTmpPath(Path.TMP + "/" + dir + ".tar.gz");
			hadoop.setHome(Path.HADOOP_DISTR + "/" + dir);
			hadoop.setCfgPath();
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
		return hadoop;
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
		return hadoopHome;
	}
	/**
	 * @param home the home to set
	 */
	public void setHome(String home) {
		this.hadoopHome = home;
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
		cfgPath = hadoopHome + "/conf/";
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
		return hadoopHome + "/bin/hadoop namenode -format";
	}

	public String startJournal() {
		return hadoopHome + "/sbin/hadoop-daemon.sh start journalnode";
	}
	
	public String startHdfs() {
		return hadoopHome + "/sbin/start-dfs.sh";
	}
	
	public String startYarn() {
		return hadoopHome + "/sbin/start-yarn.sh";
	}
	
	public String startNameNode() {
		return hadoopHome + "/sbin/hadoop-daemon.sh start namenode";
	}
	
	public String activeNameNode(String NameNode) {
		return hadoopHome + "/bin/hdfs haadmin -transitionToActive " + NameNode;
	}
	
	public String failover(String standby, String active) {
		return hadoopHome + "/bin/hdfs haadmin -failover " + standby + " " + active;
	}
	
	public String stopNameNode() {
		return hadoopHome + "/sbin/hadoop-daemon.sh stop namenode";
	}
	
	public String stopHdfs() {
		return hadoopHome + "/sbin/stop-dfs.sh";
	}
	
	public String stopYarn() {
		return hadoopHome + "/sbin/stop-yarn.sh";
	}
	
	public String bootstrapStandby() {
		return hadoopHome + "/bin/hdfs namenode -bootstrapStandby";
	}
	
	public String formatZookeeper() {
		return hadoopHome + "/bin/hdfs zkfc -formatZK";
	}
}
