package com.halloffame.thriftjsoa;

import com.halloffame.thriftjsoa.sample.iface.UserService;
import com.halloffame.thriftjsoa.sample.iface.User;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.LongAdder;

/**
 * QPS测试
 * @author zhuwx
 */
public class QpsClientTest {

    private static LongAdder successCount = new LongAdder(); //成功次数
    private static LongAdder failCount = new LongAdder(); //失败次数
    private static LongAdder responseTimeSum = new LongAdder(); //总响应时间，单位：ms

    private static int threadNum = 12; //线程数
    private static int runTime = 60; //线程运行时间，单位：s

    //cyclicBarrier目的是让所有线程同时运行，模拟并发请求
    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(threadNum + 1);

    private static volatile boolean isFinish = false; //结束线程运行标志变量

    //countDownLatch目的是所有线程都结束运行后，主线程才计算相关指标信息
    private static CountDownLatch countDownLatch = new CountDownLatch(threadNum);

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < threadNum; i++) {
            ReqThread reqThread = new ReqThread(i);
            reqThread.start();
        }

        cyclicBarrier.await();
        Thread.sleep(runTime * 1000);

        isFinish = true;
        countDownLatch.await();

        long successLongCount = successCount.longValue();
        long failLongCount = failCount.longValue();
        long responseTimeLongSum = responseTimeSum.longValue();
        
        long totalCount = successLongCount + failLongCount;
        double avgRepTime = responseTimeLongSum / totalCount;
        double qps = successLongCount / runTime;
        double concurrency = qps * (avgRepTime / 1000);
        
        System.out.println("成功调用次数：" + successLongCount);
        System.out.println("失败调用次数：" + failLongCount);
        System.out.println("平均响应时间：" + avgRepTime + "毫秒");
        System.out.println("QPS：" + qps);
        System.out.println("并发：" + concurrency);
    }

    static class ReqThread extends Thread {
        private int i;
        public ReqThread(int i){
            this.i = i;
        }

        @Override
        public void run() {
            try {
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            
            while ( !isFinish ) {
            	long startTime = System.currentTimeMillis();
            	
            	User user;
            	TTransport transport = null;
                try {
                	TSocket socket = new TSocket("localhost", 4567);
                    transport = new TFastFramedTransport(socket);
                    transport.open();
                    TProtocol tProtocol = new TCompactProtocol(transport);
                    UserService.Client userClient = new UserService.Client(tProtocol);
                    
					user = userClient.getUser(2);
				} catch (Exception e) {
					user = null;
					e.printStackTrace();
				} finally {
					if (transport != null && transport.isOpen()) {
						transport.close();
					}
				}
                
                long endTime = System.currentTimeMillis();  
                System.out.println("线程" + i + "调用结果：" + user);
                responseTimeSum.add(endTime - startTime);
                
                if (user != null) {
                	successCount.increment();
                } else {
                	failCount.increment();
                } 
            }

            countDownLatch.countDown();
        }
    }

}
