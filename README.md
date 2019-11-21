## 说明

实现了MetricsReporter类，通过Kafka客户端暴露的metric.reporters接口，把采集到的数据写入InfluxDB。 


## 安装

1. 打jar包 `mvn clean package`

1. 把打好的jar包添加到Kafka客户端`classpath`目录

1. 配置Kafka客户端（配置见下文）

1. 重启Kafka客户端

## 配置

启用InfluxdbReporter

    metric.reporters=com.weibo.dip.InfluxdbReporter
    influxdb.reporter.enabled=true"
    
下面是默认配置：
    
    influxdb.url=http://localhost:8060
    influxdb.database=kafka
    influxdb.username=root
    influxdb.password=root
    influxdb.consistency=all
    influxdb.retention.policy=autogen
    influxdb.report.interval.second=60
    
