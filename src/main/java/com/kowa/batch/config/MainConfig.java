package com.kowa.batch.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MainConfig {

    @Value("${jdbc.url}")
    private String dbUrl;
    @Value("${jdbc.username}")
    private String username;
    @Value("${jdbc.password}")
    private String password;

    public static DataSource dataSource;

    @Bean
    public DataSource dataSource() {
//        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
//        dataSourceBuilder.type(DruidDataSource.class);
//        dataSourceBuilder.driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
//        dataSourceBuilder.url(dbUrl);
//        dataSourceBuilder.username(username);
//        dataSourceBuilder.password(password);
//        DataSource dataSource = dataSourceBuilder.build();




        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(dbUrl);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);
        druidDataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        druidDataSource.setInitialSize(5);
        druidDataSource.setMinIdle(5);
        druidDataSource.setMaxActive(10);

        MainConfig.dataSource = druidDataSource;


        return dataSource;
    }


    @Bean
    public ServletRegistrationBean dataSourceMonitorServlet(){
        ServletRegistrationBean servRegBean = new ServletRegistrationBean();
        servRegBean.setServlet(new com.alibaba.druid.support.http.StatViewServlet());
        servRegBean.addUrlMappings("/druid/*");
        servRegBean.setLoadOnStartup(1);
        return servRegBean;
    }

}
