SQLTOOL_RC_FILENAME=~/sqltool.rc
SQLTOOL_RC_FILE_PRESENT=$(shell [ -f $(SQLTOOL_RC_FILENAME) ] && echo ok || echo missing )

SLIPSTREAM_DB_RUNNING=$(shell [ -f 'slipstreamdb.lck' ] && echo 'yes' || echo 'no' )
SSCLJ_DB_RUNNING=$(shell [ -f 'sscljdb.lck' ] && echo 'yes' || echo 'no' )

SQLTOOL_FILENAME=hsqldb/hsqldb-2.3.2/hsqldb/lib/sqltool.jar
SQLTOOL_FILE_PRESENT=$(shell [ -f $(SQLTOOL_FILENAME) ] && echo ok || echo missing )

SSCLJ_SERVER_PID_FILE=ssclj-server.pid
SSCLJ_SERVER_PID_FILE_PRESENT=$(shell [ -f $(SSCLJ_SERVER_PID_FILE) ] && echo ok || echo missing )

STATIC_CONTENT_LOCATION=$(shell cd ../../SlipStreamUI/clj/resources/static_content/; pwd)

# Defaults config:
# Source: http://stackoverflow.com/a/2826178
LANG?=en
# LANG?=fr
# LANG?=de
MODE?=dev
# MODE?=prod
# THEME?=nuvla
# THEME?=helixnebula

run: start-db start-ssclj-server
	mvn jetty:run-war -Dstatic.content.location="file://${STATIC_CONTENT_LOCATION}" -Dpersistence.unit=hsqldb-schema -Dslipstream.ui.util.dev.mode=$(MODE) -Dslipstream.ui.util.localization.lang-default=$(LANG) -Dslipstream.ui.util.theme.current-theme=$(THEME)

clean-run: stop-db clean-install-ui clean-install run

run-with-mem-db:
	mvn jetty:run-war -Dstatic.content.location="file://${STATIC_CONTENT_LOCATION}" -Dpersistence.unit=hsqldb-mem-schema -Dslipstream.ui.util.dev.mode=$(MODE)

new-ui: install-ui run

new-ui-clean: clean-install-ui run

install:
	(cd ..; mvn install -Dmaven.test.skip=true -DskipTests)

clean-install:
	(cd ..; mvn clean install -Dmaven.test.skip=true -DskipTests)

install-ui:
	(cd ../../SlipStreamUI; mvn install -Dmaven.test.skip=true -DskipTests)

clean-install-ui:
	(cd ../../SlipStreamUI; mvn clean install -Dmaven.test.skip=true -DskipTests)

apply-db-migration:
	@(ls -1 ../rpm/src/main/migrations/; echo; echo "Choose the migration file and execute following command:"; echo "java -jar ${SQLTOOL_FILENAME} --autoCommit --inlineRc=url=jdbc:hsqldb:file:slipstreamdb,user=sa,password= ../rpm/src/main/migrations/__MIGRATION_FILE_TO_APPLY__")

start-db-ui: start-db
	java -cp ${SQLTOOL_FILENAME} org.hsqldb.util.DatabaseManagerSwing -user sa -url jdbc:hsqldb:hsql://localhost/slipstream

SSCLJ_PORT=8201
start-ssclj-server: start-db
ifneq ("$(SSCLJ_SERVER_PID_FILE_PRESENT)","ok")
	java -Dconfig.path=config-hsqldb.edn -Dlogfile.path=dev -cp ../ssclj/jar/src/main/resources:../ssclj/jar/target/SlipStreamCljResources-jar-2.19.2-SNAPSHOT-jar-with-dependencies.jar com.sixsq.slipstream.ssclj.app.main $(SSCLJ_PORT) & echo "$$!" > $(SSCLJ_SERVER_PID_FILE)
else
	$(info SSCLJ server already running with PID $(shell cat $(SSCLJ_SERVER_PID_FILE)) on port $(SSCLJ_PORT))
endif

check-ssclj-server:
	curl -sSLf http://localhost:$(SSCLJ_PORT)/api/cloud-entry-point

