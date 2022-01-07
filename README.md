# Snowflake Proxy Service 

Service to start and stop a <a href="https://snowflake.torproject.org/">Snowflake Proxy</a> based off of <a href="https://github.com/tladesignz/IPtProxy/">IPtProxy</a>. 

## Work in progress at the moment.....

You can start the Service and specify if you want the Service to monitor if the device is connected to power and if the device is connected to a metered or unmetered connection. If the Service is tracking these things, it'll stop running the proxy and broadcast back to the client Activity why it has stopped ("power disconnected", "wifi disconnected", etc). If power is reconnected or the wifi connection is restablished the Service will resume running the snowflake proxy. 

The Service also sends an event back to the client Context whenever the proxy has helped someone bypass censorship. 
