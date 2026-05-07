package com.auction.common.model.entity;
//  sử dụng Gson để chuyển đổi đối tượng Java sang JSON và ngược lại
//  nên cũng không cần dùng serializable
public abstract class Entity {
    protected int id;

    public Entity() {
        // Khi INSERT đối tượng này xuống Database,
        // MySQL sẽ tự động sinh ID thật
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}