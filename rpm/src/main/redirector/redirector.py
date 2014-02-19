#!/bin/env python

import re
import socket
import SocketServer
import SimpleHTTPServer

def find_ip(url = None):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('google.com',80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        try:
            return socket.gethostbyname(socket.gethostname())
        except:
            return re.findall('^(?:https?://)?([0-9]{1,3}(?:\\.[0-9]{1,3}){3})', url)[0]
            

def find_redirect_url():
    try:
        with open('/etc/slipstream/slipstream.conf', 'r') as f:
            s = f.read()
        return re.findall('[ \\t]*slipstream\\.base\\.url[ \\t]*=[ \\t]*([^\\n\\r]+)', s)[0]
    except:
        return 'https://%s/' % find_ip()

redirect_url = find_redirect_url()
my_ip = 0.0.0.0

host = my_ip
port = 80

class redirectHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
   
    server_version = 'SlipStream Redirector'
    sys_version = ''

    def do_GET(self):
        self.send_response(303)
        self.send_header('Location', redirect_url)
        self.send_header('Connection', 'close')
        self.end_headers()
    
    def do_HEAD(self):
        self.send_response(405)
        self.send_header('Connection', 'close')
        self.end_headers()
    

redirect_handler = redirectHandler
redirect_server = SocketServer.TCPServer((host, port), redirect_handler)

print 'Python HTTP Redirector'
print 'redirect from %s:%d to %s' % (host, port, redirect_url)

redirect_server.serve_forever()

