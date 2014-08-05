package distrib.hadoop.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import distrib.hadoop.util.RetNo;
import distrib.hadoop.util.Path;
import distrib.hadoop.util.Util;

public class Jre {
	/** 版本信息 */
	protected String ver;
	/** 安装文件 */
	protected String installFile;
	/** 主机存放安装文件的临时目录 */
	protected String tmpPath;
	/** HOME目录 */
	protected String jreHome;
	/** 安装文件的本地目录 */
	protected String localPath;

	
	public static Jre instance;
	
	/**
	 * 构造方法
	 */
	private Jre() {
		ver = "jre1.6.0_24";
		installFile = ver + ".tar.gz";
		tmpPath = Path.TMP + "/" + installFile;
		jreHome = Path.HADOOP_DISTR + "/" + ver;
		localPath = Util.getFullPath(installFile);
	}
	
	/**
	 * 获取单例对象
	 * 
	 * @return
	 */
	public static Jre getInstance() {
		if(instance == null) {
			instance = new Jre();
		}
		
		return instance;
	}
	
	/**
	 * 根据指定安装文件得到JRE版本信息
	 * 
	 * @param file
	 * @return
	 */
	public int getFromFile(String file) {
		File input = new File(file);
		if(!input.exists()) {
			System.out.println("The JRE or JDK install file is not exist!");
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
				System.out.println("Can not read the JRE or JDK install file!");
				return RetNo.FILE_IO_ERROR;
			}
			
			String dir = entry.getName().replaceAll("/", "");
			
			ver = dir;
			localPath = file;
			installFile = file.substring(file.lastIndexOf("/") + 1);
			tmpPath = Path.TMP + "/" + installFile;
			jreHome = Path.HADOOP_DISTR + "/" + ver;
		} catch (Exception e) {
			System.out.println("Can not read the JRE or JDK install file!");
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
		return jreHome;
	}
	/**
	 * @param home the home to set
	 */
	public void setHome(String home) {
		this.jreHome = home;
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
}
