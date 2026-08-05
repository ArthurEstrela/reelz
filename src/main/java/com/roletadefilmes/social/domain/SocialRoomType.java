package com.roletadefilmes.social.domain;

public enum SocialRoomType {
    COUPLE(2),
    GROUP(8);

    private final int capacity;

    SocialRoomType(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }
}
