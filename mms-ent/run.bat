::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::      Dev environment startup script for Alfresco Community     ::
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
@echo off

set MAVEN_OPTS=-noverify -Xms256m -Xmx8G -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n

mvn install -Prun -nsu
:: mvn install -Prun
