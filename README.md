
# Railway Ticket Booking System
## CS301 Project
***
Submitted by: Yash Jain (2019EEB1208) , Tushar Jain (2019EEB1207)

## File Structure
***
RAILWAY_TICKET_BOOKING_SYSTEM
    
    |
    |-- inputs
    |        |-- Trainschedule.txt
    |        |-- pool-1-thread-1_input.txt
    |        |-- pool-1-thread-2_input.txt
    |                   :
    |-- outputs
    |        |--pool-1-thread-1_output.txt
    |        |--pool-1-thread-2_output.txt
    |                   :
    |
    |-- ServiceModule.java
    |-- client.java
    |-- train_sched.java
    |-- triggers.sql
    |-- pql_jdbc_driver.jar
    |-- README.md
    |-- LICENSE

##  Steps to Execute
***
* Setup the database
  * Execute SQL commands from `projectdb.sql`.
  * Run  ``` java -cp .;psql_jdbc_driver.jar train_sched.java ``` (For Windows)  and  ``` java -cp psql_jdbc_driver.jar train_sched.java ``` (for Linux) in Terminal.
* Paste input files in `inputs` folder.
* Paste `Trainschedule.txt` in `inputs` folder.
* Change the username and password in the `config.properties` file as your psql username and password.
* Change number of first level threads and second level threads to required value in client.java line 19 and line 48 respectively.
* Set the number of server cores in ServerModule.java line 21.
* Start the Server
  * Run  ``` java -cp .;psql_jdbc_driver.jar ServiceModule.java ``` (For Windows)  and  ``` java -cp psql_jdbc_driver.jar ServiceModule.java ``` (for Linux) in Terminal.
* Start the Clients
  * Run ```java client.java``` in another Terminal.
