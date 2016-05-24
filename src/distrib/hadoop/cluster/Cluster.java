package distrib.hadoop.cluster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import distrib.hadoop.exception.AuthException;
import distrib.hadoop.exception.InstallException;
import distrib.hadoop.host.Host;
import distrib.hadoop.resource.Messages;
import distrib.hadoop.shell.RemoteShell;
import distrib.hadoop.thread.ThreadPool;
import distrib.hadoop.util.Path;
import distrib.hadoop.util.RetNo;
import distrib.hadoop.util.Util;

public class Cluster {

	/** 分布式主机节点 */
	private List<Host> hostList = new ArrayList<Host>();

	/** Journal主机节点 */
	private List<Host> journalList = new ArrayList<Host>();
	
	/** Zookeeper主机节点 */
	private List<Host> zooKeeperList = new ArrayList<Host>();
	
	/** HBase主机节点 */
	private List<Host> hBaseList = new ArrayList<Host>();
	
	/** HMasters */
	private List<Host> hMasterList = new ArrayList<Host>();
	
	/** HRegionServers */
	private List<Host> hRegionList = new ArrayList<Host>();
	
	/** 是否支持Spark */
	private boolean supportSpark;
	
	/** 是否支持HBase */
	private boolean supportHbase;
	
	/** 是否支持Zookeeper */
	private boolean supportZookeeper;
	
	/** 是否支持HA */
	private boolean haAutoRecover;
	
	/** Hadoop */
	private Hadoop hadoop;
	
	/** Zookeeper */
	private Zookeeper zookeeper;
	
	/** HBase */
	private HBase hBase;
	
	/** Spark */
	private Spark spark;
	
	/** whether is setuped */
	private boolean setUped = false;
	
	/** 错误码 */
	private int exitCode;
	
	/** 集群实例 */
	public static Cluster instance;
	
	public static final String NN_NAME = "NameNode1";
	public static final String SN_NAME = "NameNode2";
	public static final String DN_NAME = "DataNode";
	
	public static Cluster getInstance() {
		if(instance == null) {
			instance = new Cluster();
		}
		return instance;
	}
	
	private Cluster() {

	}
	
	/**
	 * 主机间共享密钥
	 */
	public void shareRsa() throws InstallException {
		exitCode = RemoteShell.OK;
		try {
			genRsaKey();
			
			for(int i = 0; i < hostList.size(); i++) {
				for(int j = i; j < hostList.size(); j++) {
					shareRsa(hostList.get(i), hostList.get(j));
					shareRsa(hostList.get(j), hostList.get(i));
				}
			}
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
			throw new InstallException(Messages.getString("Exception.rsa.failed"));
		}
	}
	
	/**
	 * 获取NameNode节点
	 * 
	 * @return
	 */
	public Host getNameNode() {
		for(Host host : hostList) {
			if(NN_NAME.equals(host.getHostName())) {
				return host;
			}
		}
		/* 为了兼容之前的版本，如果没有NameNode1，但是有NameNode也行 */
		for(Host host : hostList) {
			if("NameNode".equals(host.getHostName())) {
				return host;
			}
		}
		return null;
	}
	
	/**
	 * 获取SecondNameNode节点
	 * 
	 * @return
	 */
	public Host getSecNameNode() {
		for(Host host : hostList) {
			if(SN_NAME.equals(host.getHostName())) {
				return host;
			}
		}
		return null;
	}

	/**
	 * 获取NameNode节点列表
	 * 
	 * @return
	 */
	public List<Host> getNameNodeList() {
		List<Host> nameNodeList = new ArrayList<Host>();
		Host nameNode = getNameNode();
		Host secNameNode = getSecNameNode();
		if(nameNode != null) {
			nameNodeList.add(nameNode);			
		}
		if(secNameNode != null) {
			nameNodeList.add(secNameNode);			
		}
		return nameNodeList;
	}
	
	/**
	 * 获取DataNode节点列表
	 * 
	 * @return
	 */
	public List<Host> getDataNodeList() {
		List<Host> dataNodeList = new ArrayList<Host>();
		for(Host host : hostList) {
			String hostName = host.getHostName();
			if(hostName.startsWith(DN_NAME)) {
				dataNodeList.add(host);
			}
		}
		return dataNodeList;
	}
	
