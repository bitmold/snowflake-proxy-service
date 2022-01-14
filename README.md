# Snowflake Proxy Service 

Service to start and stop a <a href="https://snowflake.torproject.org/">Snowflake Proxy</a> based off of <a href="https://github.com/tladesignz/IPtProxy/">IPtProxy</a>. This is a work in progress at the moment...


### Getting Started


To Use `SnowflakeProxyService` in your `Activity` create an `Intent` and start it as follows:


```kotlin
val intent = Intent(this, SnowflakeProxyService::class.java)
	.setAction(SnowflakeProxyService.ACTION_START)


if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
	startForegroundService(intent)
else
	startService(intent)

```

This will start the proxy service in your application. After starting the service, in your app, you can use `BroadcastReceiver` to listen to events from `Service`. You may listen to the events `SnowflakeProxyService.ACTION_CLIENT_CONNECTED`, `SnowflakeProxyService.ACTION_PAUSING` and `SnowflakeProxyService.ACTION_RESUMING`

```kotlin
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = when (intent?.action) {
            SnowflakeProxyService.ACTION_CLIENT_CONNECTED -> { 
            	// a client used the proxy to evade censorship, you can ask the Service to see how many clients used your proxy since starting it
                val count = intent.getIntExtra(SnowflakeProxyService.EXTRA_CLIENT_CONNECTED_COUNT, -1)
                
            }
            SnowflakeProxyService.ACTION_PAUSING -> {
                // the snowflake proxy is pausing, the reason it is pausing can be obtained as follows:
                val reason = intent.getStringExtra(SnowflakeProxyService.EXTRA_PAUSING_REASON)
                
            }
            SnowflakeProxyService.ACTION_RESUMING -> {
                // the snowflake proxy is starting up 
            }
            else -> {}
        }
    }
```

Typically in your `Activity`'s `onCreate(Bundle?)` method, your app specifies wihch events, if any, it wants to listen to. 


```kotlin
LocalBroadcastManager.getInstance(this).apply {
            registerReceiver(receiver, IntentFilter(SnowflakeProxyService.ACTION_CLIENT_CONNECTED))
            registerReceiver(receiver, IntentFilter(SnowflakeProxyService.ACTION_PAUSING))
            registerReceiver(receiver, IntentFilter(SnowflakeProxyService.ACTION_RESUMING))
        }
```

### Pausing the Proxy 

`SnowflakeProxyService` can be configured to pause the snowflake proxy. This means that the service will still continue to be running, but that your app won't act as a snowflake. Currently `SnowflakeProxyService` can be configured to pause if the device loses connection to power and/or if the device loses its network connection to an unmetered network connection. 

The above example can be extended to pause the service by adding in `Intent` extras to optionally pause `SnowflakeProxyService`. In this example, your app will only function as a Snowflake proxy if the device is connected to power and an unmetered connection (typically a Wi-Fi connection):

```kotlin
val intent = Intent(this, SnowflakeProxyService::class.java)
	.setAction(SnowflakeProxyService.ACTION_START)
	.putExtra(SnowflakeProxyService.EXTRA_START_CHECK_POWER, true)
	.putExtra(SnowflakeProxyService.EXTRA_START_CHECK_UNMETERED, true)

```  

If either of these `Intent` extra's are specified, `SnowflakeProxyService` may emit events with the action `SnowflakeProxyService.ACTION_PAUSING`. Your app may want to listen to these events and update its UI to explain why the proxy has stopped. 

When the proxy resumes, an event of `SnowflakeProxyService.ACTION_RESUMING` is emitted. 

### Proxy Configuration 

Your app can fully configure the underlying snowflake proxy, although by default `SnowflakeProxyService` uses the defaults specified in <a href="https://github.com/tladesignz/IPtProxy">IPtProxy</a>. An up-to-date explanation of how snowflake functions on <a href="https://gitlab.torproject.org/tpo/anti-censorship/pluggable-transports/snowflake/-/wikis/Technical%20Overview">The Tor Project's wiki</a>.

```kotlin
val intent = Intent(this, SnowflakeProxyService::class.java)
	.setAction(SnowflakeProxyService.ACTION_START)
                
	// URL configuration
	.putExtra(SnowflakeProxyService.EXTRA_PROXY_BROKER_URL, "https://snowflake-broker.torproject.net/")
	.putExtra(SnowflakeProxyService.EXTRA_PROXY_RELAY_URL, "wss://snowflake.bamsoftware.com/")
	.putExtra(SnowflakeProxyService.EXTRA_PROXY_BROKER_URL, "stun:stun.stunprotocol.org:3478")
	 putExtra(SnowflakeProxyService.EXTRA_PROXY_NAT_PROBE_URL, "https://snowflake-broker.torproject.net:8443/probe")


	// Proxy configuration 
	.putExtra(SnowflakeProxyService.EXTRA_PROXY_LOG_FILE_NAME, "mylog.log") // log file, default is STDERR
	.putExtra(SnowflakeProxyService.EXTRA_PROXY_CAPACITY, 10) // number of concurrent clients 
	.putExtra(SnowflakeProxyService.EXTRA_PROXY_USE_UNSAFE_LOGGING, false) // scrub logs

```

### Toast Configuration 


`SnowflakeProxyService` can optionally display a `Toast` to your users whenever someone uses your snowflake to bypass censorship. 


```kotlin
val intent = Intent(this, SnowflakeProxyService::class.java)
	.setAction(SnowflakeProxyService.ACTION_START)
	.putExtra(SnowflakeProxyService.EXTRA_START_SHOW_TOAST, true)


	// this displays: ❄️ Your snowflake proxy helped someone circumvent censorship ❄️

	// unless you specify your own message:
	.putExtra(SnowflakeProxyService.EXTRA_START_TOAST_MESSAGE, "yay, someone got connected thanks to you")

```