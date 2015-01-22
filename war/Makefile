SQLTOOL_RC_FILENAME=~/sqltool.rc
SQLTOOL_RC_FILE_PRESENT=$(shell [ -f $(SQLTOOL_RC_FILENAME) ] && echo ok || echo missing )

SLIPSTREAM_DB_RUNNING=$(shell [ -f 'slipstreamdb.lck' ] && echo 'yes' || echo 'no' )

SQLTOOL_FILENAME=hsqldb/hsqldb-2.3.2/hsqldb/lib/sqltool.jar
SQLTOOL_FILE_PRESENT=$(shell [ -f $(SQLTOOL_FILENAME) ] && echo ok || echo missing )

run: stop-db start-db re-run-with-db

run-hnx: stop-db start-db re-run-with-db-hnx

re-run-with-db:
	mvn jetty:run-war -Dstatic.content.location=file:///Users/sixsq_rob/Dropbox/dev/projects/2014/SlipStream/SlipStreamUI/clj/resources/static_content -Dpersistence.unit=hsqldb-schema

re-run-with-db-hnx:
	mvn jetty:run-war -Dstatic.content.location=file:///Users/sixsq_rob/Dropbox/dev/projects/2014/SlipStream/SlipStreamUI/clj/resources/static_content -Dpersistence.unit=hsqldb-schema -Dslipstream.ui.util.theme.current-theme=helixnebula

run-with-mem-db:
	mvn jetty:run-war -Dstatic.content.location=file:///Users/sixsq_rob/Dropbox/dev/projects/2014/SlipStream/SlipStreamUI/clj/src/slipstream/ui/views/ -Dpersistence.unit=hsqldb-mem-schema

new-ui: install-ui run

new-ui-fast: install-ui-fast run

new-ui-hnx: install-ui run-hnx

new-ui-fast-hnx: install-ui-fast run-hnx

install:
	(cd ..; mvn clean install -Dmaven.test.skip=true)

install-ui:
	(cd ../../SlipStreamUI; mvn clean install -Dmaven.test.skip=true)

install-ui-fast:
	(cd ../../SlipStreamUI; mvn install -Dmaven.test.skip=true)

start-db-ui: start-db
	java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.2/hsqldb-2.3.2.jar org.hsqldb.util.DatabaseManagerSwing -user sa -url jdbc:hsqldb:hsql://localhost/slipstream

start-db: check-sql-tool-file
	java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.2/hsqldb-2.3.2.jar org.hsqldb.server.Server --database.0 file:slipstreamdb --dbname.0 slipstream &

stop-db:
ifeq ("$(SQLTOOL_FILE_PRESENT)","ok")
ifeq ("$(SLIPSTREAM_DB_RUNNING)","yes")
	java -jar ${SQLTOOL_FILENAME} --sql 'SHUTDOWN;' slipstream
else
	$(info DB was not running. Nothing to stop.)
endif
else
	$(info sqltool.jar is not found at ${SQLTOOL_FILENAME})
	$(info hsqldb tools can be istalled following the instruction here:)
	$(info   - http://hsqldb.org/doc/guide/ch03.html)
	$(info )
	$(error File ${SQLTOOL_FILENAME} missing...)
endif

check-sql-tool-file:
ifneq ("$(SQLTOOL_RC_FILE_PRESENT)","ok")
	$(info To create the ${SQLTOOL_RC_FILENAME} execute following lines:)
	$(info )
	$(info cat << EOF > ${SQLTOOL_RC_FILENAME})
	$(info urlid slipstream)
	$(info url jdbc:hsqldb:hsql://localhost/slipstream)
	$(info username SA)
	$(info password)
	$(info EOF)
	$(info )
	$(error File $(DB_SQLITE_FILENAME) missing...)
endif

list: goals

goals:
	@cat Makefile | egrep "^[[:alpha:]-]+:"

