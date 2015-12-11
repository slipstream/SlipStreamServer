#!/usr/bin/env python3

import sys
import hashlib

def get_hashed_password(password):
    md = hashlib.new('sha512')
    md.update(password)
    return md.hexdigest().upper()

if __name__ == "__main__":
    print(get_hashed_password(sys.argv[1].encode('utf-8')))

