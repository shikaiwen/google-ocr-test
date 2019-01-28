package com.kowa.batch.component;


import org.quartz.*;

public class HelloJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {


        JobDetail detail = jobExecutionContext.getJobDetail();
        Trigger trigger = jobExecutionContext.getTrigger();
        System.out.println("key"+trigger.getKey().getName());

    }
}
