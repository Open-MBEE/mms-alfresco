::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::      Dev environment startup script for Alfresco Community     ::
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
@echo off

set MAVEN_OPTS=-noverify -Xms256m -Xmx8G -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n

mvnw.cmd install -Ddependency.surf.version=6.3 -Prun -nsu
:: mvnw.cmd install -Ddependency.surf.version=6.3 -Prun
