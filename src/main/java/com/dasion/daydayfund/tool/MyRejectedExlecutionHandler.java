package com.dasion.daydayfund.tool;

import com.dasion.daydayfund.constant.RedisConstant;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import com.dasion.daydayfund.exc.FundCrawlersMain;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class MyRejectedExlecutionHandler implements RejectedExecutionHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyRejectedExlecutionHandler.class);
    @Autowired
    private JedisTool jedisTool;

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
            Gson gson = new Gson();
            FundCrawlersMain fundCrawlersMain = (FundCrawlersMain)r;
            jedis.lpush(fundCrawlersMain.getBaseBean().getFromQueue()
                    , gson.toJson(fundCrawlersMain.getBaseBean()));
            logger.info("拒绝策略触发,当前线程数:" + executor.getActiveCount()+ " 当前排队数量:"+executor.getQueue().size());
            //通过队列当前排队数量设置等待时间
            jedis.set(RedisConstant.SEMAPHORE_KEY_NEED_WAIT,executor.getQueue().size()+"");
        }
    }
}
