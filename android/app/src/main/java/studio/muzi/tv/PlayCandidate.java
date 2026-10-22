package studio.muzi.tv;

/** One playable line: CMS name + vod_play_from + url, with probe status. */
final class PlayCandidate {
    static final String PENDING = "pending";
    static final String PROBING = "probing";
    static final String OK = "ok";
    static final String FAIL = "fail";

    final String cms;
    final String playFrom;
    final String url;
    volatile String status = PENDING;
    volatile long latencyMs = -1;

    PlayCandidate(String cms, String playFrom, String url) {
        this.cms = cms == null || cms.isEmpty() ? "当前" : cms;
        this.playFrom = playFrom == null ? "" : playFrom;
        this.url = url == null ? "" : url;
    }

    String fromLabel() {
        return playFrom == null || playFrom.isEmpty() ? "默认" : playFrom;
    }

    String statusLabel() {
        if (PROBING.equals(status)) {
            return "探测中";
        }
        if (OK.equals(status)) {
            return "可用 · " + latencyMs + " ms";
        }
        if (FAIL.equals(status)) {
            return "失败";
        }
        return "待测";
    }
}
