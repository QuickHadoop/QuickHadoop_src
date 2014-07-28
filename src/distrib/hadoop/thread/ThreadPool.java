package distrib.hadoop.thread;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 线程工厂
 * 
 * @author guolin
 */
public class ThreadPool {
	
	/** 私有实例 */
	private static ThreadPool threadPool;
	
	/** 并发线程最大个数 */
	private static final int MAX_THREAD_CNT = 100;
	
	/** 等待任务结束最长等待时间，单位:分钟 */
	private static final int MAX_WAIT_TIME = 50;
	
	public class Factory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	}
	
	/**
	 * 并发执行一组任务
	 * 
	 * @param runs	任务列表
	 * @param await	是否等待任务都结束才返回
	 */
	public static synchronized void Execute(List<Runnable> runs, boolean await) {
		if(runs == null || runs.size() == 0) {
			return;
		}
		
		if(threadPool == null) {
			threadPool = new ThreadPool();
		}
		
		try {
			Factory factory = threadPool.new Factory();
			ExecutorService exec = Executors.newFixedThreadPool(
					MAX_THREAD_CNT, factory);
			
			for(Runnable r : runs) {
				exec.execute(r);
			}
			
			exec.shutdown();
			
			if(await) {
				exec.awaitTermination(MAX_WAIT_TIME, TimeUnit.MINUTES);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
