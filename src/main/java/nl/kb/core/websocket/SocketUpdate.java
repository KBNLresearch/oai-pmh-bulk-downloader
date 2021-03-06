package nl.kb.core.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface SocketUpdate {

    @JsonProperty
    String getType();

    @JsonProperty
    Object getData();
}
