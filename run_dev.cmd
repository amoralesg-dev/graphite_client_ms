@echo off
set "GRAPHITE_API_BASE_URL=https://poc.projectgraphite.com/api/public/api/v1"
set "GRAPHITE_AUTH_URL=https://poc.projectgraphite.com/api/oauth/token"
set "GRAPHITE_CLIENT_ID=MX5402.7ea0382381cf43790999bf77"
set "GRAPHITE_CLIENT_SECRET=1b5172ab7dfd4ab3ac063ab5e28cb4ed2be73ed5d1a615dbcf6db49cf4b9b294"
set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_NAME=pagos_db"
set "DB_USER=user"
set "DB_PASSWORD=user123"
set "EMAIL_USERNAME=botrss@rassini.com"
set "EMAIL_PASSWORD=M4ng3rR4ssini2"
set "EMAIL_TO=developerrss@rassini.com"

cd /d C:\workspace\graphite_client_ms
"C:\Program Files\Amazon Corretto\jdk21.0.10_7\bin\java.exe" "-Dspring.profiles.active=dev" "-Djavax.net.ssl.trustStore=C:\Program Files\Amazon Corretto\jdk21.0.10_7\lib\security\cacerts" -Djavax.net.ssl.trustStorePassword=changeit -jar target\graphite-client-0.0.1-SNAPSHOT.jar
