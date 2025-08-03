#!/bin/bash

# Este script automatiza a compilação com Maven e GraalVM Native Image
# Certifique-se de ter o GraalVM e o Maven instalados e configurados corretamente.

# Verifique se o JAVA_HOME está apontando para uma instalação do GraalVM JDK
#if [[ -z "${JAVA_HOME}" ]]; then
#  echo "Erro: JAVA_HOME não está configurado. Por favor, configure-o para apontar para sua instalação do GraalVM JDK."
#  exit 1
#fi
#
#if [[ ! -f "${JAVA_HOME}/bin/native-image" ]]; then
#  echo "Erro: GraalVM native-image não encontrado em ${JAVA_HOME}/bin/. Certifique-se de que o GraalVM esteja corretamente instalado e o component 'native-image' adicionado (gu install native-image)."
#  exit 1
#fi

echo "Iniciando build do projeto Maven..."
# Limpa, compila e empacota o JAR, e então gera a imagem nativa
# O perfil "native" é ativado para usar o plugin do GraalVM
#mvn clean package -Dnative -Pnative
#
#if [ $? -ne 0 ]; then
#  echo "Build do Maven falhou."
#  exit 1
#fi
#
#echo "Build concluído. Executável nativo gerado em target/rinha-backend-pure-java."
#echo "Para executar: ./target/rinha-backend-pure-java"
#
#
#docker build -t csouzadocker/java-http-server-postgres .

# Exemplo de execução (opcional, remova para o Dockerfile)
# ./target/rinha-backend-pure-java

mvn clean spring-boot:build-image