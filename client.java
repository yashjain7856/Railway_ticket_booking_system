import java.util.Scanner;
import java.util.concurrent.ExecutorService ;
import java.util.concurrent.Executors   ;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException  ;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class client
{
    public static void main(String args[])throws IOException
    {
        double startTime = System.nanoTime();
        int firstLevelThreads = 7;   // Indicate no of users 
        /**************************/
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(firstLevelThreads);
        
        for(int i = 0; i < firstLevelThreads; i++)
        {
            Runnable runnableTask = new invokeWorkers();    //  Pass arg, if any to constructor sendQuery(arg)
            executorService.submit(runnableTask);
        }

        executorService.shutdown();
        try
        {    // Wait for 10 sec and then exit the executor service
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
            {
                executorService.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            executorService.shutdownNow();
        }
        double stopTime = System.nanoTime();

        System.out.println("Execution Time (in ms) = "+ ((stopTime-startTime)/1000000));
    }
}

class invokeWorkers implements Runnable
{
    /*************************/
    int secondLevelThreads = 8;
    /**************************/
    public invokeWorkers()            // Constructor to get arguments from the main thread
    {
        // Send args from main thread
    }
    
    ExecutorService executorService = Executors.newFixedThreadPool(secondLevelThreads) ;
    
    public void run()
    {
        for(int i=0; i < secondLevelThreads ; i++)
        {
            Runnable runnableTask = new sendQuery()  ;    //  Pass arg, if any to constructor sendQuery(arg)
            executorService.submit(runnableTask) ;
        }

        sendQuery s = new sendQuery();      // Send queries from current thread
        s.run();
        
        // Stop further requests to executor service
        executorService.shutdown()  ;
        try
        {
            // Wait for 8 sec and then exit the executor service
            if (!executorService.awaitTermination(8, TimeUnit.SECONDS))
            {
                executorService.shutdownNow();
            } 
        } 
        catch (InterruptedException e)
        {
            executorService.shutdownNow();
        }
    }
}
    

class sendQuery implements Runnable
{
    int sockPort = 7005 ;
    public void run()
    {
        try 
        {
            //Creating a client socket to send query requests
            Socket socketConnection = new Socket("localhost", sockPort) ;
            
            // Files for input queries and responses
            String inputfile = "./TestCases/input/input_64/" + Thread.currentThread().getName() + "_input.txt" ;
            String outputfile = "./outputs/" + Thread.currentThread().getName() + "_output.txt" ;

            System.out.println(outputfile);
            //-----Initialising the Input & ouput file-streams and buffers-------
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection.getOutputStream());
            BufferedWriter bufferedOutput = new BufferedWriter(outputStream);
            InputStreamReader inputStream = new InputStreamReader(socketConnection.getInputStream());
            BufferedReader bufferedInput = new BufferedReader(inputStream);
            PrintWriter printWriter = new PrintWriter(bufferedOutput,true);
            
            File queries = new File(inputfile); 
            File output = new File(outputfile); 
            FileWriter filewriter = new FileWriter(output);
            Scanner sc = new Scanner(queries);
            String query = "";
            //--------------------------------------------------------------------

            // Read input queries
            while(sc.hasNextLine())
            {
                query = sc.nextLine();
                printWriter.println(query);
            }

            // Get query responses from the input end of the socket of client
            char c;
            while((c = (char) bufferedInput.read()) != '#')      
            {
                filewriter.write(c);
            }

            // close the buffers and socket
            filewriter.close();
            sc.close();
            socketConnection.close();
        } 
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
    }
}