# Readme

## To view H2 Database content 

Reference: [http://www.activiti.org/userguide/#apiDebuggingUnitTest](http://www.activiti.org/userguide/#apiDebuggingUnitTest)

1.Set a break point in the code

2."Display", type the below, highlight and right-click "Display" org.h2.tools.Server.createWebServer("-web").start()

3.visit http://localhost:8082/ in a browser, fill in the JDBC URL to the in-memory database (by default this is jdbc:h2:mem:activiti), and hit the connect button