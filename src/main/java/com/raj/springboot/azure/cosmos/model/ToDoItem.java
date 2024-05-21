package com.raj.springboot.azure.cosmos.model;

public class ToDoItem {

    private Long id;
    private String name;
    private boolean completed;

    public ToDoItem() {
    }

    public ToDoItem(Long id, String name, boolean completed) {
        this.id = id;
        this.name = name;
        this.completed = completed;
    }

  //_etag metadata field
    private String _etag;

    public String get_etag() {
        return _etag;
    }

    public void set_etag(String _etag) {
        this._etag = _etag;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "ToDoItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", completed=" + completed +
                ", _etag='" + _etag + '\'' +
                '}';
    }
}