"""Local review CMS for FongMi 4.0.7 type=1 (filter parameter: f JSON).
No device calls or deployment. Refresh explicitly; stored catalog serves offline.
"""
import argparse, concurrent.futures, json, math, re, subprocess, os
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs, urlencode
ROOT=Path(__file__).resolve().parent
KINDS={'1':'电影','2':'电视剧','4':'动漫','3':'综艺','17':'体育'}
REGIONS=['全部','内地','港台','欧美','日韩','东南亚','其他']
GENRES={'1':['全部','动作','喜剧','爱情','科幻','恐怖','悬疑','犯罪','剧情','战争','纪录'], '2':['全部','剧情','爱情','喜剧','悬疑','犯罪','古装','家庭','战争'], '4':['全部','动画','冒险','奇幻','科幻','喜剧','动作'], '3':['全部','真人秀','音乐','访谈','脱口秀','舞蹈'], '17':['全部']}
def genres(v):
 text=v.get('vod_class','')+' '+v.get('type_name','')
 return [g for g in GENRES.get(v.get('_kind','1'),GENRES['1']) if g!='全部' and g in text]

SPORTS=['全部','足球','篮球','斯诺克','羽毛球','乒乓球','其他']
SOURCES=json.loads(os.environ.get('MUZI_CATALOG_SOURCES','{}'))
BAD=re.compile('伦理|情色|成人|写真|解说|预告')
def number(v):
 try: return float(v or 0)
 except (ValueError,TypeError): return 0

def region(v):
 a=v.get('vod_area','')
 for name,terms in [('港台','香港 台湾 澳门 澳門 台灣'),('日韩','日本 韩国 韓國'),('东南亚','泰国 泰國 新加坡 马来西亚 馬來西亞 印度尼西亚 印尼 菲律宾 菲律賓 越南 柬埔寨 缅甸 老挝'),('内地','中国大陆 大陆 内地 中國大陸 中国'),('欧美','美国 美國 英国 英國 法国 法國 德国 德國 加拿大 意大利 西班牙 俄罗斯 瑞典 丹麦 挪威 芬兰 荷兰 波兰 葡萄牙 爱尔兰 瑞士 奥地利')]:
  if any(t in a for t in terms.split()):return name
 return '其他'

def content_kind(v, parent):
 # Some feeds put animated series in the movie/uncategorized bucket.
 if parent=='1' and '动画' in v.get('vod_class','') and re.search(r'第.+季|全\d+集|更新至.+集',v.get('vod_name','')+' '+v.get('vod_remarks','')):
  return '4'
 return parent

def sport(v):
 s=(v.get('vod_name','')+' '+v.get('type_name','')+' '+v.get('vod_class','')).casefold()
 for name,terms in [('斯诺克',['斯诺克','斯諾克']),('篮球',['篮球','NBA','CBA']),('足球',['足球','英超','欧冠','西甲']),('羽毛球',['羽毛球']),('乒乓球',['乒乓','桌球','table tennis','ping pong','ping-pong','wtt'])]:
  if any(x.casefold() in s for x in terms):return name
 return '其他'

def fetch(src,q):
 p=subprocess.run(['curl','--fail','--silent','--show-error','--location','--max-time','20',SOURCES[src]+'?'+urlencode(q)],capture_output=True,text=True,check=True)
 return json.loads(p.stdout.lstrip('\ufeff'))

def refresh(pages):
 records={}; errors=[]
 old=ROOT/'catalog.json'
 if old.exists():records={x['vod_id']:x for x in json.loads(old.read_text())}
 # Discover provider category relationships; do not assume identical IDs across sources.
 jobs=[]
 for src in SOURCES:
  try:
   classes=fetch(src,{'ac':'list'}).get('class',[])
   parents={str(c['type_id']):str(c.get('type_pid',0)) for c in classes}
   roots={str(c['type_id']):k for c in classes for k,n in KINDS.items() if c['type_name'] in [n,'连续剧' if k=='2' else n]}
   for c in classes:
    tid=str(c['type_id']);kind=roots.get(tid) or roots.get(parents.get(tid))
    if kind and tid not in roots and not BAD.search(c['type_name']):
     for pg in range(1,pages+1):jobs.append((src,tid,kind,pg))
  except Exception as e:errors.append({'source':src,'error':str(e)})
 def pull(job):
  src,tid,kind,pg=job
  try:
   items=fetch(src,{'ac':'detail','t':tid,'pg':pg}).get('list',[])
   out=[]
   for item in items:
    if BAD.search(item.get('vod_name','')+' '+item.get('type_name','')):continue
    v=dict(item);v['vod_id']=src+':'+str(item['vod_id']);v['_source']=src;v['_kind']=content_kind(v,kind);v['_region']=region(v);v['_sport']=sport(v);out.append(v)
   return out,None
  except Exception as e:return [],{'job':job,'error':str(e)}
 with concurrent.futures.ThreadPoolExecutor(3) as pool:
  for items,error in pool.map(pull,jobs):
   for item in items:records[item['vod_id']]=item
   if error:errors.append(error)
 tmp=ROOT/'catalog.tmp';tmp.write_text(json.dumps(list(records.values()),ensure_ascii=False));tmp.replace(old)
 (ROOT/'refresh-report.json').write_text(json.dumps({'records':len(records),'pages_per_leaf':pages,'errors':errors},ensure_ascii=False,indent=2))
 print('Indexed',len(records),'records;',len(errors),'failed requests')