stop-ssclj-server:
ifeq ("$(SSCLJ_SERVER_PID_FILE_PRESENT)","ok")
	kill $(shell cat $(SSCLJ_SERVER_PID_FILE)) && rm $(SSCLJ_SERVER_PID_FILE)
	$(info SSCLJ server stopped.)
else
	$(info SSCLJ server was not running. Nothing to stop.)
endif

start-db-ui-ssclj: start-db
	java -cp ${SQLTOOL_FILENAME} org.hsqldb.util.DatabaseManagerSwing -user sa -url jdbc:hsqldb:hsql://localhost/ssclj

start-db: check-sqltool-jar-file
ifeq ("$(SLIPSTREAM_DB_RUNNING)","yes")
	$(info slipstream DB already running. Nothing to start.)
else
	java -cp ${SQLTOOL_FILENAME} org.hsqldb.server.Server --database.0 file:slipstreamdb --dbname.0 slipstream --database.1 file:sscljdb --dbname.1 ssclj &
endif

stop-db: check-sqltool-jar-file
ifeq ("$(SLIPSTREAM_DB_RUNNING)","yes")
	java -jar ${SQLTOOL_FILENAME} --sql 'SHUTDOWN;' slipstream
else
	$(info slipstream DB was not running. Nothing to stop.)
endif
ifeq ("$(SSCLJ_DB_RUNNING)","yes")
	java -jar ${SQLTOOL_FILENAME} --sql 'SHUTDOWN;' ssclj
else
	$(info ssclj DB was not running. Nothing to stop.)
endif

check-db-running: check-sqltool-jar-file
ifneq ("$(SLIPSTREAM_DB_RUNNING)","yes")
	$(error Start DB with 'make start-db')
endif
ifneq ("$(SSCLJ_DB_RUNNING)","yes")
	$(error Start DB with 'make start-db')
endif

check-sqltool-jar-file: check-sqltool-rc-file
ifneq ("$(SQLTOOL_FILE_PRESENT)","ok")
	$(info sqltool.jar is not found at ${SQLTOOL_FILENAME})
	$(info hsqldb tools can be istalled following the instruction here:)
	$(info   - http://hsqldb.org/doc/guide/ch03.html)
	$(info or directly executing following lines in the SlipStreamServer/war directory:)
	$(info )
	$(info (mkdir hsqldb && cd hsqldb && wget http://downloads.sourceforge.net/project/hsqldb/hsqldb/hsqldb_2_3/hsqldb-2.3.2.zip && unzip hsqldb-2.3.2.zip;))
	$(info )
	$(error File ${SQLTOOL_FILENAME} missing...)
endif

check-sqltool-rc-file:
ifneq ("$(SQLTOOL_RC_FILE_PRESENT)","ok")
	$(info To create the ${SQLTOOL_RC_FILENAME} execute following lines:)
	$(info )
	$(info cat << EOF > ${SQLTOOL_RC_FILENAME})
	$(info urlid slipstream)
	$(info url jdbc:hsqldb:hsql://localhost/slipstream)
	$(info username SA)
	$(info password)
	$(info )
	$(info urlid ssclj)
	$(info url jdbc:hsqldb:hsql://localhost/ssclj)
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

remove-db: stop-db backup-db
	if [ -f slipstreamdb.script ];     then rm -v slipstreamdb.script;     fi;
	if [ -f slipstreamdb.properties ]; then rm -v slipstreamdb.properties; fi;

fresh-test-db: remove-db
	curl -sSLO https://gist.github.com/rbf/fcfecf39587f2688dcd6/raw/slipstreamdb.script
	curl -sSLO https://gist.github.com/rbf/fcfecf39587f2688dcd6/raw/slipstreamdb.properties

patch-test-pom:
	curl -sSLO https://gist.github.com/rbf/fcfecf39587f2688dcd6/raw/set_up_connectors_in_server_pom.patch
	git apply set_up_connectors_in_server_pom.patch
	$(info This patch modified the war/pom.xml to use all public connectors (but Okeanos) and the private EC2 connector.)
	$(info To download the code for this last connector and build it perform following commands:)
	$(info (cd ../../ && git clone git@github.com:SixSq/SlipStreamConnector-EC2.git && cd SlipStreamConnector-EC2 && mvn clean install))

list: goals

goals:
	@cat Makefile | egrep "^[[:alpha:]-]+:"

