package com.kowa.batch.web;

import com.kowa.batch.quartz.QuzrtzConfig;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class QuartzController {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    Scheduler scheduler;

    @GetMapping("/quartz/joblist")
    @ResponseBody
    public Object getJobList() throws Exception{
        Map<String,CronTrigger> triggerMap = applicationContext.getBeansOfType(CronTrigger.class);
        List<Trigger> triggers = new ArrayList<>();
        /**
         * trigger name
         * job name
         * job detail desc
         * job class
         */
        List<Map> jobinfoList = new ArrayList<>();
        for (String s : triggerMap.keySet()) {

            Map<String, String> jobInfo = new LinkedHashMap<>();
            CronTrigger trigger = triggerMap.get(s);

            JobDetail jobDetail = scheduler.getJobDetail(trigger.getJobKey());

            Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());

            jobInfo.put("triggerKey", trigger.getKey().getName());
            jobInfo.put("jobKey", trigger.getJobKey().getName());
            jobInfo.put("jobdetail_desc", jobDetail.getDescription());
            jobInfo.put("job_class", jobDetail.getJobClass().getCanonicalName());
            jobInfo.put("triggerStatus", String.valueOf(triggerState));


//            jobInfo.put("job class", jobDetail.getJobClass().getCanonicalName());
            jobinfoList.add(jobInfo);
        }

        return jobinfoList;
    }


    @GetMapping("/quartz/change/{key}/{mi}")
    @ResponseBody
    public Object getJobList(@PathVariable("key") String key, @PathVariable("mi") String mi) throws Exception{
//        Map<String,CronTrigger> triggerMap = applicationContext.getBeansOfType(CronTrigger.class);
//        List<Trigger> triggers = new ArrayList<>();
        System.out.println(key);
        System.out.println(mi);
        TriggerKey triggerKey = TriggerKey.triggerKey(key, QuzrtzConfig.GROUP_NAME);
        CronTriggerImpl cronTrigger = (CronTriggerImpl)scheduler.getTrigger(triggerKey);
        cronTrigger.setCronExpression("0/"+ mi +" * * * * ?");
        scheduler.rescheduleJob(triggerKey, cronTrigger);

        return "";
    }


    @GetMapping("/quartz/changeStatus/{key}/{status}")
    @ResponseBody
    public Object changeStatus(@PathVariable("key") String key, @PathVariable("status") boolean status) throws Exception{


        TriggerKey triggerKey = TriggerKey.triggerKey(key, QuzrtzConfig.GROUP_NAME);
        CronTriggerImpl cronTrigger = (CronTriggerImpl)scheduler.getTrigger(triggerKey);
//        cronTrigger.setCronExpression("0/"+ mi +" * * * * ?");


        if(status){
            scheduler.resumeTrigger(triggerKey);
        }else{
            scheduler.pauseTrigger(triggerKey);
        }
        return "";
    }


    @GetMapping("/quartz/executeNow/{jobKey}")
    @ResponseBody
    public Object executeNow(@PathVariable("jobKey") String jobKey) throws Exception{

        JobKey jk = JobKey.jobKey(jobKey, QuzrtzConfig.GROUP_NAME);
        scheduler.triggerJob(jk);

        return "";
    }

}
