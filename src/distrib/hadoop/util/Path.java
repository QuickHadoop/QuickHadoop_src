package distrib.hadoop.util;

import distrib.hadoop.host.Host;

public class Path {
	public static final String TMP = "distr_hadoop_tmp";
	public static final String RSA = TMP + "/id_rsa";
	public static final String RSA_PUB = RSA + ".pub";
	public static final String RSA_SCP_NAME = "rsa_scp.pub";
	public static final String RSA_SCP_FULL_PATH = TMP + "/" + RSA_SCP_NAME;
	public static final String SSH_DIR = ".ssh/";
	public static final String SSH_AUTH = SSH_DIR + "authorized_keys";
	public static final String TMP_LOCAL = System.getProperty("user.home");
	public static final String RSA_LOCAL = TMP_LOCAL + "/id_rsa.pub";
	
	public static final String HADOOP_DISTR = "hadoop_distr";
	public static final String HADOOP_ENV = "hadoop-env.sh";
	public static final String HADOOP_CORE_SITE = "core-site.xml";
	public static final String HADOOP_HDFS_SITE = "hdfs-site.xml";
	public static final String HADOOP_MAPRED_SITE = "mapred-site.xml";
	public static final String HADOOP_YARN_SITE = "yarn-site.xml";
	public static final String HADOOP_MASTER = "masters";
	public static final String HADOOP_SLAVES = "slaves";

	public static final String ZOOKEEPER_DIR = HADOOP_DISTR + "/dir_for_zookeeper";
	public static final String ZOOKEEPER_DATA_DIR = ZOOKEEPER_DIR + "/zkdata";
	public static final String ZOOKEEPER_DATA_LOG_DIR = ZOOKEEPER_DIR + "/zkdatalog";
	public static final String ZOOKEEPER_LOG_DIR = ZOOKEEPER_DIR + "/logs";
	public static final String ZOOKEEPER_ID = ZOOKEEPER_DATA_DIR + "/myid";
	
	public static final String HBASE_ENV = "hbase-env.sh";
	public static final String HBASE_SITE = "hbase-site.xml";
	public static final String HBASE_REG = "regionservers";
	
	public static final String JAVA_FILE = "jre1.6.0_24.tar.gz";
	public static final String JAVA_HOME = HADOOP_DISTR + "/jre1.6.0_24";
	public static final String JAVA_PATH = TMP + "/" + JAVA_FILE;

	public static final String HDFS_DIR = HADOOP_DISTR + "/dir_for_hdfs"; 
	public static final String HDFS_DIR_NAME = HDFS_DIR + "/name"; 
	public static final String HDFS_DIR_DATA = HDFS_DIR + "/data"; 
	public static final String HDFS_DIR_JOURNAL = HDFS_DIR + "/journal"; 
	public static final String HDFS_DIR_TMP = HDFS_DIR + "/tmp"; 
	
	public static final String USR_PROFILE = ".bash_profile";
	public static final String ETC_HOSTS = "/etc/hosts";
	
	
	/**
	 * �õ�����"ip@user:/"���ַ�
	 * 
	 * @param h
	 * @return
	 */
	public static String hostAddr(Host h) {
		return h.getUserName() + "@" + h.getIp() + ":";
	}
}
