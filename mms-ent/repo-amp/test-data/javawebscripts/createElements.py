import sys

msg = '{ "elements" : ['

start = 100
if len(sys.argv) == 2:
   end = start + int(sys.argv[1])
else:
   end = start + 100

make_flat = False
for uid in range(start, end):
   if make_flat:
      owner  = 'null'
   else:
      owner = uid - 1
      if owner < start:
          owner = 'null'
   
   msg += '{"documentation" : "hello", "id":"' + str(uid) + '", "isDerived":false, "name":"", "owner"="' + str(owner) + '", "type": "Property", "value" : ["dlam"], "valueType" : "LiteralString"},\n'

msg += ']}'
print msg
