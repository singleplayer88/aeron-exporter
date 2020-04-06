# aeron-exporter
Aeron counters exporter for Prometheus.

```
java -jar -Dport=9001 aeron-exporter.jar
```

Exposes Aeron system counters as metrics for Prometeus to poll. 

Uses a default aeron directory to search for cnc.dat file. A custom aeron directory can be set up with a "aeron.dir" property. 

Uses jetty as a web server to expose the endpoint. 
