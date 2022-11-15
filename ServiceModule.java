import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
// import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// import javax.xml.crypto.Data;

/**
 * Main Class to controll the program flow
 */
public class ServiceModule 
{
    static int serverPort = 7005;
    static int numServerCores = 2 ;
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
            System.out.println("Listening port : " + serverPort 
                                + "\nWaiting for clients...");
            socketConnection = serverSocket.accept();   // Accept a connection from a client
            System.out.println("Accepted client :" 
                                + socketConnection.getRemoteSocketAddress().toString() 
                                + "\n");
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

    // public void query_excecute(Connection c, String query){
    //     try {
    //         c.createStatement().executeQuery(query);
    //     } catch (Exception e) {
    //         System.out.println(e);
    //     }
    // }

    public void run()
    {
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/projectdb",
                    "postgres", "yash@7856");
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
                // String queryInput = "" ;
                while(true)
                {
                    // Read client query
                    clientCommand = bufferedInput.readLine();
                    // System.out.println("Recieved data <" + clientCommand + "> from client : " 
                    //                     + socketConnection.getRemoteSocketAddress().toString());

                    //  Tokenize here
                    // StringTokenizer tokenizer = new StringTokenizer(clientCommand);
                    
                    // queryInput = tokenizer.nextToken();
                    if(clientCommand.equals("#"))
                    {
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
                    // for(int i = 1;i<n_pass;i++){
                    //     attr[i] = attr[i].substring(0,attr[i].length()-1);
                    // }
                    String train_number = attr[n_pass+1];
                    String date_of_journey = attr[n_pass+2];
                    String c_type = attr[n_pass+3];
                    String names= "";
                    for(int i = 0;i<n_pass;i++){
                        names += attr[i+1];
                    }
                    
                    // Number of available seats in train
                    String ticket_booking_query = "SELECT * FROM book_ticket ("+train_number+",'"+date_of_journey+"','"+c_type+"',"+n_pass+",'{"+names+"}');";
                    int status;
                    try {
                        ResultSet rst =   c.createStatement().executeQuery(ticket_booking_query);
                        while(rst.next()){
                            status = Integer.parseInt(rst.getString("book_ticket"));
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }

                    //-------------- your DB code goes here----------------------------
                    // try
                    // {
                    //    // Thread.sleep(6000);    
                    // } 
                    // catch (InterruptedException e)
                    // {
                    //     e.printStackTrace();    
                    // }
                    responseQuery = "******* Dummy result ******";

                    //----------------------------------------------------------------
                    
                    //  Sending data back to the client
                    printWriter.println(responseQuery); 
                    // System.out.println("\nSent results to client - " 
                    //                     + socketConnection.getRemoteSocketAddress().toString() );
                    
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

