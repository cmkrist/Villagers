import importlib.util,struct,zlib,pathlib,json,fcntl
spec=importlib.util.spec_from_file_location('nbt','tools/enable-adventure-dimensions.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def plain(n):
 t,v=n
 if t==10:return {k.decode():plain(x) for k,x in v}
 if t==9:return [plain(x) for x in v[1]]
 if t==8:return v.decode()
 if t in (1,2,3,4,5,6):return struct.unpack('>'+{1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t],v)[0]
 count=struct.unpack('>i',v[:4])[0]
 return list(struct.unpack('>'+{11:'i',12:'q'}[t]*count,v[4:])) if t!=7 else list(v[4:])
w=pathlib.Path('runs/native-camp-door-and-self-escape-v19/NativeFourRegions')
with (w/'session.lock').open('rb') as lock:
 fcntl.lockf(lock,fcntl.LOCK_SH|fcntl.LOCK_NB)
 p=next(w.glob('dimensions/minecraft/overworld/region/r.-2.-2.mca'));b=p.read_bytes();cx,cz=-40,-39;i=(cx%32)+(cz%32)*32;o=int.from_bytes(b[i*4:i*4+4],'big')>>8;s=o*4096;l=int.from_bytes(b[s:s+4],'big');chunk=plain(m.read_nbt(zlib.decompress(b[s+5:s+4+l]))[1]);secs={x.get('Y',x.get('y')):x for x in chunk.get('sections',chunk.get('Sections',[]))}
 sec=secs[4];bs=sec['block_states'];palette=bs['palette'];data=bs.get('data');bits=max(4,(len(palette)-1).bit_length());per=64//bits
 for y in range(64,69):
  for z in range(-614,-610):
   row=[]
   for x in range(-634,-628):
    k=(y%16)*256+(z%16)*16+(x%16);v=0 if data is None else ((data[k//per]&((1<<64)-1))>>((k%per)*bits))&((1<<bits)-1);row.append([x,palette[v]])
   print(y,z,row)
