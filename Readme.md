#### How to use Kamon ?
If you're looking for a quick "what does it take to add Kamon to a project?", this [commit](https://github.com/cmcmteixeira/foobar-tracing-demo/commit/ff05a9cd8ba25e8b500f17794b4d4c9a6ed7c8b6) should be able to help you.
    
#### Building the applications (optional)

```bash
docker-compose build
```
In order to try to reduce the number of required dependencies to run the project to
a minimum (only docker + docker-compose are required to run the project), it wasn't possible to
easily cache the projects scala/sbt dependencias which means that it takes a bit of time to build all the applications.
Last time I've build from scratch it took around 12m. 

#### Running the application

```bash
docker-compose up -d
``` 
WARNING: Due to the high number of services/applications (14 in total), it's recommended to increase the amount of memory and number of CPU cores
that the doker daemon can use. Currently 4GB of RAM seem to do the trick

#### Port Mappings and monitoring applications
| service | address|
|----------------|--------------
| grafana | 127.0.0.1:3000
| kibana | 127.0.0.1:5601
| jaeger | 127.0.0.1:16686
| zipkin | 127.0.0.1:9411
| console | 127.0.0.1:9999
| bartender | 127.0.0.1:9000
| taps | no ports are exposed

Additionally, the following apps also expose some ports:

|service| address |
|-----|----|
| logstash | check docker-compose.yml| 
| elasticsearch | check docker-compose.yml| 
| influxdb | check docker-compose.yml| 
| db | check docker-compose.yml| 
| jaeger | check docker-compose.yml| 
| zipkin | check docker-compose.yml| 
| rabbitmq | check docker-compose.yml| 

#### Testing and demoing
In order to facilitate the firing of request, a small script is included which can fire multiple types of requests in parallel

```bash
./pourdrinks.sh --water=1000 --soda=1000 --coke=1000 --par=3
# This specific call will fire 1000 water, soda and coke using a random order with at most 3 requests being processed in parallel
##Mac users may need to install `brew install coreutils` in order for the script to work
```

The progress of the calls can be accompanied by calling:
```bash
./monitor.sh
```




#### Description
The metric-pub platform pretends to map out a fully automated pub where a user can request 
drinks using an HTTP interface.

![Request Flow](docs/flow1.png?raw=true "") 
1) POST Console /drinkRequest {"drink": "soda"}
    * An Http request is made to the Console app which which specifies the desired drink
    * A response will be returned which contains the uuid associated w/ the request that was just made
2) POST Bartender /pour {"drink": "soda"} 
    * A request is made to the Bartender app with the desired drink 
    * A response is returned from the bartender which has the uuid associated w/ that request
    * This request (uuid and drink) are stored in the console's database
3) RabbitMQ {"identifier": "an-uuid", "drink" : "soda"}
    * A message is pushed into rabbitMQ, all taps use the same exchange but the routing key is unique for each drink
![Request Flow](docs/flow2.png?raw=true "") 

4) Push drink poured {"identifier": "an-uuid"} 
    * Once the drink has been poured (currently the tap will just sleep for a configurable amount of time), 
    the tap pushes an event signalling just that 
    * This event gets picked up by the bartender app
5) PATCH /drinkRequest/:uuid
    * The Bartender app signals to the Console app that the event has been successfully processed
    * The Console app updates it's internal state
6) \* At any time, the request status can be checked by calling GET /drinRequest/:uuid
