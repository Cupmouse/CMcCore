package net.cupmouse.minecraft.data.user;

public class OnlineUser {

    // TODO セッション時間を何秒かごとに更新すると、サーバーがクラッシュしたときもプレイヤーが遊んでいた時間が記録される。
    private int sessionId;
    private UserDataFetcher dataFetcher;

    OnlineUser(int sessionId, UserDataFetcher dataFetcher) {
        this.sessionId = sessionId;
        this.dataFetcher = dataFetcher;
    }

    public UserDataFetcher getDataFetcher() {
        return dataFetcher;
    }

    public int getSessionId() {
        return sessionId;
    }

    void updateSessionDatetime() {
        // TODO
    }
}
