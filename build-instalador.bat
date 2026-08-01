@echo off
REM Gera o instalador .exe do Painel iFood usando jpackage (JDK 14+).
REM Rode este arquivo com duplo clique ou "build-instalador.bat" no terminal,
REM sempre a partir da raiz do projeto (onde este .bat esta salvo).

echo === 1/2: Compilando o projeto (mvn package) ===
call "%~dp0mvnw.cmd" clean package -DskipTests
if errorlevel 1 (
    echo Falhou o "mvn package". Corrija os erros acima antes de continuar.
    pause
    exit /b 1
)

echo === 2/3: Preparando pasta limpa so com o jar final ===
if exist dist-jar rmdir /s /q dist-jar
mkdir dist-jar
copy target\ifood-merchant-api-0.0.1-SNAPSHOT.jar dist-jar\ >nul

echo === 3/3: Gerando app pronta para uso com jpackage ===
if exist instalador rmdir /s /q instalador
jpackage ^
  --input dist-jar ^
  --name "Painel iFood" ^
  --main-jar ifood-merchant-api-0.0.1-SNAPSHOT.jar ^
  --main-class org.springframework.boot.loader.launch.JarLauncher ^
  --type app-image ^
  --win-console ^
  --dest instalador

if errorlevel 1 (
    echo Falhou o jpackage. Veja a mensagem acima.
    pause
    exit /b 1
)

copy "%~dp0LEIA-ME.txt" "instalador\Painel iFood\LEIA-ME.txt" >nul

echo.
echo Pronto! A pasta pronta para o cliente esta em .\instalador\Painel iFood\
echo Copie essa pasta inteira (exe + runtime) para o computador do cliente.
pause
