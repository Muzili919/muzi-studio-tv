#!/usr/bin/env python3
"""木子 sports aggregator CMS: mixes several CMS sports types into one FongMi site (type 1)."""
import json,urllib.request,urllib.parse
from http.server import HTTPServer,BaseHTTPRequestHandler
from socketserver import ThreadingMixIn
class ThreadingHTTPServer(ThreadingMixIn,HTTPServer): daemon_threads=True
SRC={'hn':'https://cms-d.example.com/api.php/provide/vod/','lz':'https://cms-c.example.com/api.php/provide/vod/','bf':'https://cms-a.example.com/api.php/provide/vod/'}
TYPES=[(1,'斯诺克','bf',{'t':'57'}),(2,'斯诺克·量子','lz',{'t':'40'}),(3,'羽毛球','hn',{'t':'35','wd':'羽毛球'}),
       (4,'羽毛球·量子','lz',{'wd':'羽毛球'}),(5,'台球·红牛','hn',{'t':'34'}),(6,'足球','lz',{'t':'37'}),
       (7,'篮球','lz',{'t':'38'}),(8,'网球','lz',{'t':'39'})]
TM={t[0]:t for t in TYPES}
def get(src,q):
    u=SRC[src]+'?'+urllib.parse.urlencode(q)
    return json.loads(urllib.request.urlopen(urllib.request.Request(u,headers={'User-Agent':'Mozilla/5.0'}),timeout=15).read().decode('utf-8','ignore'))
def tag(src,lst,tid):
    for v in lst or []:
        v['vod_id']=f"{src}_{v['vod_id']}"
        if tid: v['type_id']=tid; v['type_name']=TM[tid][1]
    return lst or []
class H(BaseHTTPRequestHandler):
    def log_message(self,*a): pass
    def do_GET(self):
        q=dict(urllib.parse.parse_qsl(urllib.parse.urlparse(self.path).query))
        out={'code':1,'msg':'ok','page':1,'pagecount':1,'limit':20,'total':0,'list':[],
             'class':[{'type_id':t[0],'type_name':t[1]} for t in TYPES]}
        try:
            ac=q.get('ac','list'); pg=q.get('pg','1')
            if q.get('ids'):
                by={}
                for i in q['ids'].split(','):
                    s,_,r=i.partition('_'); by.setdefault(s,[]).append(r)
                for s,ids in by.items():
                    out['list']+=tag(s,get(s,{'ac':'detail','ids':','.join(ids)}).get('list'),0)
            elif q.get('wd') and not q.get('t'):
                for s in ('bf','lz','hn'):
                    try: out['list']+=tag(s,get(s,{'ac':'detail','wd':q['wd']}).get('list'),0)
                    except Exception: pass
            elif q.get('t'):
                tid=int(q['t']); _,_,s,p=TM[tid]
                r=get(s,dict(p,ac='detail' if ac in('videolist','detail') else 'list',pg=pg))
                out.update(page=r.get('page',1),pagecount=r.get('pagecount',1),total=r.get('total',0))
                out['list']=tag(s,r.get('list'),tid)
        except Exception as e: out['msg']=str(e)
        b=json.dumps(out,ensure_ascii=False).encode()
        self.send_response(200); self.send_header('Content-Type','application/json; charset=utf-8')
        self.send_header('Content-Length',str(len(b))); self.end_headers(); self.wfile.write(b)
ThreadingHTTPServer(('127.0.0.1',18082),H).serve_forever()
