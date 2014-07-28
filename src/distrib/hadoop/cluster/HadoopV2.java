package distrib.hadoop.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import distrib.hadoop.exception.AuthException;
import distrib.hadoop.host.Host;
import distrib.hadoop.util.Path;

public class HadoopV2 extends Hadoop {

	/**
	 * 构造方法
	 */
	public HadoopV2() {
		ver = "hadoop-2.4.0";
		installFile = ver + ".tar.gz";
		tmpPath = Path.TMP + "/" + installFile;
		hadoopHome = Path.HADOOP_DISTR + "/" + ver;
		localPath = Path.TMP_LOCAL + "/" + installFile;
		cfgPath = hadoopHome + "/etc/hadoop/";
	}
	
	/**
	 * 配置hdfs-site.xml命令
	 * 
	 * @param repCounts
	 *            复制次数
	 * @param userHome
	 *            当前用户目录
	 * @return
	 */
	public void configHdfsSite(int repCounts, String userHome) {
		cmds.add(cfgHdfsSite("sed -i '/<configuration>/,/<\\/configuration>/d'"));
		cmds.add(cfgHdfsSite("echo '<configuration>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.nameservices</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + CLUST_NAME + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));
		
		Cluster cluster = Cluster.getInstance();
		Host nn = cluster.getNameNode();
		Host snn = cluster.getSecNameNode();
		List<Host> nameNodeList = new ArrayList<Host>();
		nameNodeList.add(nn);
		if(snn != null) {
			nameNodeList.add(snn);
		}
		
		String nnList = nn.getHostName();
		if(snn != null) {
			nnList = nnList + "," + snn.getHostName();
		}

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.ha.namenodes." + CLUST_NAME + "</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + nnList + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.replication</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + repCounts + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.namenode.name.dir</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + userHome + "/"
				+ Path.HDFS_DIR_NAME + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.datanode.data.dir</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + userHome + "/"
				+ Path.HDFS_DIR_DATA + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.journalnode.edits.dir</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + userHome + "/"
				+ Path.HDFS_DIR_JOURNAL + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));
		
		for(Host n : nameNodeList) {
			String id = CLUST_NAME + "." + n.getHostName();
			String ip = n.getIp();
			
			cmds.add(cfgHdfsSite("echo '  <property>' >>"));
			cmds.add(cfgHdfsSite("echo '    <name>dfs.namenode.rpc-address." + id + "</name>' >>"));
			cmds.add(cfgHdfsSite("echo '    <value>" + ip + ":8020</value>' >>"));
			cmds.add(cfgHdfsSite("echo '  </property>' >>"));
			
			cmds.add(cfgHdfsSite("echo '  <property>' >>"));
			cmds.add(cfgHdfsSite("echo '    <name>dfs.namenode.http-address." + id + "</name>' >>"));
			cmds.add(cfgHdfsSite("echo '    <value>" + ip + ":50070</value>' >>"));
			cmds.add(cfgHdfsSite("echo '  </property>' >>"));			
		}

		/* 添加日志节点 */
		StringBuffer url = new StringBuffer("qjournal://");
		for(Host j : cluster.getJournalList()) {
			if(url.indexOf("8485") > 0) {
				url.append(";");
			}
			url.append(j.getIp());
			url.append(":8485");
		}
		url.append("/");
		url.append(CLUST_NAME);
		
		if(url.indexOf("8485") > 0) {
			cmds.add(cfgHdfsSite("echo '  <property>' >>"));
			cmds.add(cfgHdfsSite("echo '    <name>dfs.namenode.shared.edits.dir</name>' >>"));
			cmds.add(cfgHdfsSite("echo '    <value>" + url.toString() + "</value>' >>"));
			cmds.add(cfgHdfsSite("echo '  </property>' >>"));
		}

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.client.failover.proxy.provider." + CLUST_NAME + "</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.ha.fencing.methods</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>sshfence</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.ha.fencing.ssh.private-key-files</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + userHome + "/.ssh/id_rsa</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));
		
		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.ha.automatic-failover.enabled</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + cluster.isHaAutoRecover() + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));
		
		cmds.add(cfgHdfsSite("echo '</configuration>' >>"));
	}

	/**
	 * 配置core-site.xml命令
	 * 
	 * @param dfsIp
	 * @param userHome
	 * @return
	 */
	public void configCoreSite(String dfsIp, String userHome) {
		cmds.add(cfgCoreSite("sed -i '/<configuration>/,/<\\/configuration>/d'"));
		cmds.add(cfgCoreSite("echo '<configuration>' >>"));

		cmds.add(cfgCoreSite("echo '  <property>' >>"));
		cmds.add(cfgCoreSite("echo '    <name>fs.defaultFS</name>' >>"));
		cmds.add(cfgCoreSite("echo '    <value>hdfs://" + CLUST_NAME + "</value>' >>"));
		cmds.add(cfgCoreSite("echo '  </property>' >>"));

		cmds.add(cfgCoreSite("echo '  <property>' >>"));
		cmds.add(cfgCoreSite("echo '    <name>hadoop.tmp.dir</name>' >>"));
		cmds.add(cfgCoreSite("echo '    <value>" + userHome + "/"
				+ Path.HDFS_DIR_TMP + "</value>' >>"));
		cmds.add(cfgCoreSite("echo '  </property>' >>"));
		
		/* 添加Zookeeper节点 */
		Cluster cluster = Cluster.getInstance();
		StringBuffer quorum = new StringBuffer();
		for(Host j : cluster.getJournalList()) {
			if(quorum.indexOf("2181") > 0) {
				quorum.append(",");
			}
			quorum.append(j.getIp());
			quorum.append(":2181");
		}
		
		if(quorum.indexOf("2181") > 0) {
			cmds.add(cfgCoreSite("echo '  <property>' >>"));
			cmds.add(cfgCoreSite("echo '    <name>ha.zookeeper.quorum</name>' >>"));
			cmds.add(cfgCoreSite("echo '    <value>" + quorum.toString() + "</value>' >>"));
			cmds.add(cfgCoreSite("echo '  </property>' >>"));
		}

		cmds.add(cfgCoreSite("echo '</configuration>' >>"));
	}

	/**
	 * 配置hadoop-env.sh命令
	 * 
	 * @param javaHome
	 */
	public void configHadoopEnv(String userHome) {
		cmds.add(cfgHadoopEnv("sed -i '/" + COMMENT_START + "/,/"
				+ COMMENT_END + "/d'"));

		cmds.add(cfgHadoopEnv("sed -i '/export JAVA_HOME/d'"));
		cmds.add(cfgHadoopEnv("sed -i '/export HADOOP_PREFIX/d'"));
		cmds.add(cfgHadoopEnv("sed -i '/export HADOOP_MAPRED_HOME/d'"));
		cmds.add(cfgHadoopEnv("sed -i '/export HADOOP_COMMON_HOME/d'"));
		cmds.add(cfgHadoopEnv("sed -i '/export HADOOP_HDFS_HOME/d'"));
		cmds.add(cfgHadoopEnv("sed -i '/export YARN_HOME/d'"));
		cmds.add(cfgHadoopEnv("sed -i '/export HADOOP_CONF_DIR/d'"));
		cmds.add(cfgHadoopEnv("sed -i '/export HDFS_CONF_DIR/d'"));
		cmds.add(cfgHadoopEnv("sed -i '/export YARN_CONF_DIR/d'"));

		cmds.add(cfgHadoopEnv("echo '" + COMMENT_START + "' >>"));

		cmds.add(cfgHadoopEnv("echo 'export JAVA_HOME=" + userHome + "/" + Path.JAVA_HOME + "' >>"));
		cmds.add(cfgHadoopEnv("echo 'export HADOOP_PREFIX=" + userHome + "/"
				+ hadoopHome + "' >>"));
		cmds.add(cfgHadoopEnv("echo 'export HADOOP_MAPRED_HOME=${HADOOP_PREFIX}' >>"));
		cmds.add(cfgHadoopEnv("echo 'export HADOOP_COMMON_HOME=${HADOOP_PREFIX}' >>"));
		cmds.add(cfgHadoopEnv("echo 'export HADOOP_HDFS_HOME=${HADOOP_PREFIX}' >>"));
		cmds.add(cfgHadoopEnv("echo 'export YARN_HOME=${HADOOP_PREFIX}' >>"));
		cmds.add(cfgHadoopEnv("echo 'export HADOOP_CONF_DIR=${HADOOP_PREFIX}/etc/hadoop' >>"));
		cmds.add(cfgHadoopEnv("echo 'export HDFS_CONF_DIR=${HADOOP_PREFIX}/etc/hadoop' >>"));
		cmds.add(cfgHadoopEnv("echo 'export YARN_CONF_DIR=${HADOOP_PREFIX}/etc/hadoop' >>"));

		cmds.add(cfgHadoopEnv("echo '" + COMMENT_END + "' >>"));
	}

	/**
	 * 配置mapred-site.xml命令
	 * 
	 * @param JobTrackerIp
	 */
	public void configMapredSite(String jobTrackerIp) {
		cmds.add(cfgMapredSite("sed -i '/<configuration>/,/<\\/configuration>/d'"));
		cmds.add(cfgMapredSite("echo '<configuration>' >>"));

		cmds.add(cfgMapredSite("echo '  <property>' >>"));
		cmds.add(cfgMapredSite("echo '    <name>mapreduce.framework.name</name>' >>"));
		cmds.add(cfgMapredSite("echo '    <value>yarn</value>' >>"));
		cmds.add(cfgMapredSite("echo '  </property>' >>"));

		cmds.add(cfgMapredSite("echo '  <property>' >>"));
		cmds.add(cfgMapredSite("echo '    <name>mapreduce.job.tracker</name>' >>"));
		cmds.add(cfgMapredSite("echo '    <value>hdfs://" + jobTrackerIp
				+ ":9001</value>' >>"));
		cmds.add(cfgMapredSite("echo '  </property>' >>"));

		cmds.add(cfgMapredSite("echo '</configuration>' >>"));
	}

	/**
	 * 配置yarn-site.xml
	 * 
	 * @param cmd
	 */
	protected String cfgYarnSite(String cmd) {
		return cfgFile(cmd, Path.HADOOP_YARN_SITE);
	}

	/**
	 * 配置yarn-site.xml
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configYarnSite(String rm) {
		cmds.add(cfgYarnSite("sed -i '/<configuration>/,/<\\/configuration>/d'"));
		cmds.add(cfgYarnSite("echo '<configuration>' >>"));

		cmds.add(cfgYarnSite("echo '  <property>' >>"));
		cmds.add(cfgYarnSite("echo '    <name>yarn.resourcemanager.address</name>' >>"));
		cmds.add(cfgYarnSite("echo '    <value>" + rm + ":18040</value>' >>"));
		cmds.add(cfgYarnSite("echo '  </property>' >>"));

		cmds.add(cfgYarnSite("echo '  <property>' >>"));
		cmds.add(cfgYarnSite("echo '    <name>yarn.resourcemanager.scheduler.address</name>' >>"));
		cmds.add(cfgYarnSite("echo '    <value>" + rm + ":18030</value>' >>"));
		cmds.add(cfgYarnSite("echo '  </property>' >>"));

		cmds.add(cfgYarnSite("echo '  <property>' >>"));
		cmds.add(cfgYarnSite("echo '    <name>yarn.resourcemanager.webapp.address</name>' >>"));
		cmds.add(cfgYarnSite("echo '    <value>" + rm + ":18088</value>' >>"));
		cmds.add(cfgYarnSite("echo '  </property>' >>"));

		cmds.add(cfgYarnSite("echo '  <property>' >>"));
		cmds.add(cfgYarnSite("echo '    <name>yarn.resourcemanager.resource-tracker.address</name>' >>"));
		cmds.add(cfgYarnSite("echo '    <value>" + rm + ":18025</value>' >>"));
		cmds.add(cfgYarnSite("echo '  </property>' >>"));

		cmds.add(cfgYarnSite("echo '  <property>' >>"));
		cmds.add(cfgYarnSite("echo '    <name>yarn.resourcemanager.admin.address</name>' >>"));
		cmds.add(cfgYarnSite("echo '    <value>" + rm + ":18141</value>' >>"));
		cmds.add(cfgYarnSite("echo '  </property>' >>"));

		cmds.add(cfgYarnSite("echo '  <property>' >>"));
		cmds.add(cfgYarnSite("echo '    <name>yarn.nodemanager.aux-services</name>' >>"));
		cmds.add(cfgYarnSite("echo '    <value>mapreduce_shuffle</value>' >>"));
		cmds.add(cfgYarnSite("echo '  </property>' >>"));

		cmds.add(cfgYarnSite("echo '</configuration>' >>"));
	}

	/**
	 * 配置用户的profile文件，添加环境变量
	 * 
	 * @param shell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configUserProfile(String userHome) {
		String pathEnv = "PATH=$PATH:$HADOOP_PREFIX/bin:$HADOOP_PREFIX/sbin:$JAVA_HOME/bin";

		cmds.add(cfgUsrProfile("sed -i '/" + COMMENT_START + "/,/"
				+ COMMENT_END + "/d'"));
		cmds.add(cfgUsrProfile("echo '" + COMMENT_START + "' >>"));
		cmds.add(cfgUsrProfile("echo 'HADOOP_PREFIX=" + userHome + "/" + hadoopHome
				+ "' >>"));
		cmds.add(cfgUsrProfile("echo 'JAVA_HOME=" + userHome + "/" + Path.JAVA_HOME + "' >>"));
		cmds.add(cfgUsrProfile("echo '" + pathEnv + "' >>"));
		cmds.add(cfgUsrProfile("echo 'export PATH' >>"));
		cmds.add(cfgUsrProfile("echo 'HADOOP_MAPRED_HOME=${HADOOP_PREFIX}' >>"));
		cmds.add(cfgUsrProfile("echo 'HADOOP_COMMON_HOME=${HADOOP_PREFIX}' >>"));
		cmds.add(cfgUsrProfile("echo 'HADOOP_HDFS_HOME=${HADOOP_PREFIX}' >>"));
		cmds.add(cfgUsrProfile("echo 'YARN_HOME=${HADOOP_PREFIX}' >>"));
		cmds.add(cfgUsrProfile("echo 'HADOOP_CONF_DIR=${HADOOP_PREFIX}/etc/hadoop' >>"));
		cmds.add(cfgUsrProfile("echo 'HDFS_CONF_DIR=${HADOOP_PREFIX}/etc/hadoop' >>"));
		cmds.add(cfgUsrProfile("echo 'YARN_CONF_DIR=${HADOOP_PREFIX}/etc/hadoop' >>"));
		cmds.add(cfgUsrProfile("echo '" + COMMENT_END + "' >>"));

		cmds.add("export JAVA_HOME=" + userHome + "/" + Path.JAVA_HOME);
		cmds.add("export HADOOP_PREFIX=" + userHome + "/" + hadoopHome);
		cmds.add("export " + pathEnv);
		cmds.add("export HADOOP_MAPRED_HOME=" + userHome + "/" + hadoopHome);
		cmds.add("export HADOOP_COMMON_HOME=" + userHome + "/" + hadoopHome);
		cmds.add("export HADOOP_HDFS_HOME=" + userHome + "/" + hadoopHome);
		cmds.add("export YARN_HOME=" + userHome + "/" + hadoopHome);
		cmds.add("export HADOOP_CONF_DIR=" + userHome + "/" + cfgPath);
		cmds.add("export HDFS_CONF_DIR=" + userHome + "/" + cfgPath);
		cmds.add("export YARN_CONF_DIR=" + userHome + "/" + cfgPath);
	}
	
	@Override
	public void prepareConfig() {
		Cluster cluster = Cluster.getInstance();
		
		Host nn = cluster.getNameNode();
		List<Host> dn = cluster.getDataNodeList();
		List<Host> nnList = cluster.getNameNodeList();
		
		/* Hadoop2可以配HA */
		cluster.prepareJournalList();
		
		String userHome = nn.getUserHome();
		int rep = getRepCount();
		
		configUserProfile(userHome);
		configHadoopEnv(userHome);
		configCoreSite(nn.getIp(), userHome);
		configHdfsSite(rep, userHome);
		configMapredSite(nn.getIp());
		configYarnSite(nn.getIp());

		configMaster(nnList);
		configSlave(dn);
	}
	
	/**
	 * 设置路径
	 */
	public void setCfgPath() {
		cfgPath = hadoopHome + "/etc/hadoop/";
	}
}
