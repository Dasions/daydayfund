package com.dasion.daydayfund.exc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.crawler.DaydayFundCrawler;
import com.dasion.daydayfund.fund.BaseBean;
import com.dasion.daydayfund.tool.BeanContext;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

import java.util.concurrent.LinkedBlockingQueue;

public class FundCrawlersMain implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(FundCrawlersMain.class);
    private BaseBean baseBean;
    private HttpclientTool httpclientTool;
    public FundCrawlersMain(BaseBean baseBean,HttpclientTool httpclientTool) {
        this.baseBean = baseBean;
        this.httpclientTool = httpclientTool;
    }

    public BaseBean getBaseBean() {
        return baseBean;
    }


    @Override
    public void run() {
        JedisTool jedisTool = BeanContext.getBean(JedisTool.class);
        Gson gson = new Gson();
        try {
            DaydayFundCrawler daydayFundCrawler = BeanContext.getBean(DaydayFundCrawler.class);
            switch (baseBean.getCrawlerType()) {
                case "daydayfund":
                    daydayFundCrawler.excRoute(baseBean, httpclientTool);
                    break;
                default:
                    logger.error("未定义爬虫类型： " + baseBean.getCrawlerType());
                    break;
            }
        } catch (Exception e) {
            logger.error("爬虫统一执行接口发生异常：", e);
            try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
                String resultQueue = RedisConstant.ERROR_DATA_QUEUE + baseBean.getFromQueue().split("_")[1];
                jedis.lpush(resultQueue, gson.toJson(baseBean));
            }
        }

    }

}
