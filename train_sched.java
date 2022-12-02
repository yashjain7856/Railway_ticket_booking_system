import java.sql.Connection;
import java.sql.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Properties;
import java.io.*;

public class train_sched {
    public static void main(String args[]) {
        File configFile = new File("config.properties");
        Properties props = new Properties();
        try {
            FileReader reader = new FileReader(configFile);
            props.load(reader);

        } catch (FileNotFoundException ex) {
            System.out.println("Config.properties File Not Found");
        } catch (IOException ex) {
            System.out.println(ex);
        }
        String password = props.getProperty("password");
        String username = props.getProperty("username");
        String databaseName = props.getProperty("databaseName");

        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/"+databaseName, username, password);
            
            try {
                File myObj = new File("TestCases/input/Trainschedule_throughput.txt");
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();

                    if(data.equals("#")) break;

                    String inp[] = data.split("\\s+");
                    String query = "INSERT INTO trains_released(T_number,Date,N_AC,N_SL) VALUES ("+inp[0]+",'"+inp[1]+"',"+inp[2]+","+inp[3]+");";
                    
                    try {
                        c.createStatement().executeQuery(query);
                    } catch (SQLException e) {
                        if(!e.getSQLState().equals("02000"))
                            System.out.println(e);
                    }
                    
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
// java -cp .;org.jdbc_driver.jar train_sched.java
// ssh course2@172.30.2.245