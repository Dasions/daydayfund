package com.dasion.daydayfund.quarzt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.dasion.daydayfund.constant.ConfigConstant;
import com.dasion.daydayfund.tool.IPTool;
import org.apache.http.impl.client.BasicCookieStore;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dasion.daydayfund.constant.HtmlConstant;
import com.dasion.daydayfund.constant.RedisConstant;
import com.dasion.daydayfund.enums.DaydayFundExcStepEnum;
import com.dasion.daydayfund.fund.BaseBean;
import com.dasion.daydayfund.tool.HttpclientTool;
import com.dasion.daydayfund.tool.JedisTool;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import com.dasion.daydayfund.exc.FundCrawlersMain;

@Component
public class StartFundCrawlersMain {
    private static final Logger logger = LoggerFactory.getLogger(StartFundCrawlersMain.class);
    @Autowired
    private JedisTool jedisTool;
    private String FUND_COMPANY_URL = "http://fund.eastmoney.com/Company/home/KFSFundRank?gsid=";
    @Autowired
    private ConfigConstant configConstant;

    @Autowired
    private RejectedExecutionHandler myRejectedExlecutionHandler;

    private static ThreadPoolExecutor pool;

    public void execute() {
        Gson gson = new Gson();
        try {
            String nowDate = DateTime.now().toString(DateTimeFormat.forPattern("yyyy-MM-dd"));
            //设置SEMAPHORE_KEY为run， 直接往线程池添加FundCrawlersMain对象执行爬取任务即可
            Long lockFlag = null;
            //利用redis 的setnx的特性，防止多实例同时启动，重复拉取基金列表
            try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
                lockFlag = jedis.setnx(RedisConstant.LOCK_KEY + nowDate, "lock");
            }

            String sourceQueueName = RedisConstant.SOURCE_DATA_QUEUE + nowDate;
            //获取到锁，则需要获取基金列表,若所有实例都是非程序正常停止，需要手动将锁删除
            if (lockFlag == 1) {
                List<String> fndCompanys = getFundCompanysByHttp();
                String finalQueueName = RedisConstant.FINAL_DATA_QUEUE + nowDate;
                delOldData(sourceQueueName);
                delOldData(finalQueueName);
                initFundListAndSetSemaphoreKeyToRun(fndCompanys, sourceQueueName);
            }
            String ip = IPTool.getLocalIP();
            try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
                //设置当前实例最大并发数
                jedis.set(ip + "_" + RedisConstant.MAX_THREAD_NUM, configConstant.getMaxThreadNum() + "");
            }

            pool = new ThreadPoolExecutor(
                    1, configConstant.getMaxThreadNum(), 10000L, TimeUnit.MILLISECONDS
                    , new LinkedBlockingQueue<Runnable>(1), myRejectedExlecutionHandler);

            logger.info("--------------------启动--------------------------------");
            HttpclientTool httpclientTool = getHttpClient();
            while (true) {
                try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
                    if (RedisConstant.SEMAPHORE_KEY_STOP.equals(jedis.get(RedisConstant.SEMAPHORE_KEY))) {
                        logger.warn("统一开关状态是" + jedis.get(RedisConstant.SEMAPHORE_KEY) + " 停止执行!");
                        return;
                    }

                    if (!jedis.exists(sourceQueueName) || jedis.llen(sourceQueueName).intValue() == 0) {
                        logger.info("sourceQueue队列无数据, 停止执行!");
                        Thread.sleep(10000);
                        continue;
                    }

                    BaseBean baseBean = gson.fromJson(jedis.rpop(sourceQueueName), BaseBean.class);
                    if (baseBean == null) {
                        logger.error("---------------sourceQueue队列数据 is empty,  停止执行!---------------------");
                        continue;
                    }
                    String time = jedis.get(RedisConstant.SEMAPHORE_KEY_NEED_WAIT);
                    if (time != null) {
                        logger.info("线程池满载，进入休眠");
                        Thread.sleep(Long.valueOf(time) * 1000);
                        jedis.del(RedisConstant.SEMAPHORE_KEY_NEED_WAIT);
                        logger.info("线程池退出休眠");
                    }

                    pool.execute(new FundCrawlersMain(baseBean, httpclientTool));
                }
            }

        } catch (Exception e) {
            logger.error("统一入口发生异常： ", e);
        }
    }

    private HttpclientTool getHttpClient() {
        HttpclientTool tool = new HttpclientTool();
        tool.createCustomConfigClientByHttpClients(null,new BasicCookieStore());
        tool.setDefaultHeader();
        return tool;
    }

    /**
     * 获取需要爬取数据的基金列表
     * @param fndCompanys
     * @param sourceQueueName
     */
    private void initFundListAndSetSemaphoreKeyToRun(List<String> fndCompanys, String sourceQueueName) {
        try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
            Gson gson = new Gson();
            //开启统一开关
            logger.info("------开启统一开关------状态: " + RedisConstant.SEMAPHORE_KEY_RUN);
            jedis.set(RedisConstant.SEMAPHORE_KEY, RedisConstant.SEMAPHORE_KEY_RUN);
            jedis.set(RedisConstant.COUNT_BASE_INFO, "0");
            jedis.set(RedisConstant.COUNT_DETAIL_INFO, "0");
            jedis.set(RedisConstant.COUNT_INC_INFO, "0");

            for (String fundCompany : fndCompanys) {
                BaseBean baseBean = new BaseBean("daydayfund", DaydayFundExcStepEnum.BASE_INFO.getCode()
                        , FUND_COMPANY_URL + fundCompany
                        , sourceQueueName, sourceQueueName);
                jedis.lpush(sourceQueueName, gson.toJson(baseBean));
            }
        }

    }

    /**
     * 删除当天上一次启动时爬取的数据
     * @param queueName
     */
    private void delOldData(String queueName) {
        try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
            jedis.del(queueName);
            jedis.del( RedisConstant.COUNT_BASE_INFO);
            jedis.del( RedisConstant.COUNT_DETAIL_INFO );
            jedis.del( RedisConstant.COUNT_INC_INFO );
        }
    }

    /**
     * 发起http请求获取基金列表数据
     * @return
     * @throws IOException
     */
    private static List<String> getFundCompanysByHttp() throws IOException {
        HttpclientTool tool = new HttpclientTool();
        tool.createDefaultConfigClientByHttpClients();
        tool.setDefaultCookieStore();
        tool.setDefaultHeader();
        tool.setUrl("http://fund.eastmoney.com/company/default.html");
        String fundListHtml = tool.getContentByGet();
        tool.getClient().close();
        Document fundListDoc = Jsoup.parse(fundListHtml);

        Elements trs = fundListDoc.getElementById("gspmTbl").getElementsByTag(HtmlConstant.TBODY).get(0)
                .getElementsByTag(HtmlConstant.TR);
        List<String> cps = new ArrayList<>();
        for (Element tr : trs) {
            String str = tr.getElementsByTag(HtmlConstant.TD).get(1).getElementsByTag(HtmlConstant.A).get(0)
                    .attr(HtmlConstant.HREF);
            cps.add(str.substring(str.lastIndexOf("/") + 1, str.indexOf(".")));
        }
        return cps;
    }

    public ThreadPoolExecutor getPool() {
        return pool;
    }
}
