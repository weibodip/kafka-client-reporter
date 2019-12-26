package com.weibo.dip;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.weibo.dip.InfluxdbReporterConfConstants.*;
import static jdk.nashorn.internal.objects.Global.Infinity;

/**
 * Created by jianhong1 on 2019-11-12.
 */
public class InfluxdbReporter implements MetricsReporter, Runnable{
  private static final Logger LOGGER = LoggerFactory.getLogger(InfluxdbReporter.class);
  private static Map<String, InfluxDB.ConsistencyLevel> consistencyLevelMap;
  private static String hostname;

  private long interval;
  private String influxdbUrl;
  private String influxdbUsername;
  private String influxdbPassword;
  private String influxdbConsistency;
  private String influxdbDatabase;
  private String influxdbRetentionPolicy;
  private boolean influxdbReporterEnabled;

  private ScheduledExecutorService executor;
  private List<KafkaMetric> metricList;
  private InfluxDB influxDB;

  static {
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      LOGGER.error("get hostname error: {}", ExceptionUtils.getStackTrace(e));
    }

    consistencyLevelMap = new HashMap<>();
    consistencyLevelMap.put("all", InfluxDB.ConsistencyLevel.ALL);
    consistencyLevelMap.put("any", InfluxDB.ConsistencyLevel.ANY);
    consistencyLevelMap.put("one", InfluxDB.ConsistencyLevel.ONE);
    consistencyLevelMap.put("quorum", InfluxDB.ConsistencyLevel.QUORUM);
  }

  public InfluxdbReporter(){
  }

  @Override
  public void init(List<KafkaMetric> metrics) {
    metricList = metrics;
    if(!influxdbReporterEnabled){
      LOGGER.info("influxdb reporter disabled");
      return;
    }

    LOGGER.info("init influxdb reporter");
    if (influxDB == null) {
      influxDB = InfluxDBFactory.connect(influxdbUrl, influxdbUsername, influxdbPassword);
      LOGGER.info("InfluxDB connection created, {}", influxDB.ping().toString());
    }

    executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleWithFixedDelay(this, interval, interval, TimeUnit.SECONDS);
  }

  @Override
  public void metricChange(KafkaMetric metric) {
    metricList.add(metric);
  }

  @Override
  public void metricRemoval(KafkaMetric metric) {
    metricList.remove(metric);
  }

  @Override
  public void close() {
    executor.shutdown();
    String name = this.getClass().getSimpleName();
    try {
      while (!executor.awaitTermination(THREAD_POOL_AWAIT_TIMEOUT, TimeUnit.SECONDS)) {
        LOGGER.info("{} await termination", name);
      }
    } catch (InterruptedException e) {
      LOGGER.warn("{} await termination, but interrupted", name);
    }
    LOGGER.info("{} stopped", name);
  }

  @Override
  public void configure(Map<String, ?> configs) {
    String flag = ((Map<String, String>)configs).getOrDefault(INFLUXDB_REPORTER_ENABLE, DEFAULT_INFLUXDB_REPORTER_ENABLE);
    if("true".equals(flag) || "True".equals(flag) || "TRUE".equals(flag)) {
      influxdbReporterEnabled = true;
    }
    influxdbUrl = ((Map<String, String>)configs).getOrDefault(INFLUXDB_URL, DEFAULT_INFLUXDB_URL);
    influxdbUsername = ((Map<String, String>)configs).getOrDefault(INFLUXDB_USERNAME, DEFAULT_INFLUXDB_USERNAME);
    influxdbPassword = ((Map<String, String>)configs).getOrDefault(INFLUXDB_PASSWORD, DEFAULT_INFLUXDB_PASSWORD);
    influxdbConsistency = ((Map<String, String>)configs).getOrDefault(INFLUXDB_CONSISTENCY, DEFAULT_INFLUXDB_CONSISTENCY);
    influxdbDatabase = ((Map<String, String>)configs).getOrDefault(INFLUXDB_DATABASE, DEFAULT_INFLUXDB_DATABASE);
    influxdbRetentionPolicy = ((Map<String, String>)configs).getOrDefault(INFLUXDB_RETENTION_POLICY, DEFAULT_INFLUXDB_RETENTION_POLICY);
    interval = ((Map<String, Long>)configs).getOrDefault(INFLUXDB_REPORT_INTERVAL_SECOND, DEFAULT_INFLUXDB_REPORT_INTERVAL_SECOND);
  }

  @Override
  public void run() {
    try {
      BatchPoints batchPoints = BatchPoints.database(influxdbDatabase)
          .retentionPolicy(influxdbRetentionPolicy)
          .consistency(consistencyLevelMap.get(influxdbConsistency))
          .tag("hostname", hostname)
          .build();

      long currentTime = System.currentTimeMillis();
      for (KafkaMetric metric : metricList) {
        MetricName metricName = metric.metricName();
        double value = metric.value();

        //若值为无穷大，则把值转义为-1
        if (value == Infinity || value == -Infinity) {
          value = -1;
        }

        Point point = Point.measurement(metricName.name())
            .time(currentTime, TimeUnit.MILLISECONDS)
            .tag("group", metricName.group())
            .tag(metricName.tags())
            .field("value", value)
            .build();
        batchPoints.point(point);
      }
      influxDB.write(batchPoints);
      LOGGER.debug("send {} points to influxdb", batchPoints.getPoints().size());
    } catch (Exception e){
      LOGGER.error("send points to influxdb failed, {}", ExceptionUtils.getStackTrace(e));
    }
  }
}
