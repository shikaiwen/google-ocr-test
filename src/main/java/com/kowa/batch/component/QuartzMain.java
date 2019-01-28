package com.kowa.batch.component;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzMain {


    public static void main(String []args) throws Exception{
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();


         JobDetail jobDetail = JobBuilder.newJob(HelloJob.class)
             .withIdentity("myJob", "group1")
             .build();



        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("myTrigger", "group1")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever()
                        )
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        Thread.sleep(6000);

        scheduler.shutdown();
    }
}
