SQLTOOL_FILENAME=~/sqltool.rc
SQLTOOL_FILE_PRESENT=$(shell [ -f $(SQLTOOL_FILENAME) ] && echo ok || echo missing )

run-with-db: start-db re-run-with-db

re-run-with-db:
	mvn jetty:run-war -Dstatic.content.location=file:///Users/sixsq_rob/Dropbox/dev/projects/2014/SlipStream/SlipStreamUI/clj/src/slipstream/ui/views/ -Dpersistence.unit=hsqldb-schema

run:
	mvn jetty:run-war -Dstatic.content.location=file:///Users/sixsq_rob/Dropbox/dev/projects/2014/SlipStream/SlipStreamUI/clj/src/slipstream/ui/views/ -Dpersistence.unit=hsqldb-mem-schema

start-db-ui: start-db
	java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.2/hsqldb-2.3.2.jar org.hsqldb.util.DatabaseManagerSwing -user sa -url jdbc:hsqldb:hsql://localhost/slipstream

start-db: check-sql-tool-file
	java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.2/hsqldb-2.3.2.jar org.hsqldb.server.Server --database.0 file:slipstreamdb --dbname.0 slipstream &

check-sql-tool-file:
ifneq ("$(SQLTOOL_FILE_PRESENT)","ok")
	$(info To create the ${SQLTOOL_FILENAME} execute following lines:)
	$(info )
	$(info cat << EOF > ${SQLTOOL_FILENAME})
	$(info urlid slipstream)
	$(info url jdbc:hsqldb:hsql://localhost/slipstream)
	$(info username SA)
	$(info password)
	$(info EOF)
	$(info )
	$(error File $(DB_SQLITE_FILENAME) missing...)
endif

