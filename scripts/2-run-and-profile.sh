#!/bin/bash
set -e

export DATASOURCE_CLASS_NAME=org.postgresql.Driver
export DATASOURCE_MAXIMUM_POOL_SIZE=3
export DATASOURCE_MINIMUM_IDLE=3
export DATASOURCE_PASSWORD=rinha
export DATASOURCE_TIMEOUT=2000
export DATASOURCE_URL=jdbc:postgresql://localhost:5432/rinha
export DATASOURCE_USERNAME=rinha
export PAYMENT_PROCESSOR_DEFAULT_URL=http://localhost:8001
export PAYMENT_PROCESSOR_FALLBACK_URL=http://localhost:8002
export PAYMENT_PROCESSOR_WORKERS=10
export PAYMENT_PROCESSOR_REQUEST_TIMEOUT=6000
export PAYMENT_PROCESSOR_CONNECTION_TIMEOUT=50
export PAYMENT_QUEUE=1000
export SERVER_PORT=9999
export THREAD_POOL_SIZE=4
export THREAD_QUEUE_SIZE=500
export BATCH_SIZE=100
export BATCH_LIMIT=10

echo "==> Rodando binário instrumentado"

./output/app-instrumented &
APP_PID=$!

echo "Aguardando 5s para aquecimento..."
sleep 8

echo "==> Executando script de benchmark com K6"
k6 run -e MAX_REQUESTS=550 ~/IdeaProjects/rinha/rinha-de-backend-2025/rinha-test/rinha.js

sleep 30
k6 run -e MAX_REQUESTS=550 ~/IdeaProjects/rinha/rinha-de-backend-2025/rinha-test/rinha.js

echo "==> Encerrando app instrumentado..."
kill -SIGTERM "$APP_PID"
wait "$APP_PID"

echo "==> Perfil gerado: default.iprof"