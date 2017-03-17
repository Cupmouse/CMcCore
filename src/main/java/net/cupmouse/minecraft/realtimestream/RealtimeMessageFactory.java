package net.cupmouse.minecraft.realtimestream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.spongepowered.api.entity.living.player.Player;

import java.util.concurrent.Callable;

public final class RealtimeMessageFactory {

    private RealtimeMessageFactory() {
    }

    public static JSONArray createJoin(String name) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("name", name);

        return wrap(jsonObject, "player.join");
    }

    private static JSONArray wrap(JSONObject data, String dataType) {
        JSONArray jsonArray = new JSONArray();

        jsonArray.put(dataType);
        jsonArray.put(data);

        return jsonArray;
    }
}
