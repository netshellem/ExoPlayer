package com.irdeto.drm;


public class Config {
    private static Config Instance = new Config();
    private CreateSessionParams createSessionParams = new CreateSessionParams();

    private Config() {
    }

    public static Config getInstance() {
        return Instance;
    }

    public void set_createSession_userData(String userData) {
        createSessionParams.userData = userData;
    }

    public String get_createSession_userData() {
        return createSessionParams.userData;
    }

    private class CreateSessionParams {
        public String userData = "";//"{\"mgLrExtension\":{\"userId\":\"xxx\",\"userToken\":\"xxx\",\"programId\":\"xxx\"}}";
    }
}
