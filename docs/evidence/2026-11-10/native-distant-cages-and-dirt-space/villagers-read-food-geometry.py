exec(open('/tmp/villagers-read-mine-geology.py').read().split('with (w/')[0])
w=pathlib.Path('runs/native-camp-real-body-clearance-v33/NativeFourRegions');cache={}
with (w/'session.lock').open('rb') as lock:
 fcntl.lockf(lock,fcntl.LOCK_SH|fcntl.LOCK_NB)
 for y in range(63,67):
  for z in range(-637,-631):
   a=[(x,block(x,y,z)) for x in range(-600,-587)]
   print(json.dumps({'y':y,'z':z,'blocks':a}))
