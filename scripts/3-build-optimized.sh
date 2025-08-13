#!/bin/bash
set -e

export CC=musl-gcc

echo "==> Compilando imagem otimizada com PGO"


native-image \
  --static \
  --libc=musl \
  --no-fallback \
  --pgo=default.iprof \
  --install-exit-handlers \
  --enable-url-protocols=http,https \
  --enable-http \
  --enable-https \
  -march=skylake \
  --report-unsupported-elements-at-runtime \
  -H:CLibraryPath=/usr/local/lib \
  -H:Name=output/app-optimized \
  -cp ../target/rinha2025-java-undertow-1.0-SNAPSHOT-fat.jar \
  br.com.ccs.rinha.RinhaApp





#native-image \
#  --pgo=default.iprof \
#  --no-fallback \
#  --install-exit-handlers \
#  --enable-url-protocols=http,https \
#  --enable-http \
#  --enable-https \
#  --report-unsupported-elements-at-runtime \
#  -H:Name=output/app-optimized \
#  -cp ../target/rinha2025-java-undertow-1.0-SNAPSHOT-fat.jar \
#  br.com.ccs.rinha.RinhaApp
##  --initialize-at-build-time=\
##jdk.internal.net.http.common.DebugLogger,\
##jdk.internal.net.http.common.DebugLogger$LoggerConfig,\
##jdk.internal.net.http.HttpClientFacade,\
##jdk.internal.net.http.common.Utils,\
##com.sun.jmx.mbeanserver.JmxMBeanServer \
