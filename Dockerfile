FROM ubuntu:jammy

WORKDIR /app

# Copia o binário estático
COPY target/rinha2025-java-http-server /app/rinha

# Permissão de execução
RUN chmod +x /app/rinha

# Exponha a porta
EXPOSE 8080

# Entrypoint para execução
ENTRYPOINT ["/app/rinha"]
