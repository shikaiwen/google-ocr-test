package com.kowa.batch.quartz;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class QuzrtzConfig {

    public static final String GROUP_NAME = "default";


    @Autowired
    ApplicationContext applicationContext;


    public static List<CronTrigger> detaulTrigger = new ArrayList<>();

    @Bean(name = "scheduler")
    public Scheduler scheduler() throws Exception {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        factoryBean.setSchedulerFactoryClass(StdSchedulerFactory.class);
        Map<String,CronTrigger> triggerMap = applicationContext.getBeansOfType(CronTrigger.class);
        List<Trigger> triggers = new ArrayList<>();
        for (String s : triggerMap.keySet()) {
            triggers.add(triggerMap.get(s));
        }

        CronTrigger [] triggerObj = triggers.toArray(new CronTrigger[]{});
        factoryBean.setTriggers(triggerObj);

        factoryBean.afterPropertiesSet();
        Scheduler scheduler = factoryBean.getScheduler();
        scheduler.start();

        return scheduler;
    }


//    public static final String MINITE_ONE = "* */1 * * * ?";
//    public static final String MINITE_TWO = "* */2 * * * ?";
//    public static final String MINITE_THREE = "* */3 * * * ?";
    public static final String MINITE_FIVE = "*/20 * * * * ?";

    @Bean("trigger-kowa011")
    public CronTrigger trigger011() throws ParseException {
        CronTriggerFactoryBean factoryBean = getTriggerPrototype("trigger-kowa011","trigger desc",MINITE_FIVE);

        String jobDetailDesc = "商品マスター取込(在庫)";
        JobDetail jobDetail = getJobDetailPrototype("jobdetail-kowa011", jobDetailDesc, KOWA011Job.class);

        factoryBean.setJobDetail(jobDetail);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean("trigger-kowa012")
    public CronTrigger trigger012() throws ParseException {
        CronTriggerFactoryBean factoryBean = getTriggerPrototype("trigger-kowa012","zaiko desc",MINITE_FIVE);
        String jobDetailDesc = "通報メールキューT";


        JobDetail jobDetail = getJobDetailPrototype("jobdetail-kowa012", jobDetailDesc, KOWA012Job.class);
        factoryBean.setJobDetail(jobDetail);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean("trigger-kowa033")
    public CronTrigger trigger033() throws ParseException {
        CronTriggerFactoryBean factoryBean = getTriggerPrototype("trigger-kowa033","zaiko desc",MINITE_FIVE);
        String jobDetailDesc = "注文構成品情報取得";

        JobDetail jobDetail = getJobDetailPrototype("jobdetail-kowa033", jobDetailDesc, KOWA033Job.class);
        factoryBean.setJobDetail(jobDetail);
        factoryBean.afterPropertiesSet();

        return factoryBean.getObject();
    }


    @Bean("trigger-kowa034")
    public CronTrigger trigger034() throws ParseException {
        CronTriggerFactoryBean factoryBean = getTriggerPrototype("trigger-kowa034","zaiko desc",MINITE_FIVE);
        String jobDetailDesc = "在庫情報取得WebAPI";

        JobDetail jobDetail = getJobDetailPrototype("jobdetail-kowa034", jobDetailDesc, KOWA012Job.class);
        factoryBean.setJobDetail(jobDetail);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean("trigger-kowa041")
    public CronTrigger trigger041() throws ParseException {
        CronTriggerFactoryBean factoryBean = getTriggerPrototype("trigger-kowa041","zaiko desc",MINITE_FIVE);
        String jobDetailDesc = "Amazon メール　parse";

        JobDetail jobDetail = getJobDetailPrototype("jobdetail-kowa041", jobDetailDesc, KOWA041Job.class);
        factoryBean.setJobDetail(jobDetail);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }



    @Bean("trigger-kowa042")
    public CronTrigger trigger042() throws ParseException {
        CronTriggerFactoryBean factoryBean = getTriggerPrototype("trigger-kowa042","zaiko desc",MINITE_FIVE);
        String jobDetailDesc = "PriceSearch ファイル解析";

        JobDetail jobDetail = getJobDetailPrototype("jobdetail-kowa042", jobDetailDesc, KOWA042Job.class);
        factoryBean.setJobDetail(jobDetail);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean("trigger-kowa043")
    public CronTrigger trigger043() throws ParseException {
        CronTriggerFactoryBean factoryBean = getTriggerPrototype("trigger-kowa043","zaiko desc",MINITE_FIVE);
        String jobDetailDesc = "zkとFBA 在庫 ";

        JobDetail jobDetail = getJobDetailPrototype("jobdetail-kowa043", jobDetailDesc, KOWA043Job.class);
        factoryBean.setJobDetail(jobDetail);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }


    public static CronTriggerFactoryBean getTriggerPrototype(String name ,String desc,String cronExpression) throws ParseException {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setName(name);
        factoryBean.setGroup(GROUP_NAME);
        factoryBean.setCronExpression(cronExpression);
        factoryBean.setDescription(desc);
        return factoryBean;
    }


    public static JobDetail getJobDetailPrototype(String name ,String desc,Class jobClass ){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setGroup(GROUP_NAME);
        factoryBean.setName(name);
        factoryBean.setJobClass(jobClass);
        factoryBean.setDescription(desc);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }


}
