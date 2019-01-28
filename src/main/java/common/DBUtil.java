package common;

import com.kowa.batch.config.MainConfig;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

public class DBUtil {


    static Logger logger = Logger.getLogger(DBUtil.class);




    public static Connection getConnection() throws SQLException {
//        WebApplicationContext webApplicationContext = ContextLoader.getCurrentWebApplicationContext();


        Connection connection =  MainConfig.dataSource.getConnection();
        return connection;
//        Connection connection = DriverManager.getConnection(Constants.DB_URL, Constants.USER_NAME, Constants.PASSWORD);
//        return connection;

    }

    public static void close(Connection connection){

        if(connection != null ){
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("get connection failed", e);
            }
        }

    }

//    private static ApplicationContext context;
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        context = applicationContext;
//    }
}
