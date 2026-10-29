package com.fongmi.android.tv.bean;
public class Config {
    public static Config[] saved = {new Config(0, ""), new Config(1, "")};
    public static int writes;
    public static boolean failVod;
    public final int type;
    public final String url;
    public final String historyToken = "existing-history";
    public Config(int type, String url) { this.type=type; this.url=url; }
    public static Config vod() { if(failVod)throw new IllegalStateException("fixture"); return saved[0]; }
    public static Config live() { return saved[1]; }
    public String getUrl() { return url; }
    public static Config create(int type, String url, String name) { return new Config(type,url); }
    public Config update() { saved[type]=this; writes++; return this; }
}
