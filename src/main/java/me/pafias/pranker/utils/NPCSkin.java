package me.pafias.pranker.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class NPCSkin {

    private final String texture;
    private final String signature;

    public NPCSkin(String texture, String signature) {
        this.texture = texture;
        this.signature = signature;
    }

    public String getTexture() {
        return texture;
    }

    public String getSignature() {
        return signature;
    }

    public static NPCSkin fromPlayerName(String playerName) {
        UUID uuid = getUUIDFromName(playerName);
        if (uuid != null) {
            return fromPlayerUUID(uuid);
        }
        return null;
    }

    public static NPCSkin fromPlayerUUID(UUID playerUUID) {
        try {
            String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + playerUUID.toString().replace("-", "");
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());

            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray properties = jsonObject.getAsJsonArray("properties");

            for (int i = 0; i < properties.size(); i++) {
                JsonObject property = properties.get(i).getAsJsonObject();
                if (property.has("name") && property.get("name").getAsString().equals("textures")) {
                    String texture = property.getAsJsonObject("value").get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString();
                    String signature = property.getAsJsonObject("signature").getAsString();
                    return new NPCSkin(texture, signature);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static NPCSkin fromBase64EncodedString(String base64EncodedSkinData) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedSkinData);
        String decodedString = new String(decodedBytes);

        JsonObject skinJson = new JsonParser().parse(decodedString).getAsJsonObject();
        JsonObject textures = skinJson.getAsJsonObject("textures");
        JsonObject skin = textures.getAsJsonObject("SKIN");

        String textureUrl = skin.getAsJsonPrimitive("url").getAsString();

        return new NPCSkin(textureUrl, "");
    }

    private static UUID getUUIDFromName(String playerName) {
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());

            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            String uuidStr = jsonObject.get("id").getAsString();
            return UUID.fromString(uuidStr.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
