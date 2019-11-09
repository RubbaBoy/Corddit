package com.uddernetworks.reddicord.config;

public enum Config {
    CLIENTID("reddit.clientId"),
    CLIENTSECRET("reddit.clientSecret"),
    REDIRECTURL("reddit.redirectURL"),
    TOKENSTORE("reddit.tokenPath"),

    TOKEN("discord.token"),
    PREFIX("discord.prefix")
    ;

    private String path;

    Config(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
