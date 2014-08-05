package distrib.hadoop.cluster;

import java.io.IOException;
import java.util.List;

import distrib.hadoop.exception.AuthException;
import distrib.hadoop.host.Host;
import distrib.hadoop.util.Path;

public class HadoopV1 extends Hadoop {

	/**
	 * 构造方法
	 */
	public HadoopV1() {
		ver = "hadoop-1.2.1";
		installFile = ver + "-bin.tar.gz";
		tmpPath = Path.TMP + "/" + installFile;
		hadoopHome = Path.HADOOP_DISTR + "/" + ver;
		localPath = Path.TMP_LOCAL + "/" + installFile;
		cfgPath = hadoopHome + "/conf/";
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
		cmds.add(cfgHdfsSite("echo '    <name>dfs.replication</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + repCounts + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.name.dir</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + userHome + "/"
				+ Path.HDFS_DIR_NAME + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.data.dir</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + userHome + "/"
				+ Path.HDFS_DIR_DATA + "</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));

		Cluster cluster = Cluster.getInstance();
		Host nn = cluster.getNameNode();
		Host snn = cluster.getSecNameNode();
		String secIp = snn == null ? nn.getIp() : snn.getIp();
		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.http.address</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + nn.getIp() + ":50070</value>' >>"));
		cmds.add(cfgHdfsSite("echo '  </property>' >>"));
		
		cmds.add(cfgHdfsSite("echo '  <property>' >>"));
		cmds.add(cfgHdfsSite("echo '    <name>dfs.secondary.http.address</name>' >>"));
		cmds.add(cfgHdfsSite("echo '    <value>" + secIp + ":50070</value>' >>"));
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
		cmds.add(cfgCoreSite("echo '    <name>fs.default.name</name>' >>"));
		cmds.add(cfgCoreSite("echo '    <value>hdfs://" + dfsIp
				+ ":9000</value>' >>"));
		cmds.add(cfgCoreSite("echo '  </property>' >>"));

		cmds.add(cfgCoreSite("echo '  <property>' >>"));
		cmds.add(cfgCoreSite("echo '    <name>hadoop.tmp.dir</name>' >>"));
		cmds.add(cfgCoreSite("echo '    <value>" + userHome + "/"
				+ Path.HDFS_DIR_TMP + "</value>' >>"));
		cmds.add(cfgCoreSite("echo '  </property>' >>"));

		cmds.add(cfgCoreSite("echo '</configuration>' >>"));
	}

	/**
	 * 配置hadoop-env.sh命令
	 * 
	 * @param javaHome
	 */
	public void configHadoopEnv(String userHome) {
		String java = Jre.getInstance().getHome();
		cmds.add(cfgHadoopEnv("sed -i '/export JAVA_HOME/d'"));
		cmds.add(cfgHadoopEnv("echo export JAVA_HOME=" + userHome + "/" + java + " >>"));
	}

	/**
	 * 配置mapred-site.xml命令
	 * 
	 * @param JobTrackerIp
	 */
	public void configMapredSite(String JobTrackerIp) {
		cmds.add(cfgMapredSite("sed -i '/<configuration>/,/<\\/configuration>/d'"));
		cmds.add(cfgMapredSite("echo '<configuration>' >>"));

		cmds.add(cfgMapredSite("echo '  <property>' >>"));
		cmds.add(cfgMapredSite("echo '    <name>mapred.job.tracker</name>' >>"));
		cmds.add(cfgMapredSite("echo '    <value>" + JobTrackerIp
				+ ":9001</value>' >>"));
		cmds.add(cfgMapredSite("echo '  </property>' >>"));

		cmds.add(cfgMapredSite("echo '</configuration>' >>"));
	}

	/**
	 * 配置系统路径
	 * 
	 * @param userHome
	 * @throws AuthException
	 * @throws IOException
	 */
	public void configUserProfile(String userHome) {
		String java = Jre.getInstance().getHome();
		String pathEnv = "PATH=$PATH:$HADOOP_HOME/bin:$JAVA_HOME/bin";

		cmds.add(cfgUsrProfile("sed -i '/" + COMMENT_START + "/,/"
				+ COMMENT_END + "/d'"));
		cmds.add(cfgUsrProfile("echo '" + COMMENT_START + "' >>"));
		cmds.add(cfgUsrProfile("echo 'HADOOP_HOME=" + userHome + "/" + hadoopHome
				+ "' >>"));

		cmds.add(cfgUsrProfile("echo 'JAVA_HOME=" + userHome + "/" + java + "' >>"));
		cmds.add(cfgUsrProfile("echo '" + pathEnv + "' >>"));
		cmds.add(cfgUsrProfile("echo 'export PATH' >>"));
		cmds.add(cfgUsrProfile("echo '" + COMMENT_END + "' >>"));

		cmds.add("export JAVA_HOME=" + userHome + "/" + java);
		cmds.add("export " + pathEnv);
	}

	@Override
	public void prepareConfig() {
		Cluster cluster = Cluster.getInstance();
		
		Host nn = cluster.getNameNode();
		List<Host> dn = cluster.getDataNodeList();
		List<Host> nnList = cluster.getNameNodeList();
		
		String userHome = nn.getUserHome();
		int rep = getRepCount();
		
		configUserProfile(userHome);
		configHadoopEnv(userHome);
		configCoreSite(nn.getIp(), userHome);
		configHdfsSite(rep, userHome);
		configMapredSite(nn.getIp());

		configMaster(nnList);
		configSlave(dn);
	}
	
	/**
	 * 设置路径
	 */
	public void setCfgPath() {
		cfgPath = hadoopHome + "/conf/";
	}
}

