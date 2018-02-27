#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
 SlipStream Client
 =====
 Copyright (C) 2014 SixSq Sarl (sixsq.com)
 =====
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""
from __future__ import print_function

import os
import sys
from glob import glob
import time
from slipstream.api import Api
from requests import put
import logging

try:
    import jaydebeapi
except ImportError:
    print("Please install following dependency: yum install -y gcc-c++; pip install jaydebeapi")
    exit(-1)

from slipstream.command.CommandBase import CommandBase

logging.getLogger("urllib3").setLevel(logging.ERROR)

SEPARATOR = '\n=====================================================================\n'


class MainProgram(CommandBase):
    """A command-line program to migrate reports for SS v3.45->3.46."""

    def __init__(self, argv=None):
        self.api = Api('https://localhost', insecure=True)
        self.conn = jaydebeapi.connect("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://localhost/slipstream", ["SA", ""],
                                       "/opt/slipstream/cimi/lib/hsqldb-2.3.4.jar")
        super(MainProgram, self).__init__(argv)

    def parse(self):
        usage = '''usage: %prog [options].
                   This script should be run directly on slipstream server host.
                   Please authenticate with ss-login before executing this script with endpoint=https://localhost'.
                '''

        self.parser.usage = usage
        self.parser.add_option('--months', dest='months',
                               help='Number of months in past from modification time of a report. (default 12 months)',
                               default=12, type=int)

        self.options, self.args = self.parser.parse_args()

    @staticmethod
    def get_all_existing_reports():
        return [y for x in os.walk('/var/tmp/slipstream/reports') for y in glob(os.path.join(x[0], '*.tgz'))]

    @staticmethod
    def filter_reports_updated_since_less_than(reports, months):
        now = int(time.time())
        months_to_seconds = int(months * 2628002.88)
        after_time = now - months_to_seconds
        return [report for report in reports if os.path.getmtime(report) > after_time]

    def create_external_object_report(self, report):
        report_path_split = str.split(report, '/')
        uuid = report_path_split[5]
        report_name = report_path_split[6]
        node_name = str.split(report_name, '_')[0]
        self.db = self.conn.cursor()
        self.db.execute("select USER_ from RUN where UUID='{}'".format(uuid))
        db_res = self.db.fetchone()
        if db_res is None:
            raise Exception('Warning: owner not found for following report: {}'.format(uuid))
        else:
            owner = db_res[0]
        report_object = {'externalObjectTemplate': {'href': 'external-object-template/report',
                                                    'runUUID': uuid,
                                                    'component': node_name,
                                                    'name': report_name,
                                                    'acl': {
                                                        'owner': {
                                                            'principal': owner,
                                                            'type': 'USER'
                                                        },
                                                        'rules': [{
                                                            'principal': owner,
                                                            'right': 'MODIFY',
                                                            'type': 'USER'
                                                        }, {
                                                            'principal': 'ADMIN',
                                                            'right': 'ALL',
                                                            'type': 'ROLE'
                                                        }]
                                                    }}}
        resp = self.api.cimi_add('externalObjects', report_object)
        return resp.json['resource-id']

    def generate_upload_url_external_object_report(self, resource_id):
        resp = self.api.cimi_operation(resource_id, "http://sixsq.com/slipstream/1/action/upload")
        return resp.json['uri']

    def upload_report(self, url, report):
        report_file_data = open(report, 'rb').read()
        response = put(url, data=report_file_data)
        response.raise_for_status()

    def migrate_report(self, report):
        print('Migrating {}'.format(report))
        resource_id = self.create_external_object_report(report)
        upload_url = self.generate_upload_url_external_object_report(resource_id)
        self.upload_report(upload_url, report)

    def doWork(self):
        print(SEPARATOR + 'Starting migration of reports...' + SEPARATOR)
        all_reports = self.get_all_existing_reports()
        print('All reports count: {}'.format(len(all_reports)))
        reports_to_migrate = self.filter_reports_updated_since_less_than(all_reports, self.options.months)
        print('Number of reports updated in last {} months: {}'.format(self.options.months, len(reports_to_migrate))
              + SEPARATOR)

        success = 0
        for report in reports_to_migrate:
            try:
                self.migrate_report(report)
                success += 1
            except Exception as e:
                print("Failed to migrate this report: {} with error => {}".format(report, e.message))
        print(SEPARATOR +
              "Migration completed, {}/{} reports successfully migrated!".format(success, len(reports_to_migrate)))
        exit(0)


if __name__ == "__main__":
    try:
        MainProgram()
    except KeyboardInterrupt:
        print('\n\nExecution interrupted by the user... goodbye!')
        sys.exit(-1)
