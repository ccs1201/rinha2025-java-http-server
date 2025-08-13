#!/bin/bash
set -e

echo "==> Rodando binário instrumentado"

./output/app-instrumented &
APP_PID=$!

echo "Aguardando 5s para aquecimento..."
sleep 8

echo "==> Executando script de benchmark com K6"
k6 run -e MAX_REQUESTS=550 ~/IdeaProjects/rinha/rinha-de-backend-2025/rinha-test/rinha.js

sleep 15
k6 run -e MAX_REQUESTS=550 ~/IdeaProjects/rinha/rinha-de-backend-2025/rinha-test/rinha.js

echo "==> Encerrando app instrumentado..."
kill -SIGTERM "$APP_PID"
wait "$APP_PID"

echo "==> Perfil gerado: default.iprof"