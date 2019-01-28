package com.kowa.batch.quartz;

import common.DBUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.Connection;

@DisallowConcurrentExecution
public class JobWrapper implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        if(true){
            try {
                String jobName = jobExecutionContext.getJobDetail().getKey().getName();
                String triggerKey = jobExecutionContext.getTrigger().getKey().getName();
                System.out.println("job " + jobName + "... , trigger key :" + triggerKey);
                Connection co = DBUtil.getConnection();
                co.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            main(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
    }
}
