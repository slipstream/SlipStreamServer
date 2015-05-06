SQLTOOL_RC_FILENAME=~/sqltool.rc
SQLTOOL_RC_FILE_PRESENT=$(shell [ -f $(SQLTOOL_RC_FILENAME) ] && echo ok || echo missing )

SLIPSTREAM_DB_RUNNING=$(shell [ -f 'slipstreamdb.lck' ] && echo 'yes' || echo 'no' )

SQLTOOL_FILENAME=hsqldb/hsqldb-2.3.2/hsqldb/lib/sqltool.jar
SQLTOOL_FILE_PRESENT=$(shell [ -f $(SQLTOOL_FILENAME) ] && echo ok || echo missing )

STATIC_CONTENT_LOCATION=$(shell cd ../../SlipStreamUI/clj/resources/static_content/; pwd)

run: stop-db start-db re-run-with-db

run-fr: stop-db start-db re-run-with-db-fr

run-hnx: stop-db start-db re-run-with-db-hnx

run-prod: stop-db start-db re-run-with-db-prod

re-run-with-db-prod:
	mvn jetty:run-war -Dstatic.content.location=file://${STATIC_CONTENT_LOCATION} -Dpersistence.unit=hsqldb-schema

re-run-with-db:
	mvn jetty:run-war -Dstatic.content.location=file://${STATIC_CONTENT_LOCATION} -Dpersistence.unit=hsqldb-schema -Dslipstream.ui.util.dev.mode=dev

re-run-with-db-fr:
	mvn jetty:run-war -Dstatic.content.location=file://${STATIC_CONTENT_LOCATION} -Dpersistence.unit=hsqldb-schema -Dslipstream.ui.util.dev.mode=dev -Dslipstream.ui.util.localization.lang-default=fr

re-run-with-db-hnx:
	mvn jetty:run-war -Dstatic.content.location=file://${STATIC_CONTENT_LOCATION} -Dpersistence.unit=hsqldb-schema -Dslipstream.ui.util.dev.mode=dev -Dslipstream.ui.util.theme.current-theme=helixnebula

run-with-mem-db:
	mvn jetty:run-war -Dstatic.content.location=file://${STATIC_CONTENT_LOCATION} -Dpersistence.unit=hsqldb-mem-schema -Dslipstream.ui.util.dev.mode=dev

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

apply-db-migration:
	@(ls -1 ../rpm/src/main/migrations/; echo; echo "Choose the migration file and execute following command:"; echo "java -jar ${SQLTOOL_FILENAME} --autoCommit --inlineRc=url=jdbc:hsqldb:file:slipstreamdb,user=sa,password= ../rpm/src/main/migrations/__MIGRATION_FILE_TO_APPLY__")

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
	$(info or directly executing following lines in the SlipStreamServer/war directory:)
	$(info )
	$(info (mkdir hsqldb && cd hsqldb && wget http://downloads.sourceforge.net/project/hsqldb/hsqldb/hsqldb_2_3/hsqldb-2.3.2.zip && unzip hsqldb-2.3.2.zip;))
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

BACKUP_DIR="db_backup_$(shell date +"%Y%m%d%H%M")"
backup-db:
	mkdir -p $(BACKUP_DIR)
	if [ -f slipstreamdb.script ];     then cp -v slipstreamdb.script      $(BACKUP_DIR); fi;
	if [ -f slipstreamdb.properties ]; then cp -v slipstreamdb.properties  $(BACKUP_DIR); fi;

remove-db: backup-db
	if [ -f slipstreamdb.script ];     then rm -v slipstreamdb.script;     fi;
	if [ -f slipstreamdb.properties ]; then rm -v slipstreamdb.properties; fi;

fresh-test-db: remove-db
	curl -sSLO https://gist.github.com/rbf/fcfecf39587f2688dcd6/raw/slipstreamdb.script
	curl -sSLO https://gist.github.com/rbf/fcfecf39587f2688dcd6/raw/slipstreamdb.properties

patch-test-pom: fresh-test-db
	curl -sSLO https://gist.github.com/rbf/fcfecf39587f2688dcd6/raw/set_up_connectors_in_server_pom.patch
	git apply set_up_connectors_in_server_pom.patch
	$(info This patch modified the war/pom.xml to use all public connectors (but Okeanos) and the private EC2 connector.)
	$(info To download the code for this last connector and build it perform following commands:)
	$(info cd ../../ && git clone git@github.com:SixSq/SlipStreamConnector-EC2.git && cd SlipStreamConnector-EC2 && mvn clean install)

list: goals

goals:
	@cat Makefile | egrep "^[[:alpha:]-]+:"

