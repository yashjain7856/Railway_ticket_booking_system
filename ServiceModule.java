import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Properties;
import java.io.*;

/**
 * Main Class to controll the program flow
 */
public class ServiceModule 
{
    static int serverPort = 7005;
    static int numServerCores = 70;
    //------------ Main----------------------
    public static void main(String[] args) throws IOException 
    {    
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);
        
        //Creating a server socket to listen for clients
        ServerSocket serverSocket = new ServerSocket(serverPort); //need to close the port
        Socket socketConnection = null;
        
        // Always-ON server
        while(true)
        {
            System.out.println("Listening port : " + serverPort + "\nWaiting for clients...");
            socketConnection = serverSocket.accept();   // Accept a connection from a client
            System.out.println("Accepted client :"  + socketConnection.getRemoteSocketAddress().toString()+ "\n");
            //  Create a runnable task
            Runnable runnableTask = new QueryRunner(socketConnection);
            //  Submit task for execution   
            executorService.submit(runnableTask);   
        }
    }
}


class QueryRunner implements Runnable
{
    //  Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket)
    {
        this.socketConnection =  clientSocket;
    }    

    public void query_excecute(Connection c, String query){
        try {
            c.createStatement().executeQuery(query);
        } catch (SQLException e) {
            if(!e.getSQLState().equals("02000"))
                System.out.println(e.getSQLState()  + "   C");
        }
    }
    public void run()
    {
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
            try  
            {
                //  Reading data from client
                InputStreamReader inputStream = new InputStreamReader(socketConnection.getInputStream()) ;
                BufferedReader bufferedInput = new BufferedReader(inputStream) ;                                                      
                OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection.getOutputStream()) ;
                BufferedWriter bufferedOutput = new BufferedWriter(outputStream) ;                                                         
                PrintWriter printWriter = new PrintWriter(bufferedOutput, true) ;
                
                String clientCommand = "" ;
                String responseQuery = "" ;
                while(true)
                {
                    // Read client query
                    clientCommand = bufferedInput.readLine();
                    if(clientCommand.equals("#"))
                    {
                        printWriter.println("#");
                        String returnMsg = "Connection Terminated - client : " + socketConnection.getRemoteSocketAddress().toString();
                        System.out.println(returnMsg);
                        inputStream.close();
                        bufferedInput.close();
                        outputStream.close();
                        bufferedOutput.close();
                        printWriter.close();
                        socketConnection.close();
                        return;
                    }
                    String[] attr = clientCommand.split("\\s+");
                
                    int n_pass = Integer.parseInt(attr[0]);
                    
                    String train_number = attr[n_pass+1];
                    String date_of_journey = attr[n_pass+2];
                    String c_type = attr[n_pass+3];
                    String names= "";

                    for(int i = 0;i<n_pass;i++){
                        names += attr[i+1];
                    }
                    
                    // Number of available seats in train
                    String start_transaction = "START TRANSACTION ISOLATION LEVEL SERIALIZABLE;";
                    String ticket_booking_query ="SELECT * FROM book_ticket ('"+train_number+"','"+date_of_journey+"','"+c_type+"',"+n_pass+",'{"+names+"}') AS PNR;";
                    String commit_transaction = "COMMIT;";
                    String rollback_transaction = "ROLLBACK;";
                    String PNR = "-1";
                    responseQuery = "Failed! " + clientCommand; // default case if booking failed by any reason
                    while(true){
                        query_excecute(c, start_transaction);
                        try {
                            ResultSet rst =   c.createStatement().executeQuery(ticket_booking_query);
                            while(rst.next()){
                                PNR = rst.getString("PNR");
                            }
                            if(!PNR.equals("-1")){
                                responseQuery = "Successful | PNR: "+ PNR +" | Train: "+train_number+" | Date: "+date_of_journey;
                                String getberth = "Select * from ticket_passengers where PNR = '"+PNR+"';";
                                ResultSet rst2 =   c.createStatement().executeQuery(getberth);
                                while(rst2.next()){
                                    responseQuery += " | "+rst2.getString("p_name")+" "+c_type+"/";
                                    responseQuery += rst2.getString("c_number")+"/";
                                    responseQuery += rst2.getString("b_number")+"";
                                    ResultSet rst3 =   c.createStatement().executeQuery("Select b_type from " + c_type + "_coach where b_number = "
                                        +rst2.getString("b_number"));
                                    while(rst3.next()){
                                        responseQuery += "/"+ rst3.getString("b_type");
                                    }
                                    
                                }   
                            }
                            c.createStatement().executeQuery(commit_transaction);
                            break;
                        } catch (SQLException e) {
                            if((e.getSQLState().equals("40001")) || (e.getSQLState().equals("40P01"))){
                                query_excecute(c, rollback_transaction);
                                continue;
                            }
                            else{
                                if(!e.getSQLState().equals("02000"))
                                    System.out.println(e.getSQLState());
                                query_excecute(c, rollback_transaction);
                                break;
                            }
                        }
                    }
                    
                    //----------------------------------------------------------------
                    //  Sending data back to the client
                    printWriter.println(responseQuery);     
                }    
            }    
            catch(IOException e)
            {
                return;
            }
        }catch (Exception e) {
            System.out.println(e);
        }    
    }    
}    

