import os,socket,struct,sys,time
def pkt(i,t,b): d=struct.pack('<ii',i,t)+b.encode()+b'\x00\x00'; return struct.pack('<i',len(d))+d
s=socket.create_connection(('127.0.0.1',26262)); s.sendall(pkt(1,3,os.environ.get('RIG_RCON_PASSWORD',''))); s.recv(4096)
for c in sys.argv[1:]:
    s.sendall(pkt(2,2,c)); time.sleep(0.5); r=s.recv(65536); print(c,'=>',r[12:-2].decode(errors='replace'))
