::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::      Dev environment startup script for Alfresco Community     ::
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
@echo off

set MAVEN_OPTS=-noverify -Xms256m -Xmx2G

mvn install -Prun -nsu
:: mvn install -Prun 
