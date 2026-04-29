package com.auction.common.model.entity;

import java.io.Serializable;
import java.util.UUID;

// Lớp gốc cho mọi đối tượng. Dùng để tự động tạo ID và cho phép truyền qua mạng.
public abstract class Entity implements Serializable {
    protected String id;

    public Entity() {
        this.id = UUID.randomUUID().toString(); // Tự động tạo ID duy nhất
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}