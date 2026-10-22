#!/usr/bin/env python3
"""木子体育聚合：稳定一级类 + 扁平年份类 + SQLite 年份索引。

一级 type_id 保持稳定：1 斯诺克、3 羽毛球、6 足球、7 篮球。
年份类是扁平 type_name（与 FongMi B1 一致），没有「羽毛球·年」。
已去掉：台球·红牛、羽毛球·量子、网球，以及单独的「斯诺克·量子」。
暴风斯诺克只进索引，不占一级类目。

用法:
  python3 sports_cms.py                 # 兼容 systemd，直接监听
  python3 sports_cms.py --self-test
  python3 sports_cms.py --index --pages 3
  python3 sports_cms.py --serve --port 18082

环境变量:
  MUZI_SRC_LZ / MUZI_SRC_BF / MUZI_SRC_HN   上游 Apple CMS，HN 为空则羽毛球列表为空
  MUZI_SPORTS_DB                            SQLite 路径
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sqlite3
import tempfile
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, HTTPServer
from socketserver import ThreadingMixIn

YEAR_RE = re.compile(r"(20[0-3]\d)")
YEARS = (2026, 2025, 2024, 2023, 2022)

# (type_id, type_name, sport, year or None, upstream src, upstream query)
def _year_rows(start_id, sport):
    rows = []
    for i, year in enumerate(YEARS):
        rows.append((start_id + i, f"{sport}·{year}", sport, year, None, None))
    return rows

TYPES = [
    (1, "斯诺克", "斯诺克", None, "lz", {"t": "40"}),
    *_year_rows(11, "斯诺克"),
    (6, "足球", "足球", None, "lz", {"t": "37"}),
    *_year_rows(21, "足球"),
    (7, "篮球", "篮球", None, "lz", {"t": "38"}),
    *_year_rows(31, "篮球"),
    (3, "羽毛球", "羽毛球", None, "hn", {"t": "35", "wd": "羽毛球"}),
]
TM = {t[0]: t for t in TYPES}
# 索引任务：主源 + 暴风斯诺克备援。羽毛球不建五年索引。
INDEX_JOBS = (
    ("lz", "40", "斯诺克"),
    ("bf", "57", "斯诺克"),
    ("lz", "37", "足球"),
    ("lz", "38", "篮球"),
)


def sources():
    return {
        "lz": os.environ.get("MUZI_SRC_LZ", "https://cj.lziapi.com/api.php/provide/vod/").strip(),
        "bf": os.environ.get("MUZI_SRC_BF", "https://bfzyapi.com/api.php/provide/vod/").strip(),
        "hn": os.environ.get("MUZI_SRC_HN", "").strip(),
    }


def default_db():
    env = os.environ.get("MUZI_SPORTS_DB")
    if env:
        return env
    local = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sports.db")
    parent = os.path.dirname(local) or "."
    if os.access(parent, os.W_OK):
        return local
    return "/tmp/muzi-sports.db"


def connect(path=None):
    db = sqlite3.connect(path or default_db())
    db.row_factory = sqlite3.Row
    db.execute(
        """CREATE TABLE IF NOT EXISTS vod (
            vod_key TEXT PRIMARY KEY,
            src TEXT NOT NULL,
            raw_id TEXT NOT NULL,
            sport TEXT NOT NULL,
            name TEXT NOT NULL,
            year INTEGER NOT NULL DEFAULT 0,
            date TEXT NOT NULL DEFAULT '',
            type_name TEXT NOT NULL DEFAULT ''
        )"""
    )
    db.execute("CREATE INDEX IF NOT EXISTS idx_sport_year ON vod(sport, year, date)")
    return db


def guess_year(name, vod_year):
    found = []
    for src in (vod_year or "", name or ""):
        for m in YEAR_RE.findall(str(src)):
            y = int(m)
            if 1990 <= y <= 2035:
                found.append(y)
    return max(found) if found else 0


def get(src, q, timeout=12):
    base = sources().get(src) or ""
    if not base:
        raise RuntimeError(f"{src} 未配置")
    url = base + ("&" if "?" in base else "?") + urllib.parse.urlencode(q)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8", "ignore"))


def tag(src, lst, tid):
    name = TM[tid][1] if tid in TM else ""
    out = []
    for v in lst or []:
        if not isinstance(v, dict):
            continue
        type_name = str(v.get("type_name") or "")
        if any(k in type_name for k in ("伦理", "福利", "写真")):
            continue
        v = dict(v)
        v["vod_id"] = f"{src}_{v.get('vod_id')}"
        if tid:
            v["type_id"] = tid
            v["type_name"] = name
        out.append(v)
    return out


def index_sources(pages, db_path=None):
    """翻页写入 SQLite。pages<=0 时每个源最多 40 页，避免一次打满上游。"""
    db = connect(db_path)
    written = 0
    for src, tid, sport in INDEX_JOBS:
        if not sources().get(src):
            continue
        cap = 40 if pages <= 0 else pages
        for pg in range(1, cap + 1):
            try:
                payload = get(src, {"ac": "list", "t": tid, "pg": str(pg)})
            except Exception as exc:
                print(f"index {src} t={tid} pg={pg} {exc}")
                break
            rows = payload.get("list") or []
            if not rows:
                break
            for v in rows:
                raw = str(v.get("vod_id") or "").strip()
                name = str(v.get("vod_name") or "").strip()
                if not raw or not name:
                    continue
                year = guess_year(name, v.get("vod_year"))
                date = str(v.get("vod_time") or v.get("vod_year") or "")
                db.execute(
                    """INSERT INTO vod(vod_key,src,raw_id,sport,name,year,date,type_name)
                       VALUES(?,?,?,?,?,?,?,?)
                       ON CONFLICT(vod_key) DO UPDATE SET
                         name=excluded.name, year=excluded.year, date=excluded.date, type_name=excluded.type_name""",
                    (f"{src}_{raw}", src, raw, sport, name, year, date, str(v.get("type_name") or "")),
                )
                written += 1
            pagecount = int(payload.get("pagecount") or pg)
            if pg >= pagecount:
                break
    db.commit()
    db.close()
    return written


def query_index(sport, year, pg, limit=20, db_path=None):
    db = connect(db_path)
    pg = max(int(pg or 1), 1)
    where = "sport=?"
    args = [sport]
    if year:
        where += " AND year=?"
        args.append(int(year))
    total = db.execute(f"SELECT COUNT(*) FROM vod WHERE {where}", args).fetchone()[0]
    rows = db.execute(
        f"SELECT * FROM vod WHERE {where} ORDER BY date DESC, year DESC, name DESC LIMIT ? OFFSET ?",
        args + [limit, (pg - 1) * limit],
    ).fetchall()
    db.close()
    pagecount = max(1, (total + limit - 1) // limit) if total else 1
    items = []
    for r in rows:
        items.append(
            {
                "vod_id": r["vod_key"],
                "vod_name": r["name"],
                "vod_year": str(r["year"] or ""),
                "vod_time": r["date"],
                "type_name": r["type_name"] or sport,
            }
        )
    return {"page": pg, "pagecount": pagecount, "total": total, "list": items}


def classes():
    return [{"type_id": t[0], "type_name": t[1]} for t in TYPES]


def empty(msg="ok"):
    return {
        "code": 1,
        "msg": msg,
        "page": 1,
        "pagecount": 1,
        "limit": 20,
        "total": 0,
        "list": [],
        "class": classes(),
    }


def handle(q, db_path=None):
    out = empty()
    try:
        ac = q.get("ac", "list")
        pg = q.get("pg", "1")
        if q.get("ids"):
            by = {}
            for i in q["ids"].split(","):
                src, _, raw = i.partition("_")
                if src and raw:
                    by.setdefault(src, []).append(raw)
            for src, ids in by.items():
                if not sources().get(src):
                    continue
                out["list"] += tag(src, get(src, {"ac": "detail", "ids": ",".join(ids)}).get("list"), 0)
            out["total"] = len(out["list"])
            return out
        if q.get("wd") and not q.get("t"):
            for src in ("lz", "bf", "hn"):
                if not sources().get(src):
                    continue
                try:
                    out["list"] += tag(src, get(src, {"ac": "detail", "wd": q["wd"]}).get("list"), 0)
                except Exception:
                    pass
            out["total"] = len(out["list"])
            return out
        if not q.get("t"):
            return out
        tid = int(q["t"])
        spec = TM.get(tid)
        if not spec:
            out["msg"] = "unknown type"
            return out
        _tid, type_name, sport, year, src, upstream = spec
        if year:
            indexed = query_index(sport, year, pg, db_path=db_path)
            if indexed["total"]:
                out.update(page=indexed["page"], pagecount=indexed["pagecount"], total=indexed["total"])
                rows = indexed["list"]
                if ac in ("videolist", "detail") and rows:
                    ids = ",".join(r["vod_id"] for r in rows)
                    merged = handle({"ids": ids, "ac": "detail"}, db_path=db_path)
                    merged["class"] = classes()
                    merged["page"] = indexed["page"]
                    merged["pagecount"] = indexed["pagecount"]
                    merged["total"] = indexed["total"]
                    return merged
                for row in rows:
                    row["type_id"] = tid
                    row["type_name"] = type_name
                out["list"] = rows
                return out
            # 索引还没跑时，用「赛事+年」搜索，不编造条目
            src = "lz"
            upstream = {"wd": f"{sport}{year}"}
        if not src or not sources().get(src):
            out["msg"] = "该分类暂无上游" if sport != "羽毛球" else "羽毛球无五年片库"
            return out
        remote = get(src, dict(upstream, ac="detail" if ac in ("videolist", "detail") else "list", pg=pg))
        out.update(page=remote.get("page", 1), pagecount=remote.get("pagecount", 1), total=remote.get("total", 0))
        out["list"] = tag(src, remote.get("list"), tid)
        if sport == "羽毛球" and not out["list"]:
            out["msg"] = "羽毛球无五年片库"
    except Exception as exc:
        out["msg"] = str(exc)
    return out


class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True


class Handler(BaseHTTPRequestHandler):
    db_path = None

    def log_message(self, fmt, *args):
        return

    def do_GET(self):
        q = dict(urllib.parse.parse_qsl(urllib.parse.urlparse(self.path).query))
        body = json.dumps(handle(q, self.db_path), ensure_ascii=False).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def self_test():
    fd, path = tempfile.mkstemp(prefix="muzi-sports-", suffix=".db")
    os.close(fd)
    try:
        db = connect(path)
        samples = [
            ("lz_1", "lz", "1", "足球", "英超2024 第1轮", 2024, "2024-08-16 20:00:00"),
            ("lz_2", "lz", "2", "足球", "英超2024 第2轮", 2024, "2024-08-17 20:00:00"),
            ("lz_3", "lz", "3", "足球", "世界杯2022 决赛", 2022, "2022-12-18 23:00:00"),
            ("lz_4", "lz", "4", "足球", "欧洲杯2021", 2021, "2021-07-11 00:00:00"),
            ("bf_9", "bf", "9", "斯诺克", "斯诺克2026 北爱尔兰", 2026, "2026-10-01 00:00:00"),
            ("lz_8", "lz", "8", "篮球", "NBA2023 总决赛", 2023, "2023-06-12 00:00:00"),
        ]
        for row in samples:
            db.execute(
                "INSERT INTO vod(vod_key,src,raw_id,sport,name,year,date,type_name) VALUES(?,?,?,?,?,?,?,'')",
                row,
            )
        db.commit()
        db.close()
        names = [t[1] for t in TYPES]
        assert names[0] == "斯诺克" and 1 == TYPES[0][0]
        assert names.count("羽毛球") == 1
        assert all(not n.startswith("羽毛球·") for n in names)
        assert "台球·红牛" not in names and "羽毛球·量子" not in names and "网球" not in names
        assert "斯诺克·量子" not in names
        for sport in ("斯诺克", "足球", "篮球"):
            for year in YEARS:
                assert f"{sport}·{year}" in names
        assert TM[1][1] == "斯诺克" and TM[3][1] == "羽毛球" and TM[6][1] == "足球" and TM[7][1] == "篮球"
        listed = query_index("足球", 2024, 1, db_path=path)
        assert listed["total"] == 2
        assert listed["list"][0]["vod_time"] >= listed["list"][1]["vod_time"]
        assert query_index("足球", 2021, 1, db_path=path)["total"] == 1
        assert query_index("羽毛球", 2024, 1, db_path=path)["total"] == 0
        assert guess_year("世锦赛2023", "") == 2023
        assert guess_year("无年份", "2022") == 2022
        # 类目接口在没有上游时仍返回 B1 类名
        payload = handle({}, db_path=path)
        got = [c["type_name"] for c in payload["class"]]
        assert got == names
        print("sports_cms self-test ok", len(names), "types")
    finally:
        try:
            os.remove(path)
        except OSError:
            pass


def serve(host, port, db_path):
    Handler.db_path = db_path
    ThreadingHTTPServer((host, port), Handler).serve_forever()


def main(argv=None):
    parser = argparse.ArgumentParser(description="木子体育 CMS")
    parser.add_argument("--serve", action="store_true")
    parser.add_argument("--index", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--pages", type=int, default=3)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=18082)
    parser.add_argument("--db", default="")
    args = parser.parse_args(argv)
    db_path = args.db or None
    if args.self_test:
        self_test()
        return
    if args.index:
        n = index_sources(args.pages, db_path)
        print(f"indexed {n} rows -> {db_path or default_db()}")
        if not args.serve:
            return
    serve(args.host, args.port, db_path or default_db())


if __name__ == "__main__":
    import sys

    if len(sys.argv) == 1:
        serve("127.0.0.1", 18082, default_db())
    else:
        main()