	/**
	 * 生产公共秘钥
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	private void genRsaKey() throws InstallException {
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						host.genRsaKey();
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					}
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.rsa.failed"));
		}
	}
	
	/**
	 * 两台主机之间共享RSA
	 * 
	 * @param client
	 * @param server
	 */
	private void shareRsa(Host client, Host server) throws AuthException, IOException {
		RemoteShell cShell = client.getShell();
		RemoteShell sShell = server.getShell();
		
		if(!client.equals(server)) {			
			cShell.getFile(Path.RSA_PUB, Path.TMP_LOCAL);
			sShell.putFile(Path.RSA_LOCAL, Path.RSA_SCP_NAME, Path.TMP);
			sShell.excute("cat " + Path.RSA_SCP_FULL_PATH + " >> " + Path.SSH_AUTH);			
		}
		
		cShell.excute("ssh -o StrictHostKeyChecking=no " + server.getIp());
		cShell.excute("ssh -o StrictHostKeyChecking=no " + server.getHostName());
	}
	
	/**
	 * 安装流程
	 * 
	 * @param nnShell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void install() throws InstallException {
		createInstallDir();
		setupHadoop();
		if(supportZookeeper) {
			setupZookeeper(); 						
		}
		if(supportHbase) {
			setupHBase();
		}
		if(supportSpark) {
			setupSpark();
		}
		cleanTmpDir();
	}
	
	/**
	 * 安装之前的准备
	 */
	public void prepareWork() {
		if(supportZookeeper) {
			zookeeper = Zookeeper.getInstance();
			zooKeeperList.clear();
			for(Host h : hostList) {
				if(h.isZookeeperProperty().getValue()) {
					zooKeeperList.add(h);					
				}
			}
		}
		
		if(supportHbase) {
			hBase = HBase.getInstance();
			hBaseList.clear();
			hMasterList.clear();
			hRegionList.clear();
			
			for(Host h : hostList) {
				if(h.isHBaseProperty().getValue()) {
					hBaseList.add(h);
				}
			}

			for(int i = 0; i < hBaseList.size(); i++) {
				if(i == 0) {
					hMasterList.add(hBaseList.get(i));
				} else {
					hRegionList.add(hBaseList.get(i));
				}
			}
		}
		
		journalList.clear();
		for(Host h : hostList) {
			if(h.isJournalNodeProperty().getValue()) {
				journalList.add(h);
			}
		}
		
		if(supportSpark) {
			spark = Spark.getInstance();
		}
	}

	/**
	 * 指定journalList
	 */
	public void prepareJournalList() {
		journalList.clear();
		
		if(getSecNameNode() != null) {
			for(Host h : hostList) {
				if(h.isJournalNodeProperty().getValue()) {
					journalList.add(h);
				}
			}			
		}
	}
	