def filters(kind):
 area=SPORTS if kind=='17' else REGIONS
 return [{'key':'order','name':'排序','init':'latest','value':[{'n':'最新','v':'latest'},{'n':'推荐','v':'recommend'},{'n':'热门','v':'hot'}]}, {'key':'area','name':'项目' if kind=='17' else '地区','init':'全部','value':[{'n':x,'v':x} for x in area]}, {'key':'genre','name':'类型','init':'全部','value':[{'n':x,'v':x} for x in GENRES[kind]]}]

def response(q,rows):
 result={'code':1,'msg':'ok','class':[{'type_id':k,'type_name':n} for k,n in KINDS.items()], 'filters':{k:filters(k) for k in KINDS}}
 if q.get('ids'):
  wanted=q['ids'].split(',');lookup={x['vod_id']:x for x in rows};chosen=[lookup[x] for x in wanted if x in lookup]
  return dict(result,list=chosen,total=len(chosen),page=1,pagecount=1,limit=24)
 kind=q.get('t','1')
 if kind not in KINDS:raise ValueError('未知一级分类')
 f=json.loads(q.get('f','{}'))
 if not isinstance(f,dict):raise ValueError('无效筛选')
 order=f.get('order','latest');area=f.get('area','全部');genre=f.get('genre','全部')
 if genre not in GENRES[kind]:raise ValueError('无效类型')
 if q.get('wd'):order='latest';area='全部';genre='全部'
 if not isinstance(f,dict) or order not in ['recommend','latest','hot']:raise ValueError('无效排序')
 if area not in (SPORTS if kind=='17' else REGIONS):raise ValueError('无效地区/项目')
 chosen=[x for x in rows if (q.get('wd') or x['_kind']==kind) and (area=='全部' or (sport(x) if kind=='17' else x['_region'])==area)]
 if genre!='全部':chosen=[x for x in chosen if genre in genres(x)]
 if q.get('wd'):chosen=[x for x in chosen if q['wd'].casefold() in x.get('vod_name','').casefold()]
 if order=='hot':
  chosen=[x for x in chosen if number(x.get('vod_hits_week'))>0]
  chosen.sort(key=lambda x:(number(x.get('vod_hits_week')),x.get('vod_time','')),reverse=True)
  result['msg']='按片源周点击量排序，各站口径不同；不是全网热度榜。' if chosen else '片源未提供有效周热度，暂不展示热门榜。'
 elif order=='recommend':
  chosen=[x for x in chosen if number(x.get('vod_douban_score'))>=7]
  chosen.sort(key=lambda x:(number(x.get('vod_douban_score')),x.get('vod_time','')),reverse=True)
  result['msg']='推荐依据：片源提供的豆瓣评分≥7，未经豆瓣实时核验。' if chosen else '暂无符合评分条件的推荐，请选择最新。'
 else:
  chosen.sort(key=lambda x:x.get('vod_time',''),reverse=True);result['msg']='按片源更新时间排序，并非上映时间。'
 # Keep alternate providers for details/search; deduplicate browse posters by exact name/year.
 if not q.get('wd'):
  seen=set(); unique=[]
  for x in chosen:
   key=(x.get("vod_name"),str(x.get("vod_year")))
   if key not in seen: unique.append(x);seen.add(key)
  chosen=unique
 pg=max(1,int(q.get('pg',1)));limit=24;total=len(chosen)
 return dict(result,list=chosen[(pg-1)*limit:pg*limit],total=total,page=pg,pagecount=max(1,math.ceil(total/limit)),limit=limit)

def fresh_details(ids, rows):
 lookup={x['vod_id']:x for x in rows};out=[]
 groups={}
 for vid in ids.split(','):
  if vid not in lookup:continue
  source,raw=vid.split(':',1);groups.setdefault(source,[]).append(raw)
 for source,rawids in groups.items():
  payload=fetch(source,{'ac':'detail','ids':','.join(rawids)})
  for item in payload.get('list',[]):
   vid=source+':'+str(item.get('vod_id',''))
   if vid not in lookup:continue
   v=dict(lookup[vid]);v.update(item);v['vod_id']=vid;out.append(v)
 found={x['vod_id']:x for x in out}
 return [found[x] for x in ids.split(',') if x in found]

class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  parsed=urlparse(self.path)
  try:
   if parsed.path in ['/wallpaper.png','/preview.html','/preview-data.js','/preview-ui.js','/home-data.js']:
    file=ROOT/parsed.path.lstrip('/');body=file.read_bytes();mime={'png':'image/png','html':'text/html; charset=utf-8','js':'application/javascript; charset=utf-8'}[file.suffix[1:]]
   else:
    q={k:v[-1] for k,v in parse_qs(parsed.query).items()};rows=json.loads((ROOT/'catalog.json').read_text());result=response(q,rows)
    if q.get('ids'):result['list']=fresh_details(q['ids'],rows);result['total']=len(result['list'])
    body=json.dumps(result,ensure_ascii=False).encode();mime='application/json; charset=utf-8'
   self.send_response(200)
  except (ValueError,TypeError) as e:
   self.send_response(400);body=json.dumps({'code':0,'msg':str(e)}).encode();mime='application/json'
  except Exception:
   self.send_response(503);body=b'{"code":0,"msg":"catalog unavailable"}';mime='application/json'
  self.send_header('Content-Type',mime);self.send_header('Content-Length',str(len(body)));self.end_headers()
  try:self.wfile.write(body)
  except (BrokenPipeError,ConnectionResetError):pass
 def log_message(self,*args):pass

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--refresh',action='store_true');p.add_argument('--pages',type=int,default=1);p.add_argument('--port',type=int,default=18181);a=p.parse_args()
 if a.refresh:refresh(a.pages)
 else:ThreadingHTTPServer(('127.0.0.1',a.port),Handler).serve_forever()
