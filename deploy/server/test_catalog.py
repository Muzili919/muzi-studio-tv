import unittest,json
import catalog as c
class CatalogTest(unittest.TestCase):
 def setUp(self):
  self.rows=[]
  for i,(area,hits,score) in enumerate([('泰国',30,8),('日本',10,9),('美国',0,0),('中国香港',20,7)]):
   v={'vod_id':'360:'+str(i),'vod_name':'片'+str(i),'vod_year':'2026','vod_area':area,'vod_time':'2026-10-'+str(20+i),'vod_hits_week':hits,'vod_douban_score':score,'_kind':'1','_sport':'其他'};v['_region']=c.region(v);self.rows.append(v)
 def test_regions(self):
  self.assertEqual([x['_region'] for x in self.rows],['东南亚','日韩','欧美','港台'])
 def test_filter_and_hot(self):
  r=c.response({'t':'1','f':json.dumps({'order':'hot','area':'东南亚'})},self.rows);self.assertEqual([x['vod_id'] for x in r['list']],['360:0'])
 def test_sort_distinct(self):
  ids=lambda o:[x['vod_id'] for x in c.response({'t':'1','f':json.dumps({'order':o})},self.rows)['list']]
  self.assertEqual(ids('recommend'),['360:1','360:0','360:3']);self.assertEqual(ids('hot'),['360:0','360:3','360:1']);self.assertEqual(ids('latest')[0],'360:3')
 def test_no_fake_popularity(self):
  r=c.response({'t':'1','f':json.dumps({'order':'hot','area':'欧美'})},self.rows);self.assertEqual(r['total'],0)
 def test_details_keep_id_order(self):
  self.assertEqual([x['vod_id'] for x in c.response({'ids':'360:2,360:0'},self.rows)['list']],['360:2','360:0'])
 def test_bad_filters(self):
  with self.assertRaises(ValueError):c.response({'f':'[]'},self.rows)
 def test_pagination(self):
  rows=[dict(self.rows[0],vod_id=str(i),vod_name=str(i)) for i in range(30)]
  r=c.response({'pg':'2'},rows);self.assertEqual(len(r['list']),6);self.assertEqual(r['pagecount'],2)
 def test_default_latest(self):
  self.assertEqual(c.filters('1')[0]['init'],'latest');self.assertEqual(c.response({},self.rows)['list'][0]['vod_id'],'360:3')
 def test_genre_composes_with_region(self):
  self.rows[0]['vod_class']='恐怖,科幻';self.rows[1]['vod_class']='恐怖';r=c.response({'f':json.dumps({'genre':'恐怖','area':'东南亚','order':'latest'})},self.rows);self.assertEqual([x['vod_id'] for x in r['list']],['360:0'])
 def test_missing_genre_not_guessed(self):
  self.assertEqual(c.response({'f':json.dumps({'genre':'恐怖'})},self.rows)['total'],0)
 def test_search_ignores_browse_filters(self):
  r=c.response({'wd':'片','f':json.dumps({'genre':'恐怖','area':'欧美','order':'hot'})},self.rows);self.assertEqual(r['total'],4)
 def test_invalid_genre(self):
  with self.assertRaises(ValueError):c.response({'f':json.dumps({'genre':'bogus'})},self.rows)
 def test_ping_pong_filter_reclassifies_existing_records(self):
  row=dict(self.rows[0],_kind='17',vod_name='WTT 乒乓球决赛',_sport='其他')
  other=dict(self.rows[1],_kind='17',vod_name='羽毛球决赛')
  result=c.response({'t':'17','f':json.dumps({'area':'乒乓球'})},[row,other])
  self.assertEqual([x['vod_id'] for x in result['list']],[row['vod_id']])
  self.assertEqual(c.sport({'vod_name':'Table Tennis final'}),'乒乓球')
if __name__=='__main__':unittest.main()
