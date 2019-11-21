package com.weibo.dip;

/**
 * Created by jianhong1 on 2019-11-18.
 */
public class InfluxdbReporterConfConstants {
  public static final String INFLUXDB_REPORTER_ENABLE = "influxdb.reporter.enabled";
  public static final String DEFAULT_INFLUXDB_REPORTER_ENABLE = "false";

  public static final String INFLUXDB_URL = "influxdb.url";
  public static final String DEFAULT_INFLUXDB_URL = "http://localhost:8060";

  public static final String INFLUXDB_USERNAME = "influxdb.username";
  public static final String DEFAULT_INFLUXDB_USERNAME = "root";

  public static final String INFLUXDB_PASSWORD = "influxdb.password";
  public static final String DEFAULT_INFLUXDB_PASSWORD = "root";

  public static final String INFLUXDB_CONSISTENCY = "influxdb.consistency";
  public static final String DEFAULT_INFLUXDB_CONSISTENCY = "all";

  public static final String INFLUXDB_DATABASE = "influxdb.database";
  public static final String DEFAULT_INFLUXDB_DATABASE = "kafka";

  public static final String INFLUXDB_RETENTION_POLICY = "influxdb.retention.policy";
  public static final String DEFAULT_INFLUXDB_RETENTION_POLICY = "autogen";

  public static final String INFLUXDB_REPORT_INTERVAL_SECOND = "influxdb.report.interval.second";
  public static final long DEFAULT_INFLUXDB_REPORT_INTERVAL_SECOND = 60;

  public static final int THREAD_POOL_AWAIT_TIMEOUT = 30;
}
