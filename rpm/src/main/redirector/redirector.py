#!/bin/env python

import socket
import SocketServer
import SimpleHTTPServer

my_ip = socket.gethostbyname(socket.gethostname())

redirect_url = 'https://%s/' % my_ip
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

