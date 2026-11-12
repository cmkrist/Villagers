exec(open('/tmp/villagers-read-guard-blocks.py').read().split("w=pathlib.Path(")[0])
w=pathlib.Path('runs/native-earned-ore-expedition-v31/NativeFourRegions');cache={}
def block(x,y,z):
 cx,cz=x//16,z//16
 if (cx,cz) not in cache:
  p=w/f'dimensions/minecraft/overworld/region/r.{cx//32}.{cz//32}.mca'
  if not p.exists():return 'unloaded'
  b=p.read_bytes();i=cx%32+(cz%32)*32;o=int.from_bytes(b[i*4:i*4+4],'big')>>8
  if not o:return 'unloaded'
  t=o*4096;l=int.from_bytes(b[t:t+4],'big');ch=plain(m.read_nbt(zlib.decompress(b[t+5:t+4+l]))[1]);cache[cx,cz]={s.get('Y',s.get('y')):s for s in ch.get('sections',[])}
 sec=cache[cx,cz].get(y//16)
 if sec is None:return 'air'
 bs=sec['block_states'];pal=bs['palette'];data=bs.get('data');bits=max(4,(len(pal)-1).bit_length());per=64//bits;k=y%16*256+z%16*16+x%16;i=0 if data is None else (data[k//per]>>((k%per)*bits))&((1<<bits)-1);p=pal[i];return p if isinstance(p,str) else p.get('',p.get('id',p.get('Name','?')))
with (w/'session.lock').open('rb') as lock:
 fcntl.lockf(lock,fcntl.LOCK_SH|fcntl.LOCK_NB)
 for x,z in [(-470,-380),(-480,-379),(-494,-304),(-450,-400),(-510,-350)]:
  rows=[];last=None
  for y in range(-64,81):
   b=block(x,y,z)
   if b==last:rows[-1]['to']=y
   else:rows.append({'from':y,'to':y,'block':b});last=b
  print(json.dumps({'x':x,'z':z,'column':rows}))
