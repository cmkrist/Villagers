from pathlib import Path
import struct,zlib,json,sys,hashlib,collections
ROOT=Path('/home/niko/.internal/source/Villagers');RUN=ROOT/'runs/play';WORLD=RUN/'saves/VillageTrial';OUT=ROOT/'docs/evidence/2026-11-07/farm-capacity'
sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
MAX=4*1024*1024
class NBT:
 def __init__(self,b):self.b=b;self.i=0;self.nodes=0
 def take(self,n):
  if n<0 or self.i+n>len(self.b):raise ValueError('truncated/invalid NBT length')
  r=self.b[self.i:self.i+n];self.i+=n;return r
 def num(self,f):return struct.unpack('>'+f,self.take(struct.calcsize('>'+f)))[0]
 def string(self):return self.take(self.num('H')).replace(b'\xc0\x80',b'\0').decode('utf-8','surrogatepass')
 def count(self):
  n=self.num('i')
  if n<0 or n>1_048_576:raise ValueError('NBT collection exceeds bounds')
  return n
 def tag(self,t,depth=0):
  self.nodes+=1
  if depth>64 or self.nodes>1_048_576:raise ValueError('NBT nesting/node limit')
  if t in {1,2,3,4,5,6}:return self.num({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
  if t==7:return self.take(self.count())
  if t==8:return self.string()
  if t==9:
   kind=self.num('B');n=self.count()
   if kind==0 and n:raise ValueError('nonempty TAG_End list')
   return [self.tag(kind,depth+1) for _ in range(n)]
  if t==10:
   result={}
   while True:
    kind=self.num('B')
    if kind==0:return result
    name=self.string()
    if name in result:raise ValueError('duplicate NBT key')
    result[name]=self.tag(kind,depth+1)
  if t in (11,12):
   n=self.count();f='i' if t==11 else 'q';size=4 if t==11 else 8
   return list(struct.unpack('>'+str(n)+f,self.take(n*size)))
  raise ValueError('unsupported NBT tag '+str(t))
 def root(self):
  if self.num('B')!=10:raise ValueError('expected compound NBT root')
  self.string();r=self.tag(10)
  if self.i!=len(self.b):raise ValueError('trailing NBT data')
  return r
class Reader:
 def __init__(self):self.chunks={};self.identities=[]
 def chunk(self,cx,cz):
  if (cx,cz) in self.chunks:return self.chunks[cx,cz]
  file=WORLD/'dimensions/minecraft/overworld/region'/f'r.{cx//32}.{cz//32}.mca'
  with file.open('rb') as f:
   f.seek(4*((cx&31)+(cz&31)*32));entry=f.read(4)
   if len(entry)!=4:raise ValueError('truncated region index')
   offset=int.from_bytes(entry[:3],'big');sectors=entry[3]
   if not offset or not sectors:raise ValueError(f'unrecorded chunk {cx},{cz}')
   f.seek(offset*4096);size=struct.unpack('>I',f.read(4))[0];codec=f.read(1)[0]
   if codec&128:raise ValueError('external chunk record not supported')
   if size<1 or size>sectors*4096-4 or size>1_048_576:raise ValueError('invalid compressed chunk bound')
   packed=f.read(size-1)
  if len(packed)!=size-1:raise ValueError('truncated chunk')
  if codec in (1,2):
   d=zlib.decompressobj(31 if codec==1 else 15);raw=d.decompress(packed,MAX+1)
   if len(raw)>MAX or not d.eof:raise ValueError('oversized or truncated compressed chunk')
  elif codec==3:
   raw=packed
   if len(raw)>MAX:raise ValueError('oversized raw chunk')
  else:raise ValueError('unsupported compression '+str(codec))
  j=NBT(raw).root()
  if j.get('xPos')!=cx or j.get('zPos')!=cz:raise ValueError('chunk coordinate mismatch')
  sections={s['Y']:s for s in j['sections']};self.chunks[cx,cz]=sections
  self.identities.append({'path':str(file.relative_to(ROOT)),'chunk':[cx,cz],'codec':codec,'nbtBytes':len(raw),'nbtSHA256':hashlib.sha256(raw).hexdigest(),'dataVersion':j.get('DataVersion')})
  (OUT/f'chunk-{cx}-{cz}.nbt').write_bytes(raw)
  return sections
 def block(self,x,y,z):
  section=self.chunk(x//16,z//16).get(y//16)
  if section is None:raise ValueError('missing vertical section')
  states=section['block_states'];palette=states['palette']
  if len(palette)==1:return self.normal(palette[0])
  bits=max(4,(len(palette)-1).bit_length());per=64//bits;data=states['data']
  if len(data)!=(4096+per-1)//per:raise ValueError('unsupported packed block-state length')
  idx=((y&15)<<8)|((z&15)<<4)|(x&15);word=data[idx//per]&((1<<64)-1);p=(word>>((idx%per)*bits))&((1<<bits)-1)
  if p>=len(palette):raise ValueError('palette index out of bounds')
  return self.normal(palette[p])
 def normal(self,state):
  if isinstance(state,str):return {'Name':state}
  if not isinstance(state,dict):raise ValueError('unsupported palette entry type')
  name=state.get('Name',state.get('id',state.get('')))
  if not isinstance(name,str) or ':' not in name:raise ValueError('unsupported block-state registry encoding')
  props=state.get('Properties',state.get('properties',{}))
  if not isinstance(props,dict):raise ValueError('unsupported block-state properties')
  return {'Name':name,**({'Properties':props} if props else {})}
with runtime_lock(RUN):
 reader=Reader();center=(-226,72,752);anchors={}
 for name,pos,expected in [('jobSite',center,'minecraft:composter'),('previousStation',(-226,71,744),'minecraft:crafting_table')]:
  state=reader.block(*pos);assert state['Name']==expected,(name,state);anchors[name]={'pos':pos,'state':state}
 count=collections.Counter();interesting=[]
 for y in range(68,77):
  for z in range(736,769):
   for x in range(-242,-209):
    state=reader.block(x,y,z);name=state['Name'];count[name]+=1
    if name in {'minecraft:wheat','minecraft:carrots','minecraft:potatoes','minecraft:beetroots','minecraft:farmland','minecraft:water','minecraft:crafting_table','minecraft:composter','minecraft:hay_block','minecraft:dirt','minecraft:grass_block','minecraft:coarse_dirt'}:
     interesting.append({'pos':[x,y,z],**state})
 report={'scope':'Stopped isolated world only; radius16/y+-4 around real remembered jobsite. No saved-world writes; independent report/raw selected chunks retained.','center':center,'radius':16,'verticalRadius':4,'anchors':anchors,'chunks':reader.identities,'counts':dict(count),'blocks':interesting}
 (OUT/'farm-blocks.json').write_text(json.dumps(report,indent=2)+'\n')
 print('chunks',len(reader.chunks),'blocks',sum(count.values()))
 print('counts',{k:v for k,v in count.items() if k in {b['Name'] for b in interesting}})
 print('crops',[(b['pos'],b['Name'],b.get('Properties')) for b in interesting if b['Name'] in {'minecraft:wheat','minecraft:carrots','minecraft:potatoes','minecraft:beetroots'}])
 print('farmland',[(b['pos'],b.get('Properties')) for b in interesting if b['Name']=='minecraft:farmland'])