	/**
	 * 安装、配置Hadoop
	 * 
	 * @param nnShell
	 * @throws AuthException
	 * @throws IOException
	 */
	public void setupHadoop() throws InstallException {
		exitCode = RemoteShell.OK;
		hadoop.prepareConfig();
		
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					setupHadoop(host);
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.hadoop.install.failed"));
		}
	}
	
	/**
	 * 安装、配置Zookeeper
	 * 
	 * @throws InstallException
	 */
	public void setupZookeeper() throws InstallException {
		if(zookeeper == null) {
			return;
		}
		
		exitCode = RemoteShell.OK;
		zookeeper.prepareConfig();
				
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : zooKeeperList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					setupZookeeper(host);
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.zookeeper.install.failed"));
		}
	}
	
	/**
	 * 安装、配置HBase
	 * 
	 * @throws InstallException
	 */
	public void setupHBase() throws InstallException {
		if(hBase == null) {
			return;
		}
		
		exitCode = RemoteShell.OK;
		hBase.prepareConfig();
		
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hBaseList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					setupHBase(host);
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.hbase.install.failed"));
		}
	}
	
	/**
	 * 安装、配置Spark
	 * 
	 * @throws InstallException
	 */
	public void setupSpark() throws InstallException {
		if(spark == null) {
			return;
		}
		
		exitCode = RemoteShell.OK;
		spark.prepareConfig();
		
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					setupSpark(host);
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.spark.install.failed"));
		}
	}
	
	/**
	 * 获取主机信息
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void getHostEnv() throws InstallException {
		exitCode = RemoteShell.OK;
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						host.getHostEnv();
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					} 
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.install.failed"));
		}
	}
	
	/**
	 * 一键停止
	 */
	public void stopAll() throws AuthException, IOException {
		Host nameNode = getNameNode();
		if(nameNode == null) {
			return;
		}
		
		nameNode.getShell().excutePtySudo(spark.stop());
		stopHBase();	
		nameNode.getShell().excutePtySudo("stop-all.sh");
		stopZookeeper();			
	}
	
	/**
	 * 一键启动
	 */
	public void startAll() throws AuthException, IOException {
		Host nameNode = getNameNode();
		if(nameNode == null) {
			return;
		}
		
		startZookeeper();
		nameNode.getShell().excutePtySudo("start-all.sh");
		
		if(supportHbase) {
			startHBase();			
		}
		
		if(supportSpark) {			
			startSpark();
		}
	}
	
	/**
	 * 创建安装目录
	 * 
	 * @param nnShell
	 * @throws AuthException
	 * @throws IOException
	 */
	private void createInstallDir() throws InstallException {
		exitCode = RemoteShell.OK;
		
		for(final Host host : hostList) {
			try {
				host.createHadoopDir();
			} catch (Exception e) {
				throw new InstallException(Messages.getString(
						"Exception.hadoop.install.failed"));
			}
		}
	}
	
	/**
	 * 安装、配置Hadoop
	 * 
	 * @param nnShell
	 * @throws AuthException
	 * @throws IOException
	 */
	private void setupHadoop(Host host) {
		try {
			host.installJava();
			
			host.installHadoop();
			host.configHadoop();
		} catch (Exception e) {
			e.printStackTrace();
			exit(RemoteShell.FAILED);
		}
	}
	
	/**
	 * 安装、配置Spark
	 * 
	 * @param host
	 */
	private void setupSpark(Host host) {
		try {
			host.installScala();
			
			host.installSpark();
			host.configSpark();
		} catch (Exception e) {
			e.printStackTrace();
			exit(RemoteShell.FAILED);
		}
	}
	
	/**
	 * 安装、配置Zookeeper
	 * 
	 * @param host
	 * @throws AuthException
	 * @throws IOException
	 */
	private void setupZookeeper(Host host) {
		try {
			host.installZookeeper();
			host.configZookeeper();
		} catch (Exception e) {
			e.printStackTrace();
			exit(RemoteShell.FAILED);
		}
	}
	
	/**
	 * 安装、配置HBase
	 * 
	 * @param host
	 */
	private void setupHBase(Host host) {
		try {
			host.installHBase();
			host.configHBase();
		} catch (Exception e) {
			e.printStackTrace();
			exit(RemoteShell.FAILED);
		}
	}
	
	
	/**
	 * 清除临时目录
	 * 
	 * @throws InstallException 
	 */
	private void cleanTmpDir() throws InstallException {
		exitCode = RemoteShell.OK;
		
		for(final Host host : hostList) {
			try {
				host.cleanTmpDir();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 设置主机名称
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void cfgHostName() throws InstallException {
		exitCode = RemoteShell.OK;
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						host.cfgHostName();
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					} 
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.hostname.failed"));
		}
	}

	/**
	 * 设置DNS服务
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void cfgDNS() throws InstallException {
		exitCode = RemoteShell.OK;
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						host.cfgDNS(hostList);
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					}
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.hostname.failed"));
		}
	}
	

	/**
	 * 启动JournalNode
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void startJournalNode() throws InstallException {
		if(getNameNode() == null || getSecNameNode() == null
				|| !(hadoop instanceof HadoopV2)) {
			return;
		}
		
		batchCmd(journalList, hadoop.startJournal());
		
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(
					Messages.getString("Exception.start.journal.failed"));
		}
	}

	/**
	 * 同步NameNode的metadata到Second NameNode
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void bootstrapStandby() throws InstallException {
		exitCode = RemoteShell.OK;
		
		try {
			Host nameNode = getNameNode();
			Host secNameNode = getSecNameNode();
			if(nameNode == null || secNameNode == null
					|| !(hadoop instanceof HadoopV2)) {
				return;
			}
			nameNode.getShell().excutePty(hadoop.startNameNode());
			secNameNode.getShell().excutePty(hadoop.bootstrapStandby());
			secNameNode.getShell().excutePty(hadoop.startNameNode());
			nameNode.getShell().excutePty(hadoop.activeNameNode(nameNode.getHostName()));
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
			throw new InstallException(
					Messages.getString("Exception.bootstrap.standby.failed"));
		}
	}

	/**
	 * 启动Zookeeper
	 * 
	 * @throws InstallException
	 */
	public void startZookeeper() {
		if(zookeeper == null) {
			return;
		}
		batchCmd(zooKeeperList, zookeeper.start());
	}

	/**
	 * 停止Zookeeper
	 * 
	 * @throws InstallException
	 */
	public void stopZookeeper() {
		if(zookeeper == null) {
			return;
		}
		batchCmd(zooKeeperList, zookeeper.stop());
	}
	
	/**
	 * 启动HBase
	 * 
	 * @throws InstallException
	 */
	public void startHBase() {
		if(hBase == null) {
			return;
		}
		
		batchCmd(hMasterList, hBase.start());
	}
	
	/**
	 * 停止HBase
	 * 
	 * @throws InstallException
	 */
	public void stopHBase() {
		if(hBase == null) {
			return;
		}
		
		batchCmd(hMasterList, hBase.stop());
	}
	
	/**
	 * 在指定的主机列表中批量执行命令
	 */
	public void batchCmd(List<Host> hosts, final String... cmds) {
		if(hosts == null || cmds == null) {
			return;
		}
		
		exitCode = RemoteShell.OK;
		
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hosts) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						for(String c : cmds) {
							host.getShell().excutePtySudo(c);
						}
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					}
				}
			});
		}
		ThreadPool.Execute(runList, true);
	}
	
	/**
	 * 格式化NameNode
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void formatNN() throws InstallException {
		exitCode = RemoteShell.OK;
		
		try {
			Host nameNode = getNameNode();
			if(nameNode == null) {
				return;
			}
			RemoteShell shell = nameNode.getShell();
			int cnt = 0;
			while(!shell.fileExists(Path.HDFS_DIR_NAME)) {
				Thread.sleep(1000);
				shell.excutePtySudo(hadoop.getFormatCmd());
				Thread.sleep(1000);
				
				if(cnt++ > 200) {
					throw new InstallException(Messages.getString("Exception.format.failed"));
				}
			}
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
			throw new InstallException(Messages.getString("Exception.format.failed"));
		}
	}
	
	/**
	 * 格式化Zookeeper
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void formatZK() throws InstallException {
		exitCode = RemoteShell.OK;
		
		try {
			Host nameNode = getNameNode();
			if(nameNode == null) {
				return;
			}	
			nameNode.getShell().excutePtySudo(hadoop.formatZookeeper());
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
			throw new InstallException(Messages.getString("Exception.init.failed"));
		}
	}
	
	/**
	 * 启动HDFS
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void startHdfs() throws InstallException {
		exitCode = RemoteShell.OK;
		
		try {
			Host nn = getNameNode();
			if(nn == null) {
				return;
			}		
			nn.getShell().excute(hadoop.startHdfs());
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
			throw new InstallException(Messages.getString("Exception.init.failed"));
		}
	}
	
	/**
	 * 启动Yarn
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void startYarn() throws InstallException {
		exitCode = RemoteShell.OK;
		
		try {
			Host nn = getNameNode();
			if(nn == null) {
				return;
			}		
			nn.getShell().excute(hadoop.startYarn());
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
			throw new InstallException(Messages.getString("Exception.init.failed"));
		}
	}
	
	/**
	 * 启动Spark
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void startSpark()  {
		exitCode = RemoteShell.OK;
		
		try {
			Host nn = getNameNode();
			if(nn == null) {
				return;
			}		
			nn.getShell().excute(spark.start());
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
		}
	}
	
	/**
	 * 初始化HA，让NameNode1主控
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void initFailover() throws InstallException {
		exitCode = RemoteShell.OK;
		
		try {
			Host nn = getNameNode();
			Host snn = getSecNameNode();
			if(nn == null || snn == null) {
				return;
			}		
			nn.getShell().excute(hadoop.failover(snn.getHostName(), nn.getHostName()));
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
			throw new InstallException(Messages.getString("Exception.init.failed"));
		}
	}
	
	/**
	 * 恢复设备到安装Hadoop之前的状态
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void restore() throws AuthException, IOException {
		prepareWork();
		stopAll();
		
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						host.restore();
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					}
				}
			});
		}
		ThreadPool.Execute(runList, true);
	}
	
	/**
	 * 关闭防火墙
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void closeFireWall() throws InstallException {	
		exitCode = RemoteShell.OK;
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						host.closeFireWall();
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					}
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.firewall.failed"));
		}
	}
	
	/**
	 * 登录设备
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void login() throws InstallException {
		exitCode = RemoteShell.OK;
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						if(!host.login()) {
							exit(RemoteShell.FAILED);
						}
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					}
				}
			});
		}
		ThreadPool.Execute(runList, true);
		if(exitCode != RemoteShell.OK) {
			throw new InstallException(Messages.getString("Exception.login.failed"));
		}
	}
	
	/**
	 * 上传文件到所有设备指定路径
	 * 
	 * @throws AuthException
	 * @throws IOException
	 */
	public void putFile(final String localFile, 
			final String remoteFileName,
			final String remotePath) 
			throws AuthException, IOException {	
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						/* 如果文件已经存在，先备份 */
						host.backUpFile(remotePath + "/" + remoteFileName);
						host.putFile(localFile, remoteFileName, remotePath);
					} catch (Exception e) {
						e.printStackTrace();
						exit(RemoteShell.FAILED);
					}
				}
			});
		}
		ThreadPool.Execute(runList, true);
	}

	/**
	 * 初始化
	 * 
	 * @throws InstallException
	 */
	public void initialize() throws InstallException {
		if(supportZookeeper) {
			startZookeeper(); 
			formatZK();
		}

		startJournalNode();
		formatNN();
		bootstrapStandby();
		startHdfs();
		
		if(hadoop instanceof HadoopV2) {
			initFailover();			
		}
		
		if(supportHbase) {
			startHBase();
		}
		
		startYarn();
		
		if(supportSpark) {
			startSpark();			
		}
	}

	/**
	 * 创建集群
	 */
	public void distrib() {
		exitCode = RemoteShell.OK;
		try {
			login();
			prepareWork();
			stopAll();
			cfgHostName();
			cfgDNS();
			shareRsa();
			getHostEnv();
			install();
			closeFireWall();
			initialize();
			
//			startAll();
//			restore();
			
			logout();
			exit(RemoteShell.OK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printUsage() {
		System.out.println("Usage1: java -jar QuickHadoop.jar install [-autoHA]");
		System.out.println("Usage2: java -jar QuickHadoop.jar start");
		System.out.println("Usage3: java -jar QuickHadoop.jar stop");
		System.out.println("Usage4: java -jar QuickHadoop.jar uninstall");
	}
	
	/**
	 * cmd
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		if(args.length < 1) {
			printUsage();
			return;
		}
		
		Cluster cluster = Cluster.getInstance();
		boolean supportHA = false;
		
		if(args[0].equalsIgnoreCase("install")) {
			if(args.length > 1){
				if(!args[1].equalsIgnoreCase("-autoHA")) {
					printUsage();
					return;
				}
				supportHA = true;
			}
			
			cluster.checkInstall(supportHA);
		} else if (args[0].equalsIgnoreCase("uninstall")) {
			cluster.uninstall();
		} else if (args[0].equalsIgnoreCase("start")) {
			try {
				if(cluster.getInfo()) {
					cluster.login();
					cluster.startAll();
					cluster.logout();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (args[0].equalsIgnoreCase("stop")) {
			try {
				if(cluster.getInfo()) {
					cluster.login();
					cluster.stopAll();
					cluster.logout();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			printUsage();
		}
	}
	
	/**
	 * 检查并执行安装命令
	 * 
	 * @param supportHA
	 */
	public void checkInstall(boolean supportHA) {
		boolean installJre = false;
		boolean installHadoop = false;
		boolean installZK = false;
		boolean installHBase = false;
		boolean installScala = false;
		boolean installSpark = false;
		
		String cfgDir = System.getProperty("user.dir") + "/config/";
		File dir = new File(cfgDir);
		for(String fn : dir.list()) {
			if((fn.toLowerCase().startsWith("jre") 
					|| fn.toLowerCase().contains("jdk")) &&
					fn.toLowerCase().endsWith("gz")) {
				int ret = Jre.getInstance().getFromFile(cfgDir + fn);
				if(ret == RetNo.OK) {
					installJre = true;
				}
			}
			if(fn.toLowerCase().startsWith("hadoop") &&
					fn.toLowerCase().endsWith("gz")) {
				Hadoop hp = Hadoop.getFromFile(cfgDir + fn);
				if(hp != null) {
					setHadoop(hp);
					installHadoop = true;
				}
			}
			if(fn.toLowerCase().startsWith("zookeeper") &&
					fn.toLowerCase().endsWith("gz")) {
				Zookeeper.getInstance().getFromFile(cfgDir + fn);
				installZK = true;
			}
			if(fn.toLowerCase().startsWith("hbase") && 
					fn.toLowerCase().endsWith("gz")) {
				HBase.getInstance().getFromFile(cfgDir + fn);
				installHBase = true;
			}
			if(fn.toLowerCase().startsWith("scala") &&
					fn.toLowerCase().endsWith("gz")) {
				Scala.getInstance().getFromFile(cfgDir + fn);
				installScala = true;
			}
			if(fn.toLowerCase().startsWith("spark") &&
					fn.toLowerCase().endsWith("gz")) {
				Scala.getInstance().getFromFile(cfgDir + fn);
				installSpark = true;
			}
		}
		
		if(!installJre) {
			System.err.println("Could not find the JRE or JDK install file, it should be a .tar.gz file which in the dir ./config/");
			return;
		}
		
		if(!installHadoop) {
			System.err.println("Could not find the Hadoop install file, it should be a .tar.gz file which in the dir ./config/");
			return;
		}
		
		if(hadoop instanceof HadoopV1 && supportHA) {
			System.err.println("The Auto HA function requires Hadoop2.0+");
			return;
		}
		
		if(installHBase && !installZK) {
			System.err.println("The HBase requires Zookeeper, Could not find the Zookeeper install file in the dir ./config/");
			return;
		}
		
		if(installSpark && !installScala) {
			System.err.println("The Spark requires Scala, Could not find the Scala install file in the dir ./config/");
			return;
		}
		
		System.out.println("### Start Install the Hadoop Cluster");
		List<Host> hosts = getHostsFromFile(cfgDir + "hosts");
		boolean hasNameNode = false;
		boolean hasDataNode = false;
		for(Host h : hosts) {
			if(h.getHostName().contains(NN_NAME)) {
				hasNameNode = true;
			}
			if(h.getHostName().contains(DN_NAME)) {
				hasDataNode = true;
			}
		}
		if(!hasNameNode || !hasDataNode) {
			System.err.println("At least one NameNode and one DataNode are required to setup a cluster!");
			return;
		}
		setHostList(hosts);
		
		if(supportHA && (getNameNode()== null || getSecNameNode() == null)) {
			System.err.println("At least 2 NameNodes are required for Auto HA function!");
			return;
		}
		
		int cnt = hosts.size() % 2 ==0 ? hosts.size() : hosts.size() - 1;
		for(int i = 0; i < cnt; i++) {
			hosts.get(i).isZookeeperProperty().setValue(installZK);
			hosts.get(i).isHBaseProperty().setValue(installHBase);
			hosts.get(i).isJournalNodeProperty().setValue(installZK);
		}
		
		setSupportZookeeper(installZK);
		setHaAutoRecover(supportHA);
		setSupportHbase(installHBase);
		setSupportSpark(installSpark);
		distrib();
		
		System.out.println("### Install successfully! You need logout the old SSH, before login to the cluster hosts.");
	}
	
	/**
	 * get the info of cluster.
	 */
	public boolean getInfo() {
		System.out.println("### Getting the info of cluster.");
		String cfgDir = System.getProperty("user.dir") + "/config/";
		List<Host> hosts = getHostsFromFile(cfgDir + "hosts");
		setHostList(hosts);
		
		if(hosts.size() < 1) {
			System.err.println("There is no host exist according the file ./config/hosts");
			return false;
		}
		
		boolean hadoopInstalled = false;
		boolean zkInstalled = false;
		boolean hBaseInstalled = false;
		
		try {
			for(Host h : hosts) {
				if(!Util.ping(h.getIp(), 2000)) {
					System.err.println("The host " + h.getIp() + " is unreachable!");
					return false;
				}
				
				h.login();
				RemoteShell sh = h.getShell();
				
				boolean dirExist = sh.fileExists(Path.HADOOP_DISTR);
				hadoopInstalled |= dirExist;
				
				boolean zkExist = false;
				boolean hbExist = false;
				
				if(dirExist) {
					if(getHadoop() == null) {
						String ver = h.getHadoopVer();
						Hadoop hadoop = Hadoop.get(ver);
						
						if(hadoop != null) {
							String hadoopBin = sh.getCmdOutPut("which hadoop");
							if(hadoopBin != null && !hadoopBin.isEmpty()) {
								int index = hadoopBin.indexOf("/bin/hadoop");
								hadoop.setHome(hadoopBin.substring(0, index));
								hadoop.setCfgPath();
							}
							setHadoop(hadoop);							
						}
					}
					
					String zHome = sh.getCmdRet("ls " + Path.HADOOP_DISTR + " | grep zookeeper");
					if(zHome != null && zHome.toLowerCase().startsWith("zookeeper")) {
						Zookeeper.getInstance().setHome(Path.HADOOP_DISTR + "/" + zHome);
						Zookeeper.getInstance().setCfgPath();
						zkExist = true;
					}
					
					String hBaseHome = sh.getCmdRet("ls " + Path.HADOOP_DISTR + " | grep hbase");
					if(hBaseHome != null && hBaseHome.toLowerCase().startsWith("hbase")) {
						HBase.getInstance().setHome(Path.HADOOP_DISTR + "/" + hBaseHome);
						HBase.getInstance().setCfgPath();
						hbExist = true;
					}			
				}
				
				h.logout();
				
				h.isZookeeperProperty().setValue(zkExist);
				h.isHBaseProperty().setValue(hbExist);
				
				zkInstalled |= zkExist;
				hBaseInstalled |= hbExist;
			}
			
			if(!hadoopInstalled) {
				System.err.println("Hadoop is not installed on the hosts according the file ./config/hosts");
				return false;
			}
			
			setSupportZookeeper(zkInstalled);
			setSupportHbase(hBaseInstalled);
			
			prepareWork();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * 卸载集群
	 */
	public void uninstall() {
		System.out.println("### Start Uninstall the Hadoop Cluster");
		
		if(!getInfo()) {
			return;
		}
		
		try {
			login();
			restore();
			logout();			
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		System.out.println("### Uninstall successfully!");
	}
	
	/**
	 * 从文件中获取主机列表
	 * 
	 * @param fileName
	 * @return
	 */
	public List<Host> getHostsFromFile(String fileName) {
		List<Host> hosts = new ArrayList<Host>();
		File file = new File(fileName);
		if(!file.exists()) {
			System.err.println("./config/hosts not exist!");
			return hosts;
		}
		
		BufferedReader buf = null;
		try {
			buf = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = buf.readLine()) != null) {
				if(line.startsWith("#")) {
					continue;
				}
				String[] h = line.split("\\s+");
				if(h.length < 4) {
					continue;
				}
				hosts.add(new Host(h[3].trim(), h[0].trim(), h[1].trim(), h[2].trim()));
			}
		} catch (Exception e) {
			System.err.println("Read ./config/hosts err!");
			e.printStackTrace();
		} finally {
			try {
				if(buf != null) {
					buf.close();					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return hosts;
	}
	
	/**
	 * 退出程序
	 * 
	 * @param code
	 */
	public void exit(int code) {
		exitCode = code;
	}

	/**
	 * 断开连接
	 */
	public void logout() {
		List<Runnable> runList = new ArrayList<Runnable>();
		for(final Host host : hostList) {
			runList.add(new Runnable() {
				@Override
				public void run() {
					try {
						System.out.println("### " + host.getIp() + " close connect.");
						host.logout();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		ThreadPool.Execute(runList, true);
	}
	
	/**
	 * @return the hadoop
	 */
	public Hadoop getHadoop() {
		return hadoop;
	}

	/**
	 * @param hadoop the hadoop to set
	 */
	public void setHadoop(Hadoop hadoop) {
		this.hadoop = hadoop;
	}
	
	/**
	 * @return the zookeeper
	 */
	public Zookeeper getZookeeper() {
		return zookeeper;
	}

	/**
	 * @param zookeeper the zookeeper to set
	 */
	public void setZookeeper(Zookeeper zookeeper) {
		this.zookeeper = zookeeper;
	}

	/**
	 * @return the hostList
	 */
	public List<Host> getHostList() {
		return hostList;
	}
	
	/**
	 * @param hostList the hostList to set
	 */
	public void setHostList(List<Host> hostList) {
		this.hostList = hostList;
	}

	/**
	 * @return the journalList
	 */
	public List<Host> getJournalList() {
		return journalList;
	}

	/**
	 * @return the zooKeeperList
	 */
	public List<Host> getZooKeeperList() {
		return zooKeeperList;
	}

	/**
	 * @param zooKeeperList the zooKeeperList to set
	 */
	public void setZooKeeperList(List<Host> zooKeeperList) {
		this.zooKeeperList = zooKeeperList;
	}
	
	public List<Host> gethBaseList() {
		return hBaseList;
	}

	public void sethBaseList(List<Host> hBaseList) {
		this.hBaseList = hBaseList;
	}
	
	public List<Host> gethRegionList() {
		return hRegionList;
	}

	public void sethRegionList(List<Host> hRegionList) {
		this.hRegionList = hRegionList;
	}
	
	public List<Host> gethMasterList() {
		return hMasterList;
	}

	public void sethMasterList(List<Host> hMasterList) {
		this.hMasterList = hMasterList;
	}

	/**
	 * @return the hBase
	 */
	public HBase gethBase() {
		return hBase;
	}

	/**
	 * @param hBase the hBase to set
	 */
	public void sethBase(HBase hBase) {
		this.hBase = hBase;
	}	
	
	/**
	 * @return the supportSpark
	 */
	public boolean isSupportSpark() {
		return supportSpark;
	}

	/**
	 * @param supportSpark the supportSpark to set
	 */
	public void setSupportSpark(boolean supportSpark) {
		this.supportSpark = supportSpark;
	}

	/**
	 * @return the supportHbase
	 */
	public boolean isSupportHbase() {
		return supportHbase;
	}

	/**
	 * @param supportHbase the supportHbase to set
	 */
	public void setSupportHbase(boolean supportHbase) {
		this.supportHbase = supportHbase;
	}

	/**
	 * @return the supportZookeeper
	 */
	public boolean isSupportZookeeper() {
		return supportZookeeper;
	}

	/**
	 * @param supportZookeeper the supportZookeeper to set
	 */
	public void setSupportZookeeper(boolean supportZookeeper) {
		this.supportZookeeper = supportZookeeper;
	}

	/**
	 * @return the haAutoRecover
	 */
	public boolean isHaAutoRecover() {
		return haAutoRecover;
	}

	/**
	 * @param autoHa the supportHA to set
	 */
	public void setHaAutoRecover(boolean autoHa) {
		this.haAutoRecover = autoHa;
	}

	public boolean isSetUped() {
		return setUped;
	}

	public void setSetUped(boolean setUped) {
		this.setUped = setUped;
	}
}
