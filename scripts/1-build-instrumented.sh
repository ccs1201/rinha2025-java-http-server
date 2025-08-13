#!/bin/bash
set -e

cd ..
mvn clean package -DskipTests

cd scripts

echo "==> Build da imagem instrumentada com PGO"

native-image \
  --pgo-instrument \
  --no-fallback \
  --install-exit-handlers \
  --enable-url-protocols=http,https \
  --enable-http \
  --enable-https \
  -march=x86-64-v3 \
  -H:+UnlockExperimentalVMOptions \
  -H:Name=output/app-instrumented \
  -cp ../target/rinha2025-java-http-server-1.0-SNAPSHOT-fat.jar \
  br.com.ccs.rinha.RinhaApp
#  --initialize-at-build-time=br.com.ccs \
#  --initialize-at-run-time=org.postgresql.jdbc.PgStatement,\
#jdk.internal.net.http.HttpClientFacade,\
#org.postgresql.util.SharedTimer,\
#jdk.internal.net.http.common.Utils,\
#com.zaxxer.hikari.util.ConcurrentBag,\
#jdk.internal.net.http.common.DebugLogger,\
#io.undertow.util.HttpString,\
#com.zaxxer.hikari.pool.PoolBase,\
#org.postgresql.Driver,\
#jdk.internal.net.http.common.DebugLogger$LoggerConfig,\
#java.sql.DriverManager,\
#com.zaxxer.hikari.HikariDataSource,\
#org.postgresql.core.QueryExecutorCloseAction,\
#com.zaxxer.hikari.pool.PoolEntry,\
#org.slf4j.LoggerFactory,\
#com.zaxxer.hikari.HikariConfig \

#./2-run-and-profile.sh
#
#sleep 2
#
#./3-build-optimized.sh
#
#cd ..
#
#image-build.sh