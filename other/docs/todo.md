### TODO
* Create Arduino or Java program to simulate MATE or Rover
* Create a calendar program to be able to view daily kWh on a calendar interface
  * If we do something with a calendar, we could use Google calendar https://developers.google.com/calendar/create-events/
* Create button/widget on Grafana that sends an encrypted command to shut off the generator
* Packet for disk usage
* Send packet when mate serial port hasn't output data for 30 seconds
* Create Dockerfile and example docker-compose file
* Add event packets for rover and tracer devices (like we did for MXs and FXs)
* Short term record packets for high/low battery voltage, FX inverter current, pv wattage, charging current, etc  
  * This would be very useful so that if packets are replaced there is still information on how
  high or low the battery voltage got or how high the load was
  * Another possibility is that multiple packets could be saved per each period
    * For instance this document id: `2021,06,12,19,(15/30),[d9587f95]` could also have additional things to tack onto
    it such as: `2021,06,12,19,(15/30),[d9587f95]-high_battery_voltage` or `2021,06,12,19,(15/30),[d9587f95]-high_fx_inv`.
    This has the advantage of being easy to query. When data is queried, the last packet in that period is queried
    along with the record packets, meaning no additional code needs to be written to find the high/low packets
    for older packets.
    * Because of the additional overhead of saving more packets per hour, the `unique` option could be lowered to
    something like 15 per hour.
* Look into graylog or logstash for better logging
  * https://hub.docker.com/r/graylog/graylog
* Look into implementing pymate like features into SolarThing: https://github.com/jorticus/pymate
  * https://github.com/jorticus/pymate/blob/master/doc/protocol/Protocol.md
* Add way to queue up commands in the automation program
  * The goal here is to be able to say "start generator at 5PM" and then also be able to cancel it
* Make sure clients tolerate quick disconnects and reconnects from a device so that devices with unstable connections
don't look like they're constantly disconnecting and reconnecting
* Add action to store an informational message in the database
  * This is good for feedback for a particular command that is running.
* Add packet for rover write commands that ran - (we already have one for mate commands)
* Possibly refactor PacketListReceiver
  * Get rid of InstantType
  * Have a way to tell if a packet included in the packets list is important enough to warrant storing in the database.
    * Right now we do a bunch of packets.isEmpty() checks to see if we should continue adding packets, but there's probably a better way
* Better way to update SNAPSHOT jar so that running SolarThing instances don't get mad
  * We have a great setup for versioned SolarThing jar files, because running instances will still use
  whatever jar solarthing.jar pointed them to originally, but this isn't the case when we actually change the jar it is pointing to
* Backend Grafana plugin to allow commands to be sent
* Have a CouchDB view that emits null so that the millis view can be more efficient and people can use include_docs=true
  * https://www.dimagi.com/blog/what-every-developer-should-know-about-couchdb/
* Have option to use shorter document IDs
  * CouchDB recommends shorter IDs: https://docs.couchdb.org/en/stable/maintenance/performance.html#document-s-id
* GraphQL queries for getting the angle of the sun in the sky (current and highest point in the day)
  * https://ipgeolocation.io/documentation/astronomy-api.html - unintuitive Java SDK with bad documentation
  * https://github.com/caarmen/SunriseSunset - gross requirement to use java.util.Calendar
  * https://github.com/mikereedell/sunrisesunsetlib-java - better, but still requires use of java.util.Calendar
* Serialize convenience fields only if a certain condition is met (maybe a config option)
  * Heck, maybe only serialize convenience fields in debug messages
* Don't serialize available commands packet in every single packet collection
  
### Completed
* Provide option/configuration for multiple MATEs (maybe using multiple databases with an id at the end? i.e.: solarthing-1, solarthing-2 or commands-1, commands-2)
    * Done by using fragmented packets. Will be stored in the same database but uses InstancePackets to indicate source and fragment ids
* Add field to MX Status Packet to indicate whether it supports dailyAH and field to indicate the version of the MX or if it is a FM
* Create a PacketHandler that saves json data to a file location that can be easily accessed using a Apache web server
* Add better logging with timestamps
* Make Grafana easy to use by supporting InfluxDB as a database
* Add a basic file for a systemd service
* Stop requiring command line parameters and have option to use all JSON configs
* Integrate power usage
* Detect when Mate packets go from address 4->1 or 1->1 to detect a new collection of packets
  * This was done by just removing duplicate packets. Not ideal, but it works.
* Change command_feedback to something generic and use that database for "event"-like data
  * Also upload this to InfluxDB
* Have packets for inverters going on/off, AC Drop/AC Use, Daily kWh->0, etc
* Log CPU temperature, ram, etc into a database and logs.
  * Use the `vcgencmd measure_temp` available on Raspberry Pis
* Query all Renogy Rover data at once by reading almost all the registers.
* Use Google Analytics Collection API https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters
  * Could use this: https://github.com/brsanthu/google-analytics-java
* Make systemd programs use "solarthing" user instead of root
* Use Mattermost as a way to send push notifications for things
* DS18B20 temperature sensor
* Make command requests use JSON
* Store metadata
  * Put this in a document named `meta` in `solarthing_closed`
* Add temperature event to message program
* Create GraphQL query that allows someone to get dailyKWH from a large time period (many days) for
easy displaying in Grafana
* Do something with https://www.home-assistant.io/ somehow
* Log data to https://solcast.com.au/
* Notification for when MX wakes up and goes straight to float (happens on older models)
* Support InfluxDB 2.0
* Don't report analytics for dummy rover programs
* Silence warning for GraphQL
* Make CouchDB and InfluxDB database configuration warn when setting unused values
* Add way to scan for rover modbus slave addresses
* Have standard timings for grouping/sorting packets and increase time for GraphQL grouped queries so that
  all temperature readings show up even if one is from 10 minutes ago.
* More accurate solcast by using actual kWh data throughout the day

### Look into
* Look into supporting Elasticsearch, MongoDB, Graphite
* Support https://dweet.io
  * Will make using freeboard easy
* https://thingspeak.com/
* https://github.com/netdata (grafana alternative?)
* https://github.com/grafana/loki (for logging)
* thingsboard.io
* Cypher [guide here](https://neo4j.com/developer/guide-sql-to-cypher/)
  * Looks interesting
* [Metrictank](https://grafana.com/oss/metrictank/)
* IntelliJ is complaining about "busy waiting". We do a lot of this. Should we change how we're doing this?

#### Additions I'm not going to work on
These might be useful to some people. I will not implement these in the future, but pull requests are welcome!
* Implement Outback FlexNet DC Packets
  * I do not have a FlexNet DC unit and I do not plan to get one anytime soon
* Create 'exporter' module which will have a Prometheus exporter that runs and gets data from CouchDB
  * This is not useful to me, but anyone else is welcome to do this
* Use https://emoncms.org/ to graph data (https://emoncms.org/site/api#input)
  * Some of this is done, but it will not be worked on any further by me
* Use Matrix Chat as a notification service https://github.com/matrix-org
  * I found Synapse a pain to set up, but might be worth trying in the future
  
  
#### Interesting stuff that will probably never be implemented
* https://github.com/resilience4j/resilience4j It might be cool to try and heavily structure the client program around
resilience4j
