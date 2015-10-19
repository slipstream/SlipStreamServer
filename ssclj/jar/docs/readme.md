## Usages

### Usage Records

`Usage records` represent the most atomic piece of information regarding usages.
It is a REST resource managed by the API server. As it is a technical resource (used only internally by Web Server and 
not exposed to SlipStream users, only POST operation is currently supported). 

A usage record is typically created by Web server during the lifecycle of a VM.
A POST is made with a new usage record (that does not contain an end_timestamp) when a VM is started.
When it is stopped, another POST is made with the same information but including the end_timestamp.
See UsageRecorder class for more details.

**Note that a single usage record is stored in database on multiple rows (one for each metric).**

To see in database, some useful commands:

How many usage records:

```
java -jar /opt/hsqldb/lib/sqltool.jar --inlineRc=url=jdbc:hsqldb:hsql://localhost:9001/ssclj,user=sa,password= --sql 'select count(*) from "usage_records";'
```


```
java -jar /opt/hsqldb/lib/sqltool.jar --inlineRc=url=jdbc:hsqldb:hsql://localhost:9001/ssclj,user=sa,password= --sql "select * from \"usage_records\" where \"cloud\"='ec2-eu-west' and \"user\"='sixsq_dev' limit 10;"
```

cloud_vm_instanceid    | user      | cloud       | start_timestamp          | end_timestamp            | metric_name | metric_value
---------------------- | --------- | ----------- | ------------------------ | ------------------------ | ----------- | ------------
ec2-eu-west:i-8034d12a | sixsq_dev | ec2-eu-west | 2015-05-08T02:18:17.816Z | 2015-05-08T02:23:29.097Z | vm          |        1.0E0
ec2-eu-west:i-a5e7080f | sixsq_dev | ec2-eu-west | 2015-05-13T10:24:59.443Z | 2015-05-13T10:32:59.859Z | vm          |        1.0E0
ec2-eu-west:i-f56a9a5f | sixsq_dev | ec2-eu-west | 2015-05-19T11:25:52.620Z | 2015-05-19T11:29:01.093Z | vm          |        1.0E0
ec2-eu-west:i-ce6a9a64 | sixsq_dev | ec2-eu-west | 2015-05-19T11:27:42.938Z | 2015-05-19T11:29:06.054Z | vm          |        1.0E0
ec2-eu-west:i-8609f62c | sixsq_dev | ec2-eu-west | 2015-05-21T15:17:44.964Z | 2015-05-21T16:22:54.338Z | vm          |        1.0E0
ec2-eu-west:i-660bf4cc | sixsq_dev | ec2-eu-west | 2015-05-21T15:17:45.030Z | 2015-05-21T16:23:04.565Z | vm          |        1.0E0
ec2-eu-west:i-877b852d | sixsq_dev | ec2-eu-west | 2015-05-22T03:18:27.708Z | 2015-05-22T04:22:12.502Z | vm          |        1.0E0
ec2-eu-west:i-f97b8553 | sixsq_dev | ec2-eu-west | 2015-05-22T03:18:27.669Z | 2015-05-22T04:22:12.589Z | vm          |        1.0E0
ec2-eu-west:i-4741b9ed | sixsq_dev | ec2-eu-west | 2015-05-23T03:19:48.401Z | 2015-05-23T04:23:51.568Z | vm          |        1.0E0
ec2-eu-west:i-cb41b961 | sixsq_dev | ec2-eu-west | 2015-05-23T03:19:48.464Z | 2015-05-23T04:23:51.651Z | vm          |        1.0E0


### Usage summaries

This is also a REST resource.

A Usage Summary is the aggregation of all metrics for a given time interval, user and cloud.
e.g:

TODO
One usage summary
TODO
{"vm":{"unit_minutes":1440.0},
 "ram":{"unit_minutes":4.718592E7},
 "disk":{"unit_minutes":0.0},
 "instance-type.Huge":{"unit_minutes":1440.0}}


### Process

#### Computation of Usage Summaries

Each day at 2 AM (UTC), a cron job computes and stores the summaries for all users on each cloud.
Here are the details of the execution on nuv.la:

```
cron job: 
/etc/cron.d/create-daily-usage 

--> triggers at 2 AM

/usr/sbin/create-daily-usage.sh (simple wrapper around clojure launcher without arguments)

--> that calls  

com.sixsq.slipstream.ssclj.usage.daily_summary_launcher
```

`daily_summary_launcher` by default (no arguments) computes the summaries for yesterday.
It can also be called with optional arguments `-d date`
e.g: 

```
daily_summary_launcher -d 20150921
```

#### Manual checks

To summarize manually one given month from the command line:

```
java -Ddb.config.path=src/main/resources/config-hsqldb.edn \
   -cp ".:target/SlipStreamCljResources-jar-2.18-SNAPSHOT-jar-with-dependencies.jar" \
    com.sixsq.slipstream.ssclj.usage.monthly_summary_launcher \
    -m 2016-03     
```

To get one monthly usage for a given cloud:

```
curl -H "slipstream-authn-info: azure azure" "http://localhost:8201/api/usage?%24filter=start_timestamp%3D2016-03-01%20and%20end_timestamp%3D2016-04-01"
```

#### Send Usage emails

Each day at 3 AM UTC, a cron job sends via email (to users having set the preference) these usage summaries.
Here are the details of the execution on nuv.la:

cron job:
/etc/cron.d/daily-usage-emails

--> triggers at 3 AM

/usr/sbin/send-daily-usage-emails.sh

--> that calls

com.sixsq.slipstream.action.DailyUsageSender (Java class actually doing the job)

The DailyUsageSender queries the Usage Summary resource with the help of the CIMI filter.












