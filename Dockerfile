# Usa a imagem base do GraalVM para compilação
# Você pode escolher uma versão específica, por exemplo, `ghcr.io/graalvm/jdk:21`
FROM ghcr.io/graalvm/jdk:21-ol8 as graalvm-builder

WORKDIR /app

# Copia o código-fonte da aplicação
COPY . .

# Compila a aplicação Java e gera o executável nativo
# Instala o Maven se não estiver presente na imagem base (muitas imagens GraalVM já o incluem)
RUN apt-get update && apt-get install -y maven \
    && mvn clean package -Dnative -Pnative

# Usa uma imagem base muito leve para a aplicação final
# `scratch` é a imagem mais leve possível, contendo apenas o executável
FROM scratch

# Define as variáveis de ambiente para o banco de dados e porta
ENV DB_URL="jdbc:postgresql://host.docker.internal:5432/rinha" \
    DB_USER="admin" \
    DB_PASSWORD="123" \
    SERVER_PORT="8080" \
    DB_MAX_POOL_SIZE="10" \
    DB_MIN_IDLE_POOL_SIZE="10"

# Copia o executável nativo do estágio de build
COPY --from=graalvm-builder /app/target/rinha-backend-pure-java /usr/local/bin/rinha-backend

# Expõe a porta que o servidor vai ouvir
EXPOSE 8080

# Comando para executar a aplicação nativa
CMD ["/usr/local/bin/rinha-backend"]