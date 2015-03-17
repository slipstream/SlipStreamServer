#!/bin/env python

import sys
import base64
import urllib2
import xml.etree.ElementTree as ET

if len(sys.argv) != 3:
    print >> sys.stderr, '\nUsage: %s <username> <password> \n\n<username> and <password> should be credentials of a administrator.\n' % sys.argv[0]
    exit(1)

endpoint = 'http://127.0.0.1:8182'
auth = base64.encodestring('%s:%s' % (sys.argv[1], sys.argv[2])).replace('\n','')
http = urllib2.build_opener(urllib2.HTTPHandler)

users_xml = http.open(urllib2.Request(url=endpoint + '/user?media=xml',
                                      headers={'Authorization': 'Basic %s' % auth})).read()
root = ET.fromstring(users_xml)

users = []
for child in root:
    if 'name' in child.attrib:
        users.append(child.attrib.get('name'))

sql_script = ''
for user in users:
    sql_script += "INSERT INTO UserParameter (name, category, order_, mandatory, readonly, type, value, container_resourceuri, description, instructions, enumvalues) " + \
                  " VALUES ('General.keep-running', 'General', 15, true, false, 5, 'on-success', 'user/%s', 'Keep running after deployment', 'Here you can define if and when SlipStream should leave the application running after performing the deployment. <br/><code>On success</code> is useful for production deployments or long tests. </br><code>On Error</code> might be useful so that resources are consumed only when debugging is needed. <br/><code>Never</code> ensures that SlipStream automatically terminates the application after performing the deployment. <br/>Note: This parameter doesn''t apply to <code>mutable deployment</code> Runs and to <code>build image</code> Runs.', 'aced0005757200135b4c6a6176612e6c616e672e537472696e673badd256e7e91d7b47020000787000000004740006616c776179737400056e6576657274000a6f6e2d737563636573737400086f6e2d6572726f72'); \n\n" % user

with open('013_convert_to_keep_running.sql', 'r+') as f:
    script = f.read()
    script = script.replace('-- PLACEHOLDER FOR 012_edit_save_all_users.py', sql_script);
    script = script.replace('-- COMMIT;', 'COMMIT;')
    f.truncate(0)
    f.flush()
    f.seek(0)
    f.write(script)

