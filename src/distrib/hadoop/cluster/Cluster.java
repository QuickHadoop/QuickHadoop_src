package distrib.hadoop.cluster;
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

public class Cluster {

	/** 分布式主机节点 */
	private List<Host> hostList = new ArrayList<>();

	/** Journal主机节点 */
	private List<Host> journalList = new ArrayList<>();
	
	/** Zookeeper主机节点 */
	private List<Host> zooKeeperList = new ArrayList<>();
	
	/** HBase主机节点 */
	private List<Host> hBaseList = new ArrayList<>();
	
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
			
			shareRsaToNameNode(getNameNode());
			shareRsaToNameNode(getSecNameNode());
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
			throw new InstallException(Messages.getString("Exception.rsa.failed"));
		}
	}

	/**
	 * 与 NameNode之间共享秘钥
	 * 
	 * @param nameNode
	 * @throws AuthException
	 * @throws IOException
	 */
	private void shareRsaToNameNode(Host nameNode) throws AuthException,
			IOException {
		if(nameNode == null) {
			return;
		}
		
		for(Host h : hostList) {
			if(h.equals(nameNode)) {
				continue;
			}
			shareRsa(nameNode, h);
			shareRsa(h, nameNode);
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
		List<Host> nameNodeList = new ArrayList<>();
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
		List<Host> dataNodeList = new ArrayList<>();
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
		
		cShell.getFile(Path.RSA_PUB, Path.TMP_LOCAL);
		sShell.putFile(Path.RSA_LOCAL, Path.RSA_SCP_NAME, Path.TMP);
		sShell.excute("cat " + Path.RSA_SCP_FULL_PATH + " >> " + Path.SSH_AUTH);
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
			for(Host h : hostList) {
				if(h.isHBaseProperty().getValue()) {
					hBaseList.add(h);					
				}
			}
		}
		
		journalList.clear();
		for(Host h : hostList) {
			if(h.isJournalNodeProperty().getValue()) {
				journalList.add(h);
			}
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
		startHBase();
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
		if(hBase == null || hBaseList.size() < 1) {
			return;
		}
		try {
			Host host = hBaseList.get(0);
			if(host == null) {
				return;
			}
			host.getShell().excutePty(hBase.start());
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
		}
	}
	
	/**
	 * 停止HBase
	 * 
	 * @throws InstallException
	 */
	public void stopHBase() {
		if(hBase == null || hBaseList.size() < 1) {
			return;
		}
		try {
			Host host = hBaseList.get(0);
			if(host == null) {
				return;
			}
			host.getShell().excutePty(hBase.stop());
		} catch (Exception e) {
			exit(RemoteShell.FAILED);
		}
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
	
	/**
	 * 测试代码
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		Cluster cluster = Cluster.getInstance();
		Host host1 = new Host("NameNode1", "172.16.168.134", "root", "passwd");
		Host host2 = new Host("NameNode2", "172.16.168.144", "root", "passwd");
		Host host3 = new Host("DataNode1", "172.16.177.183", "root", "passwd");
		Host host4 = new Host("DataNode2", "172.16.186.84", "root", "passwd");
		
		host1.isZookeeperProperty().setValue(true);
		host2.isZookeeperProperty().setValue(true);
		host3.isZookeeperProperty().setValue(true);
		host4.isZookeeperProperty().setValue(true);
//		
		host1.isHBaseProperty().setValue(true);
		host2.isHBaseProperty().setValue(true);
		host3.isHBaseProperty().setValue(true);
		host4.isHBaseProperty().setValue(true);

		
		host1.isJournalNodeProperty().setValue(true);
		host2.isJournalNodeProperty().setValue(true);
		host3.isJournalNodeProperty().setValue(true);
		host4.isJournalNodeProperty().setValue(true);


		cluster.getHostList().add(host1);
		cluster.getHostList().add(host2);
		cluster.getHostList().add(host3);
		cluster.getHostList().add(host4);

//		cluster.getHostList().add(new Host("DataNode3", "172.16.186.178", "root", "passwd"));
//		cluster.getHostList().add(new Host("DataNode4", "172.16.177.150", "root", "passwd"));
//		cluster.getHostList().add(new Host("DataNode5", "172.16.177.151", "root", "passwd"));
		cluster.setHadoop(new HadoopV2());
		cluster.setSupportZookeeper(true);
		cluster.setHaAutoRecover(true);
		cluster.setSupportHbase(true);
		cluster.distrib();
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
}
